plugins {
    alias(libs.plugins.resonote.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android { namespace = "com.resonote.feature.player.api" }

dependencies {
    api(libs.androidx.navigation3.runtime)
}
