import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(kei.plugins.extension)
}

keiyoushi {
    name = "Al-Aashiq"
    versionCode = 2
    contentWarning = ContentWarning.SAFE
    libVersion = "1.6"

    source {
        name = "العاشق"
        lang = "ar"
        baseUrl = "https://3asq.online"
    }

    deeplink {
        path("/manga/..*")
    }
}
