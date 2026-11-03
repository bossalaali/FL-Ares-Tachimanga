package eu.kanade.tachiyomi.extension.ar.mangawy

import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BrowseDto(
    val items: List<SeriesDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
)

@Serializable
class PaginationDto(
    val page: Int = 1,
    @SerialName("total_pages") val totalPages: Int = 1,
)

@Serializable
class SeriesDto(
    val slug: String,
    private val path: String? = null,
    private val title: String,
    @SerialName("title_ar") private val titleAr: String? = null,
    private val description: String? = null,
    @SerialName("cover_url") private val coverUrl: String? = null,
    private val status: String? = null,
    private val type: String? = null,
    private val genres: List<String> = emptyList(),
) {
    fun toSManga() = SManga.create().apply {
        url = path ?: "/series/$slug"
        title = this@SeriesDto.title
        thumbnail_url = coverUrl
        description = buildString {
            append(this@SeriesDto.description.orEmpty())
            if (!titleAr.isNullOrBlank()) append("\n\nالاسم العربي: $titleAr")
        }.trim()
        genre = (listOfNotNull(type?.replaceFirstChar(Char::uppercase)) + genres).joinToString()
        status = when (this@SeriesDto.status) {
            "ongoing" -> SManga.ONGOING
            "completed" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
    }
}

@Serializable
class FiltersDto(
    val genres: List<NameSlugDto> = emptyList(),
)

@Serializable
class NameSlugDto(
    val name: String,
    val slug: String,
)
