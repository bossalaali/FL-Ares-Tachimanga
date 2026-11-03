package eu.kanade.tachiyomi.extension.ar.mangawy

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import keiyoushi.annotation.Source
import keiyoushi.network.Fetch
import keiyoushi.network.get
import keiyoushi.source.KeiSource
import keiyoushi.utils.asJsoup
import keiyoushi.utils.parseAs
import kotlinx.serialization.json.JsonElement
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document
import java.util.Calendar

@Source
abstract class Mangawy : KeiSource() {

    // ============================== Browse ===============================

    private suspend fun browse(page: Int, query: String, filters: FilterList, sort: String? = null): MangasPage {
        val apiUrl = "$baseUrl/api/browse".toHttpUrl().newBuilder().apply {
            if (query.isNotBlank()) addQueryParameter("q", query.trim())
            if (sort != null) addQueryParameter("sort", sort)
            filters.forEach { filter ->
                when (filter) {
                    is SelectFilter -> filter.addTo(this)
                    is GenreFilter -> filter.state.filter { it.state }.forEach { addQueryParameter("genre", it.slug) }
                    else -> {}
                }
            }
            addQueryParameter("page", page.toString())
        }.build()
        val data = client.get(apiUrl, Fetch.XHR).parseAs<BrowseDto>()
        val mangas = data.items.map { it.toSManga() }
        return MangasPage(mangas, data.pagination.page < data.pagination.totalPages)
    }

    override suspend fun getPopularManga(page: Int) = browse(page, "", FilterList(), "popular")

    override suspend fun getLatestUpdates(page: Int) = browse(page, "", FilterList(), "latest")

    override suspend fun getSearchMangaList(page: Int, query: String, filters: FilterList) = browse(page, query, filters)

    override suspend fun getMangaByUrl(url: HttpUrl): SManga? {
        val segments = url.pathSegments
        if (segments.firstOrNull() != "series" || segments.size < 2) return null
        val manga = SManga.create().apply { this.url = "/series/${segments[1]}" }
        return fetchMangaUpdate(manga, emptyList(), fetchDetails = true, fetchChapters = false).manga.apply {
            this.url = manga.url
        }
    }

    // ============================== Details ==============================

    override fun getMangaUrl(manga: SManga): String = baseUrl + manga.url

