package eu.kanade.tachiyomi.extension.ar.alaashiq

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import keiyoushi.annotation.Source
import keiyoushi.network.Fetch
import keiyoushi.network.get
import keiyoushi.network.post
import keiyoushi.source.KeiSource
import keiyoushi.utils.asJsoup
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.Calendar

@Source
abstract class AlAashiq : KeiSource() {

    // ============================== Browse ===============================

    private suspend fun browse(page: Int, query: String, order: String?): MangasPage {
        val url = "$baseUrl/".toHttpUrl().newBuilder().apply {
            if (page > 1) addPathSegments("page/$page/")
            addQueryParameter("s", query.trim())
            addQueryParameter("post_type", "wp-manga")
            if (order != null) addQueryParameter("m_orderby", order)
        }.build()
        val document = client.get(url, Fetch.DOCUMENT).asJsoup()
        val mangas = document.select("div.c-tabs-item__content").mapNotNull { card ->
            val link = card.selectFirst("div.post-title a") ?: return@mapNotNull null
            SManga.create().apply {
                setUrlWithoutDomain(link.absUrl("href"))
                title = link.text()
                thumbnail_url = card.selectFirst("img")?.imageUrl()
            }
        }
        val hasNext = document.selectFirst("div.nav-previous a, a.nextpostslink") != null
        return MangasPage(mangas, hasNext)
    }

    override suspend fun getPopularManga(page: Int) = browse(page, "", "views")

    override suspend fun getLatestUpdates(page: Int) = browse(page, "", "latest")

    override suspend fun getSearchMangaList(page: Int, query: String, filters: FilterList) = browse(page, query, if (query.isBlank()) "views" else null)

    override suspend fun getMangaByUrl(url: HttpUrl): SManga? {
        val segments = url.pathSegments.filter(String::isNotEmpty)
        if (segments.firstOrNull() != "manga" || segments.size < 2) return null
        val manga = SManga.create().apply { this.url = "/manga/${segments[1]}/" }
        return fetchMangaUpdate(manga, emptyList(), fetchDetails = true, fetchChapters = false).manga
    }

    // ============================== Details ==============================

    override fun getMangaUrl(manga: SManga): String = baseUrl + manga.url

