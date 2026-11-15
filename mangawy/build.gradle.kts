import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(kei.plugins.extension)
}

keiyoushi {
    name = "مانجاوي"
    versionCode = 2
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
