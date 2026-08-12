plugins {
    alias(libs.plugins.resonote.android.library)
    alias(libs.plugins.resonote.android.library.compose)
    alias(libs.plugins.resonote.hilt)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.resonote.feature.video.impl"
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
    implementation(projects.feature.video.api)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui.compose.material3)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(projects.core.screenshotTesting)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
