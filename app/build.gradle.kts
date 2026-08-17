import java.util.Properties

plugins {
    alias(libs.plugins.resonote.android.application)
    alias(libs.plugins.resonote.android.application.compose)
    alias(libs.plugins.resonote.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

val localSigningPropertiesFile = rootProject.file("keystore/signing.properties")
val localSigningProperties = Properties().apply {
    if (localSigningPropertiesFile.isFile) {
        localSigningPropertiesFile.inputStream().use(::load)
    }
}

fun signingValue(propertyName: String, environmentName: String): String? =
    providers.environmentVariable(environmentName).orNull
        ?: localSigningProperties.getProperty(propertyName)?.takeIf(String::isNotBlank)

val releaseStorePath = signingValue("storeFile", "RESONOTE_KEYSTORE_PATH")
val releaseStorePassword = signingValue("storePassword", "RESONOTE_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "RESONOTE_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "RESONOTE_KEY_PASSWORD")
val releaseSigningValues = listOf(releaseStorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)

check(releaseSigningValues.all { it == null } || releaseSigningValues.all { it != null }) {
    "Release signing is partially configured. Provide every signing.properties value or every RESONOTE_* variable."
}

android {
    namespace = "com.resonote.app"
    testOptions.unitTests.isIncludeAndroidResources = true

    defaultConfig {
        applicationId = "com.resonote.app"
        versionCode = providers.gradleProperty("resonoteVersionCode").get().toInt()
        versionName = providers.gradleProperty("resonoteVersionName").get()
    }

    signingConfigs {
        create("release") {
            releaseStorePath?.let { storeFile = rootProject.file(it) }
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.named("release").get()
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.navigation)
    implementation(projects.core.data)
    implementation(projects.core.network)
    implementation(projects.core.playback.api)
    implementation(projects.core.playback.service)
    implementation(projects.feature.auth.impl)
    implementation(projects.feature.album.api)
    implementation(projects.feature.album.impl)
    implementation(projects.feature.artist.api)
    implementation(projects.feature.artist.impl)
    implementation(projects.feature.cloud.api)
    implementation(projects.feature.cloud.impl)
    implementation(projects.feature.discover.impl)
    implementation(projects.feature.home.impl)
    implementation(projects.feature.history.api)
    implementation(projects.feature.history.impl)
    implementation(projects.feature.library.impl)
    implementation(projects.feature.local.api)
    implementation(projects.feature.local.impl)
    implementation(projects.feature.player.impl)
    implementation(projects.feature.player.api)
    implementation(projects.feature.playlist.api)
    implementation(projects.feature.playlist.impl)
    implementation(projects.feature.ranking.api)
    implementation(projects.feature.ranking.impl)
    implementation(projects.feature.recognition.api)
    implementation(projects.feature.recognition.impl)
    implementation(projects.feature.search.api)
    implementation(projects.feature.search.impl)
    implementation(projects.feature.settings.api)
    implementation(projects.feature.settings.impl)
    implementation(projects.feature.vip.impl)
    implementation(projects.feature.video.api)
    implementation(projects.feature.video.impl)
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
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    testImplementation(projects.core.screenshotTesting)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
