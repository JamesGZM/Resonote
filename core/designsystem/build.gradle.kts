plugins {
    alias(libs.plugins.resonote.android.library)
    alias(libs.plugins.resonote.android.library.compose)
}

android {
    namespace = "com.resonote.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
}
