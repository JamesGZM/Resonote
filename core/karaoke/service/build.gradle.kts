plugins {
    alias(libs.plugins.resonote.android.library)
    alias(libs.plugins.resonote.hilt)
}

android { namespace = "com.resonote.core.karaoke.service" }

dependencies {
    implementation(projects.core.karaoke.api)
    implementation(projects.core.data)
    implementation(projects.core.model)
    implementation(projects.core.playback.api)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.work.ktx)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
