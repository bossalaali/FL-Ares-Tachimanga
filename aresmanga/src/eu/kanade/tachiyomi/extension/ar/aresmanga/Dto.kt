package eu.kanade.tachiyomi.extension.ar.aresmanga

import kotlinx.serialization.Serializable

@Serializable
class CatalogDto(
    val items: List<CatalogItemDto> = emptyList(),
    val hasMore: Boolean = false,
)

@Serializable
class CatalogItemDto(
    val title: String,
    val slug: String,
    val poster: String? = null,
)

@Serializable
class MangaDto(
    val slug: String,
    val title: String,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val genreLabels: Map<String, String> = emptyMap(),
    val author: String? = null,
    val artist: String? = null,
    val status: String? = null,
    val altNames: List<String> = emptyList(),
    val chapters: List<ChapterDto> = emptyList(),
)

@Serializable
class ChapterDto(
    val number: Float,
    val title: String? = null,
    val createdAt: String? = null,
    val translator: String? = null,
    val isLocked: Boolean = false,
    val team: TeamDto? = null,
)

@Serializable
class TeamDto(
    val name: String,
)
