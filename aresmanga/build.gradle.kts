import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(kei.plugins.extension)
}

keiyoushi {
    name = "ARESManga"
    versionCode = 4
    contentWarning = ContentWarning.SAFE
    libVersion = "1.6"

    source {
        name = "أريس مانجا"
        lang = "ar"
        baseUrl = "https://fl-ares.com"
        // Site moved from WordPress (MangaThemesia) to a custom Next.js app
        versionId = 2
    }

    deeplink {
        path("/manga/..*")
    }
}
