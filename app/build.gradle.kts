plugins {
    alias(libs.plugins.resonote.android.application)
    alias(libs.plugins.resonote.android.application.compose)
    alias(libs.plugins.resonote.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.resonote.app"
    testOptions.unitTests.isIncludeAndroidResources = true

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
    implementation(projects.core.navigation)
    implementation(projects.core.data)
    implementation(projects.feature.auth.impl)
    implementation(projects.feature.album.api)
    implementation(projects.feature.album.impl)
    implementation(projects.feature.artist.api)
    implementation(projects.feature.artist.impl)
    implementation(projects.feature.cloud.api)
    implementation(projects.feature.cloud.impl)
    implementation(projects.feature.discover.impl)
    implementation(projects.feature.home.impl)
    implementation(projects.feature.library.impl)
    implementation(projects.feature.player.impl)
    implementation(projects.feature.playlist.api)
    implementation(projects.feature.playlist.impl)
    implementation(projects.feature.ranking.api)
    implementation(projects.feature.ranking.impl)
    implementation(projects.feature.search.api)
    implementation(projects.feature.search.impl)
    implementation(projects.feature.vip.api)
    implementation(projects.feature.vip.impl)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    testImplementation(projects.core.screenshotTesting)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
