package eu.kanade.tachiyomi.extension.ar.aresmanga

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
import keiyoushi.utils.tryParse
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document
import kotlin.time.Instant

@Source
abstract class AresManga : KeiSource() {

    // ============================== Catalog ==============================

    private suspend fun catalog(page: Int, query: String = "", sort: String? = null): MangasPage {
        val url = "$baseUrl/api/catalog".toHttpUrl().newBuilder().apply {
            if (query.isNotBlank()) addQueryParameter("q", query.trim())
            if (sort != null) addQueryParameter("sort", sort)
            addQueryParameter("page", page.toString())
        }.build()
        val data = client.get(url, Fetch.XHR).parseAs<CatalogDto>()
        val mangas = data.items.map { item ->
            SManga.create().apply {
                url = "/manga/${item.slug}"
                title = item.title
                thumbnail_url = item.poster
            }
        }
        return MangasPage(mangas, data.hasMore)
    }

    override suspend fun getPopularManga(page: Int) = catalog(page)

    override suspend fun getLatestUpdates(page: Int) = catalog(page, sort = "updated_at")

    override suspend fun getSearchMangaList(page: Int, query: String, filters: FilterList) = catalog(page, query)

    override suspend fun getMangaByUrl(url: HttpUrl): SManga? {
        val slug = url.pathSegments.getOrNull(1)?.takeIf { url.pathSegments.firstOrNull() == "manga" } ?: return null
        val manga = SManga.create().apply { this.url = "/manga/$slug" }
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
        val dto = document.flightData().extractObject("\"chapters\":[").parseAs<MangaDto>()

        val details = if (fetchDetails) {
            SManga.create().apply {
                url = manga.url
                title = dto.title
                description = buildString {
                    append(dto.description.orEmpty())
                    if (dto.altNames.isNotEmpty()) {
                        append("\n\nأسماء أخرى: ")
                        append(dto.altNames.joinToString())
                    }
                }.trim()
                author = dto.author?.takeIf(String::isNotBlank)
                artist = dto.artist?.takeIf(String::isNotBlank)
                genre = dto.genres.map { dto.genreLabels[it] ?: it }.joinToString()
                status = when (dto.status) {
                    "Ongoing" -> SManga.ONGOING
                    "Completed" -> SManga.COMPLETED
                    "Hiatus", "Paused" -> SManga.ON_HIATUS
                    "Cancelled", "Dropped" -> SManga.CANCELLED
                    else -> SManga.UNKNOWN
                }
                thumbnail_url = document.selectFirst("img[src*=/media/covers/]:not(.wallpaper-img)")?.absUrl("src")
                    ?: manga.thumbnail_url
            }
        } else {
            manga
        }

        val chapterList = if (fetchChapters) {
            dto.chapters
                .filterNot { it.isLocked }
                .distinctBy { it.number }
                .map { ch ->
                    val num = ch.number.toString().removeSuffix(".0")
                    SChapter.create().apply {
                        url = "/manga/${dto.slug}/chapter/$num"
                        name = buildString {
                            append("الفصل $num")
                            if (!ch.title.isNullOrBlank()) append(" - ${ch.title}")
                        }
                        chapter_number = ch.number
                        scanlator = ch.team?.name ?: ch.translator?.takeIf(String::isNotBlank)
                        date_upload = Instant.tryParse(ch.createdAt?.removePrefix("\$D"))
                    }
                }
        } else {
            chapters
        }

        return SMangaUpdate(manga = details, chapters = chapterList)
    }

    // =============================== Pages ===============================

    override fun getChapterUrl(chapter: SChapter): String = baseUrl + chapter.url

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val document = client.get(baseUrl + chapter.url, Fetch.DOCUMENT).asJsoup()
        val data = document.flightData()
        val start = data.indexOf("\"pages\":[")
        if (start == -1) throw Exception("الفصل مقفل أو غير متاح، افتحه في WebView")
        val array = data.substring(start + "\"pages\":".length).let { it.substring(0, it.matchingBracketEnd() + 1) }
        val pages = array.parseAs<List<String>>()
        if (pages.isEmpty()) throw Exception("الفصل مقفل أو غير متاح، افتحه في WebView")
        return pages.mapIndexed { i, img -> Page(i, imageUrl = img) }
    }

    // ============================= Utilities =============================

    /** Joins the Next.js flight (RSC) payload chunks embedded in the page into one string. */
    private fun Document.flightData(): String = select("script").mapNotNull { script ->
        val data = script.data()
        if (!data.startsWith(FLIGHT_PREFIX)) return@mapNotNull null
        data.removePrefix(FLIGHT_PREFIX).substringBeforeLast("])").parseAs<String>()
    }.joinToString("")

    /** Returns the JSON object that encloses the first occurrence of [marker]. */
    private fun String.extractObject(marker: String): String {
        val markerIndex = indexOf(marker)
        if (markerIndex == -1) throw Exception("تعذر قراءة بيانات العمل")
        var start = lastIndexOf('{', markerIndex)
        while (start != -1) {
            val end = runCatching { substring(start).matchingBracketEnd() + start }.getOrNull()
            if (end != null && end > markerIndex) return substring(start, end + 1)
            start = lastIndexOf('{', start - 1)
        }
        throw Exception("تعذر قراءة بيانات العمل")
    }

    /** Index of the bracket closing the one at position 0, skipping string contents. */
    private fun String.matchingBracketEnd(): Int {
        var depth = 0
        var inString = false
        var escaped = false
        for (i in indices) {
            val c = this[i]
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
                continue
            }
            when (c) {
                '"' -> inString = true
                '{', '[' -> depth++
                '}', ']' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        throw Exception("تعذر قراءة بيانات الصفحة")
    }
}

private const val FLIGHT_PREFIX = "self.__next_f.push([1,"
