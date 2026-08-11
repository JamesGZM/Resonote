plugins {
    alias(libs.plugins.resonote.android.library)
    alias(libs.plugins.resonote.hilt)
}

android {
    namespace = "com.resonote.core.datastore"
}

dependencies {
    api(projects.core.datastoreProto)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
}
