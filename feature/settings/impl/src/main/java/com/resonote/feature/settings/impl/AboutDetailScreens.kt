package com.resonote.feature.settings.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PrivacySettingsRoute(onBack: () -> Unit, bottomContentPadding: Dp = 32.dp) {
    AboutTextDetailScreen(
        title = stringResource(R.string.feature_settings_impl_privacy),
        body = stringResource(R.string.feature_settings_impl_privacy_body),
        onBack = onBack,
        bottomContentPadding = bottomContentPadding,
    )
}

@Composable
fun LicenseSettingsRoute(onBack: () -> Unit, bottomContentPadding: Dp = 32.dp) {
    AboutTextDetailScreen(
        title = stringResource(R.string.feature_settings_impl_license),
        body = stringResource(R.string.feature_settings_impl_license_body),
        onBack = onBack,
        bottomContentPadding = bottomContentPadding,
    )
}

@Composable
fun OpenSourceLibrariesRoute(onBack: () -> Unit, bottomContentPadding: Dp = 32.dp) {
    SettingsPageScaffold(
        title = stringResource(R.string.feature_settings_impl_open_source_libraries),
        onBack = onBack,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp),
                contentPadding = PaddingValues(bottom = bottomContentPadding),
            ) {
                items(OPEN_SOURCE_LIBRARIES.size) { index ->
                    val library = OPEN_SOURCE_LIBRARIES[index]
                    SettingsInfoRow(
                        title = library.name,
                        value = library.license,
                        supportingText = stringResource(library.summaryRes),
                    )
                    if (index != OPEN_SOURCE_LIBRARIES.lastIndex) SettingsDivider()
                }
            }
        }
    }
}

@Composable
private fun AboutTextDetailScreen(title: String, body: String, onBack: () -> Unit, bottomContentPadding: Dp) {
    SettingsPageScaffold(title = title, onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = bottomContentPadding),
            ) {
                item {
                    Text(
                        text = body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private data class OpenSourceLibrary(val name: String, val summaryRes: Int, val license: String)

private val OPEN_SOURCE_LIBRARIES = listOf(
    OpenSourceLibrary("AndroidX & Jetpack Compose", R.string.feature_settings_impl_library_androidx, "Apache 2.0"),
    OpenSourceLibrary("AndroidX Media3", R.string.feature_settings_impl_library_media3, "Apache 2.0"),
    OpenSourceLibrary("Dagger Hilt", R.string.feature_settings_impl_library_hilt, "Apache 2.0"),
    OpenSourceLibrary("Coil", R.string.feature_settings_impl_library_coil, "Apache 2.0"),
    OpenSourceLibrary("Retrofit", R.string.feature_settings_impl_library_retrofit, "Apache 2.0"),
    OpenSourceLibrary("OkHttp", R.string.feature_settings_impl_library_okhttp, "Apache 2.0"),
    OpenSourceLibrary("Kotlin Coroutines", R.string.feature_settings_impl_library_coroutines, "Apache 2.0"),
    OpenSourceLibrary("Protocol Buffers", R.string.feature_settings_impl_library_protobuf, "BSD 3-Clause"),
)
