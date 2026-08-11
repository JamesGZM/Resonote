plugins {
    alias(libs.plugins.resonote.android.library)
}

android {
    namespace = "com.resonote.core.model"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
