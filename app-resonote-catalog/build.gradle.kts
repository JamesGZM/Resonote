plugins {
    alias(libs.plugins.resonote.android.application)
    alias(libs.plugins.resonote.android.application.compose)
}

android {
    namespace = "com.resonote.catalog"

    defaultConfig {
        applicationId = "com.resonote.catalog"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)
}
