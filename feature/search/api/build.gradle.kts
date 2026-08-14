plugins {
    alias(libs.plugins.resonote.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.resonote.feature.search.api"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.serialization.json)
}
