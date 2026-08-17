plugins {
    alias(libs.plugins.resonote.android.library)
    alias(libs.plugins.resonote.hilt)
}

android {
    namespace = "com.resonote.core.playback.service"
}

dependencies {
    api(projects.core.playback.api)
    implementation(projects.core.data)
    implementation(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
}
