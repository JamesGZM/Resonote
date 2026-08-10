plugins {
    alias(libs.plugins.resonote.android.library)
    alias(libs.plugins.resonote.android.library.compose)
}

android {
    namespace = "com.resonote.core.screenshottesting"
}

dependencies {
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.roborazzi)
    api(libs.roborazzi.accessibility.check)
    implementation(projects.core.designsystem)
}
