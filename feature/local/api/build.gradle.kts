plugins {
    alias(libs.plugins.resonote.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.resonote.feature.local.api"
}

dependencies {
    implementation(libs.androidx.navigation3.runtime)
}
