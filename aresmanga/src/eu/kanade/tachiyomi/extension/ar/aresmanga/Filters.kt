package eu.kanade.tachiyomi.extension.ar.aresmanga

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import okhttp3.HttpUrl

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

class SortFilter :
    SelectFilter(
        "الترتيب",
        "sort",
        listOf(
            "حسب الرواج" to "",
            "آخر تحديث" to "updated_at",
            "الأحدث" to "created_at",
            "عدد الفصول" to "chapters",
            "التقييم" to "rating",
            "عدد الأصوات" to "votes",
        ),
    )

class TypeFilter :
    SelectFilter(
        "النوع",
        "type",
        listOf(
            "الكل" to "",
            "مانجا" to "Manga",
            "مانهوا" to "Manhwa",
            "مانها" to "Manhua",
            "ون شوت" to "One-shot",
            "كوميكس" to "Comics",
            "أخرى" to "Other",
        ),
    )

class StatusFilter :
    SelectFilter(
        "الحالة",
        "status",
        listOf(
            "الكل" to "",
            "مستمرة" to "Ongoing",
            "مكتملة" to "Completed",
            "متوقفة مؤقتاً" to "Hiatus",
            "ملغاة" to "Cancelled",
            "قادمة" to "Announced",
        ),
    )

class GenreFilter :
    SelectFilter(
        "التصنيف",
        "genre",
        listOf("الكل" to "") + listOf(
            "أكشن" to "Action",
            "مغامرة" to "Adventure",
            "كوميديا" to "Comedy",
            "دراما" to "Drama",
            "فانتازيا" to "Fantasy",
            "تاريخي" to "Historical",
            "رعب" to "Horror",
            "فنون قتالية" to "Martial Arts",
            "ميكا" to "Mecha",
            "غموض" to "Mystery",
            "نفسي" to "Psychological",
            "رومانسي" to "Romance",
            "حياة مدرسية" to "School Life",
            "خيال علمي" to "Sci-fi",
            "شريحة من الحياة" to "Slice of Life",
            "رياضي" to "Sports",
            "خارق للطبيعة" to "Supernatural",
            "مأساة" to "Tragedy",
            "شونين" to "Shounen",
            "سينين" to "Seinen",
            "شوجو" to "Shoujo",
            "جوسي" to "Josei",
            "حريم" to "Harem",
            "ناضج" to "Mature",
            "تحول جنسي" to "Gender Bender",
        ),
    )

fun aresFilterList() = FilterList(
    SortFilter(),
    TypeFilter(),
    StatusFilter(),
    GenreFilter(),
)
