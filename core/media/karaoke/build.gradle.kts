plugins {
    alias(libs.plugins.resonote.android.library)
    alias(libs.plugins.resonote.hilt)
}

android {
    namespace = "com.resonote.core.media.karaoke"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
}
