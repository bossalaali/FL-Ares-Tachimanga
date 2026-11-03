import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(kei.plugins.extension)
}

keiyoushi {
    name = "Mangawy"
    versionCode = 1
    contentWarning = ContentWarning.SAFE
    libVersion = "1.6"

    source {
        name = "مانجاوي"
        lang = "ar"
        baseUrl = "https://mangawy.org"
    }

    deeplink {
        path("/series/..*")
    }
}
