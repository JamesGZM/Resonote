plugins {
    alias(libs.plugins.resonote.android.library)
    alias(libs.plugins.resonote.android.library.compose)
    alias(libs.plugins.resonote.hilt)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.resonote.feature.local.impl"
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
    implementation(projects.core.karaoke.api)
    implementation(projects.core.playback.api)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(projects.core.screenshotTesting)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
