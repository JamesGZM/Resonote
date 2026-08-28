plugins {
    alias(libs.plugins.resonote.android.library)
    alias(libs.plugins.resonote.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.resonote.core.data"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.database)
    implementation(projects.core.media.local)
    implementation(projects.core.media.karaoke)
    implementation(projects.core.network)
    implementation(projects.core.datastore)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.lyrics.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
}
