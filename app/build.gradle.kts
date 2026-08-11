plugins {
    alias(libs.plugins.resonote.android.application)
    alias(libs.plugins.resonote.android.application.compose)
    alias(libs.plugins.resonote.hilt)
}

android {
    namespace = "com.resonote.app"

    defaultConfig {
        applicationId = "com.resonote.app"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)
}
