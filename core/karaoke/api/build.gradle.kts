plugins {
    alias(libs.plugins.resonote.android.library)
}

android { namespace = "com.resonote.core.karaoke" }

dependencies {
    api(projects.core.model)
    api(projects.core.playback.api)
    api(libs.kotlinx.coroutines.core)
}
