package eu.kanade.tachiyomi.extension.ar.alaashiq

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import okhttp3.HttpUrl

interface UrlFilter {
    fun addTo(url: HttpUrl.Builder)
}

open class SelectFilter(
    name: String,
    private val param: String,
    private val options: List<Pair<String, String>>,
) : Filter.Select<String>(name, options.map { it.first }.toTypedArray()),
    UrlFilter {
    val value get() = options[state].second
    override fun addTo(url: HttpUrl.Builder) {
        if (value.isNotEmpty()) url.addQueryParameter(param, value)
    }
}

class SortFilter :
    SelectFilter(
        "الترتيب",
        "m_orderby",
        listOf(
            "الأكثر مشاهدة" to "views",
            "آخر تحديث" to "latest",
            "الرائج" to "trending",
            "التقييم" to "rating",
            "أبجدي" to "alphabet",
            "الأحدث إضافة" to "new-manga",
            "حسب الصلة (للبحث)" to "",
        ),
    )

class OperatorFilter :
    SelectFilter(
        "شروط التصنيف",
        "op",
        listOf("أو (واحد على الأقل)" to "", "و (كل التصنيفات)" to "1"),
    )

class AdultFilter :
    SelectFilter(
        "محتوى للبالغين",
        "adult",
        listOf("الكل" to "", "بدون محتوى للبالغين" to "0", "للبالغين فقط" to "1"),
    )

class TextFilter(name: String, private val param: String) :
    Filter.Text(name),
    UrlFilter {
    override fun addTo(url: HttpUrl.Builder) {
        if (state.isNotBlank()) url.addQueryParameter(param, state.trim())
    }
}

class CheckBox(name: String, val value: String) : Filter.CheckBox(name)

/** [encoded] values are already percent-encoded by the site, so they are added as-is. */
class CheckBoxGroup(
    name: String,
    private val param: String,
    options: List<Pair<String, String>>,
    private val encoded: Boolean = false,
) : Filter.Group<CheckBox>(name, options.map { CheckBox(it.first, it.second) }),
    UrlFilter {
    override fun addTo(url: HttpUrl.Builder) {
        state.filter { it.state }.forEach {
            if (encoded) url.addEncodedQueryParameter(param, it.value) else url.addQueryParameter(param, it.value)
        }
    }
}

fun alAashiqFilters() = FilterList(
    SortFilter(),
    CheckBoxGroup("التصنيفات", "genre[]", GENRES, encoded = true),
    OperatorFilter(),
    CheckBoxGroup(
        "الحالة",
        "status[]",
        listOf(
            "مستمرة" to "on-going",
            "مكتملة" to "end",
            "متوقفة" to "on-hold",
            "ملغاة" to "canceled",
            "لم تُنشر بعد" to "upcoming",
        ),
    ),
    AdultFilter(),
    TextFilter("الكاتب", "author"),
    TextFilter("الرسام", "artist"),
    TextFilter("سنة الإصدار", "release"),
)

private val GENRES = listOf(
    "100%" to "100",
    "3asq" to "3asq",
    "99%" to "99",
    "أكشن" to "action",
    "إيتشي" to "ecchi",
    "إيسيكاي" to "%d8%a5%d9%8a%d8%b3%d9%8a%d9%83%d8%a7%d9%8a",
    "استراتيجي" to "%d8%a7%d8%b3%d8%aa%d8%b1%d8%a7%d8%aa%d9%8a%d8%ac%d9%8a",
    "اضطهاد" to "%d8%a7%d8%b6%d8%b7%d9%87%d8%a7%d8%af",
    "العاب" to "%d8%a7%d9%84%d8%b9%d8%a7%d8%a8",
    "تاريخ" to "historical",
    "تبادل أجناس" to "gender-bender",
    "تسلق" to "%d8%aa%d8%b3%d9%84%d9%82",
    "جريمة" to "%d8%ac%d8%b1%d9%8a%d9%85%d8%a9",
    "جوائز" to "%d8%ac%d9%88%d8%a7%d8%a6%d8%b2",
    "جوسي" to "josei",
    "حرب" to "%d8%ad%d8%b1%d8%a8",
    "حريم" to "harem",
    "حكايات شعبية" to "%d8%ad%d9%83%d8%a7%d9%8a%d8%a7%d8%aa-%d8%b4%d8%b9%d8%a8%d9%8a%d8%a9",
    "خارق للطبيعة" to "supernatural",
    "خيال" to "fantasy",
    "خيال علمي" to "sci-fi",
    "دراما" to "drama",
    "رعب" to "horror",
    "رواية خفيفة" to "%d8%b1%d9%88%d8%a7%d9%8a%d8%a9-%d8%ae%d9%81%d9%8a%d9%81%d8%a9",
    "رومانسية" to "romance",
    "رياضة" to "sports",
    "ساموراي" to "%d8%b3%d8%a7%d9%85%d9%88%d8%b1%d8%a7%d9%8a",
    "سينين" to "seinen",
    "شريحة من الحياة" to "slice-of-life",
    "شوجو" to "shoujo",
    "شوغي" to "%d8%b4%d9%88%d8%ba%d9%8a",
    "شونين" to "shounen",
    "شياطين" to "demons",
    "عسكرية" to "military",
    "علم نفس" to "psychological",
    "عنف" to "%d8%b9%d9%86%d9%81",
    "غموض" to "mystery",
    "فضائيِّين" to "%d9%81%d8%b6%d8%a7%d8%a6%d9%8a%d9%90%d9%91%d9%8a%d9%86",
    "فلسفة" to "%d9%81%d9%84%d8%b3%d9%81%d8%a9",
    "فنون قتالية" to "martial-arts",
    "قوى خارقة" to "super-powers",
    "كوميديا" to "comedy",
    "مأساة" to "tragedy",
    "مدرسة" to "school-life",
    "مغامرة" to "adventure",
    "ميكا" to "mecha",
    "نفسي" to "%d9%86%d9%81%d8%b3%d9%8a",
    "نينجا" to "%d9%86%d9%8a%d9%86%d8%ac%d8%a7",
    "ون شوت" to "%d9%88%d9%86-%d8%b4%d9%88%d8%aa",
    "ويب-تون" to "%d9%88%d9%8a%d8%a8-%d8%aa%d9%88%d9%86",
)