    override suspend fun fetchMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        val details = if (fetchDetails) {
            parseDetails(client.get(baseUrl + manga.url, Fetch.DOCUMENT).asJsoup(), manga)
        } else {
            manga
        }
        val chapterList = if (fetchChapters) fetchChapters(manga.url) else chapters
        return SMangaUpdate(manga = details, chapters = chapterList)
    }

    private fun parseDetails(document: Document, manga: SManga) = SManga.create().apply {
        url = manga.url
        title = document.selectFirst("div.post-title h1")?.ownText()?.ifBlank { null }
            ?: document.selectFirst("div.post-title h1")?.text()
            ?: manga.title
        thumbnail_url = document.selectFirst("div.summary_image img")?.imageUrl() ?: manga.thumbnail_url
        description = document.selectFirst("div.summary__content")?.text()
        author = document.select("div.author-content a").joinToString { it.text() }.ifBlank { null }
        artist = document.select("div.artist-content a").joinToString { it.text() }.ifBlank { null }
        genre = document.select("div.genres-content a").joinToString { it.text() }
        status = when (infoValue(document, "الحالة")) {
            null -> SManga.UNKNOWN
            in listOf("مستمرة", "مستمر", "OnGoing", "Ongoing") -> SManga.ONGOING
            in listOf("مكتملة", "مكتمل", "Completed") -> SManga.COMPLETED
            in listOf("متوقفة", "متوقف", "On Hold") -> SManga.ON_HIATUS
            in listOf("ملغية", "ملغاة", "Canceled") -> SManga.CANCELLED
            else -> SManga.UNKNOWN
        }
    }

    private fun infoValue(document: Document, heading: String): String? = document.select("div.post-content_item")
        .firstOrNull { it.selectFirst("div.summary-heading")?.text()?.trim() == heading }
        ?.selectFirst("div.summary-content")?.text()?.trim()

    /** Chapters are served 50 at a time from `{manga}/ajax/chapters/?t={page}`. */
    private suspend fun fetchChapters(mangaUrl: String): List<SChapter> {
        val base = baseUrl + mangaUrl.trimEnd('/') + "/ajax/chapters/"
        val result = mutableListOf<SChapter>()
        var page = 1
        var lastPage = 1
        do {
            val url = if (page == 1) base else "$base?t=$page"
            val document = client.post(url, Fetch.XHR, FormBody.Builder().build()).asJsoup()
            if (page == 1) {
                lastPage = document.select("[data-page]").mapNotNull { it.attr("data-page").toIntOrNull() }.maxOrNull() ?: 1
            }
            val items = document.select("li.wp-manga-chapter").map(::parseChapter)
            result += items
            page++
        } while (page <= lastPage && items.isNotEmpty() && page <= 200)
        return result.distinctBy { it.url }
    }

    private fun parseChapter(element: Element) = SChapter.create().apply {
        val link = element.selectFirst("a")!!
        setUrlWithoutDomain(link.absUrl("href"))
        name = link.text()
        val dateElement = element.selectFirst("span.chapter-release-date")
        date_upload = parseDate(dateElement?.selectFirst("a[title]")?.attr("title") ?: dateElement?.text())
    }

    // =============================== Pages ===============================

    override fun getChapterUrl(chapter: SChapter): String = baseUrl + chapter.url

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val document = client.get(baseUrl + chapter.url, Fetch.DOCUMENT).asJsoup()
        val images = document.select("div.reading-content img, img.wp-manga-chapter-img")
            .mapNotNull { it.imageUrl() }
            .filterNot { it.contains("dflazy") }
            .distinct()
        if (images.isEmpty()) throw Exception("الفصل غير متاح، افتحه في WebView")
        return images.mapIndexed { i, url -> Page(i, imageUrl = url) }
    }

    // ============================= Utilities =============================

    private fun Element.imageUrl(): String? = listOf("data-src", "data-lazy-src", "src")
        .map { attr(it).trim() }
        .firstOrNull { it.isNotEmpty() && !it.contains("dflazy") && !it.startsWith("data:") }
        ?.let { if (it.startsWith("//")) "https:$it" else it }
        ?.let { absUrlOf(it) }

    private fun absUrlOf(url: String) = if (url.startsWith("http")) url else baseUrl + "/" + url.trimStart('/')

    private fun parseDate(raw: String?): Long {
        val text = raw?.trim()?.map { c -> if (c in '٠'..'٩') '0' + (c - '٠') else c }?.joinToString("") ?: return 0L
        if (text.isEmpty()) return 0L

        // Relative dates such as "منذ 3 ساعات" or "2 days ago"
        val amount = Regex("\\d+").find(text)?.value?.toIntOrNull() ?: 1
        val relativeField = when {
            text.contains("ثاني") || text.contains("sec") -> Calendar.SECOND
            text.contains("دقيق") || text.contains("min") -> Calendar.MINUTE
            text.contains("ساع") || text.contains("hour") -> Calendar.HOUR_OF_DAY
            text.contains("يوم") || text.contains("أيام") || text.contains("day") -> Calendar.DAY_OF_MONTH
            text.contains("أسبوع") || text.contains("اسبوع") || text.contains("week") -> Calendar.WEEK_OF_YEAR
            else -> null
        }
        if (relativeField != null && (text.contains("منذ") || text.contains("ago"))) {
            return Calendar.getInstance().apply { add(relativeField, -amount) }.timeInMillis
        }

        // Absolute dates such as "4 سبتمبر، 2023"
        val parts = text.replace("،", " ").replace(",", " ").split(Regex("\\s+"))
        val day = parts.getOrNull(0)?.toIntOrNull() ?: return 0L
        val month = parts.getOrNull(1)?.let { name -> MONTHS.indexOfFirst { name in it } }?.takeIf { it >= 0 } ?: return 0L
        val year = parts.getOrNull(2)?.toIntOrNull() ?: return 0L
        return Calendar.getInstance().apply {
            clear()
            set(year, month, day)
        }.timeInMillis
    }
}

private val MONTHS = listOf(
    listOf("يناير", "كانون الثاني"),
    listOf("فبراير", "شباط"),
    listOf("مارس", "آذار"),
    listOf("أبريل", "ابريل", "إبريل", "نيسان"),
    listOf("مايو", "أيار"),
    listOf("يونيو", "حزيران"),
    listOf("يوليو", "تموز"),
    listOf("أغسطس", "اغسطس", "آب"),
    listOf("سبتمبر", "أيلول"),
    listOf("أكتوبر", "اكتوبر", "تشرين الأول"),
    listOf("نوفمبر", "تشرين الثاني"),
    listOf("ديسمبر", "كانون الأول"),
)