    override suspend fun fetchMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        val document = client.get(baseUrl + manga.url, Fetch.DOCUMENT).asJsoup()
        val details = if (fetchDetails) parseDetails(document, manga) else manga
        val chapterList = if (fetchChapters) parseChapters(document, manga.url) else chapters
        return SMangaUpdate(manga = details, chapters = chapterList)
    }

    private suspend fun parseDetails(document: Document, manga: SManga): SManga {
        val title = document.selectFirst("h1")?.text() ?: manga.title
        // The search API returns full metadata, so prefer it over scraping the page
        val slug = manga.url.substringAfterLast('/')
        val dto = runCatching {
            val url = "$baseUrl/api/series/search".toHttpUrl().newBuilder().addQueryParameter("q", title).build()
            client.get(url, Fetch.XHR).parseAs<List<SeriesDto>>().firstOrNull { it.slug == slug }
        }.getOrNull()

        return dto?.toSManga()?.apply { this.url = manga.url } ?: SManga.create().apply {
            url = manga.url
            this.title = title
            description = document.selectFirst("meta[name=description]")?.attr("content")
            thumbnail_url = document.selectFirst("meta[property=og:image]")?.attr("content") ?: manga.thumbnail_url
            genre = document.select("a[href^=/browse?genre=]").joinToString { it.text() }
            author = document.select("a[href^=/browse?author=]").joinToString { it.text() }.ifBlank { null }
            artist = document.select("a[href^=/browse?artist=]").joinToString { it.text() }.ifBlank { null }
        }
    }

    private fun parseChapters(document: Document, mangaUrl: String): List<SChapter> = document.select("a[href^=$mangaUrl/chapter/]")
        .distinctBy { it.attr("href") }
        .map { link ->
            SChapter.create().apply {
                url = link.attr("href")
                val number = link.selectFirst("span.font-bold")?.text()?.replace("ف.", "")?.trim()
                val title = link.selectFirst("div.truncate")?.text()
                name = when {
                    number.isNullOrBlank() -> title ?: url.substringAfterLast('/')
                    title.isNullOrBlank() || title.equals("Chapter $number", ignoreCase = true) -> "الفصل $number"
                    else -> "الفصل $number - $title"
                }
                number?.toFloatOrNull()?.let { chapter_number = it }
                date_upload = parseArabicDate(link.selectFirst("span.text-muted-foreground")?.text())
            }
        }

    // =============================== Pages ===============================

    override fun getChapterUrl(chapter: SChapter): String = baseUrl + chapter.url

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val document = client.get(baseUrl + chapter.url, Fetch.DOCUMENT).asJsoup()
        val images = document.select("img[src*=/published/]")
            .map { it.absUrl("src") }
            .distinct()
        if (images.isEmpty()) throw Exception("الفصل مقفل أو غير متاح، افتحه في WebView")
        return images.mapIndexed { i, url -> Page(i, imageUrl = url) }
    }

    // ============================== Filters ==============================

    override val supportsFilterFetching get() = true

    override suspend fun fetchFilterData(): JsonElement = client.get("$baseUrl/api/filters", Fetch.XHR).parseAs<JsonElement>()

    override fun getFilterList(data: JsonElement?): FilterList {
        val genres = data?.let { runCatching { it.parseAs<FiltersDto>().genres }.getOrNull() }.orEmpty()
        return FilterList(
            listOfNotNull(
                SortFilter(),
                TypeFilter(),
                StatusFilter(),
                if (genres.isNotEmpty()) GenreFilter(genres.map { GenreCheckBox(it.name, it.slug) }) else null,
            ),
        )
    }

    // ============================= Utilities =============================

    private fun parseArabicDate(text: String?): Long {
        if (text.isNullOrBlank()) return 0L
        val normalized = text.map { c -> if (c in '٠'..'٩') '0' + (c - '٠') else c }.joinToString("").trim()
        val parts = normalized.split(Regex("\\s+"))
        if (parts.size < 3) return 0L
        val day = parts[0].toIntOrNull() ?: return 0L
        val month = ARABIC_MONTHS.indexOf(parts[1]).takeIf { it >= 0 } ?: return 0L
        val year = parts[2].toIntOrNull() ?: return 0L
        return Calendar.getInstance().apply {
            clear()
            set(year, month, day)
        }.timeInMillis
    }
}

private val ARABIC_MONTHS = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
)

open class SelectFilter(
    name: String,
    private val param: String,
    private val options: List<Pair<String, String>>,
) : Filter.Select<String>(name, options.map { it.first }.toTypedArray()) {
    fun addTo(url: HttpUrl.Builder) {
        val value = options[state].second
        if (value.isNotEmpty()) url.addQueryParameter(param, value)
    }
}

class SortFilter : SelectFilter("الترتيب", "sort", listOf("الأكثر شعبية" to "popular", "آخر تحديث" to "latest"))

class TypeFilter :
    SelectFilter(
        "النوع",
        "type",
        listOf("الكل" to "", "مانهوا" to "manhwa", "مانها" to "manhua", "مانجا" to "manga", "أخرى" to "oel"),
    )

class StatusFilter :
    SelectFilter("الحالة", "status", listOf("الكل" to "", "مستمرة" to "ongoing", "مكتملة" to "completed"))

class GenreCheckBox(name: String, val slug: String) : Filter.CheckBox(name)

class GenreFilter(genres: List<GenreCheckBox>) : Filter.Group<GenreCheckBox>("التصنيفات", genres)
