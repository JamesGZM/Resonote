@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.settings.impl

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteTopAppBar

@Composable
fun AboutSettingsRoute(
    onBack: () -> Unit,
    bottomContentPadding: Dp = 32.dp,
    viewModel: AboutSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            .orEmpty()
    }
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    LaunchedEffect(version) { viewModel.checkForUpdates(version) }

    AboutSettingsScreen(
        version = version,
        updateState = updateState,
        onBack = onBack,
        bottomContentPadding = bottomContentPadding,
        onProjectClick = { context.openUrl(PROJECT_URL) },
        onUpdateClick = {
            val releaseUrl = (updateState as? AboutUpdateState.Available)?.release?.releaseUrl
            if (releaseUrl != null) {
                context.openUrl(releaseUrl)
            } else {
                viewModel.checkForUpdates(version, force = true)
            }
        },
    )
}

@Composable
internal fun AboutSettingsScreen(
    version: String,
    updateState: AboutUpdateState,
    onBack: () -> Unit,
    onProjectClick: () -> Unit,
    onUpdateClick: () -> Unit,
    bottomContentPadding: Dp = 32.dp,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = { Text(stringResource(R.string.feature_settings_impl_about)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.feature_settings_impl_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = bottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    Text("Resonote", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        stringResource(R.string.feature_settings_impl_version, version),
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    SettingsActionRow(
                        title = stringResource(R.string.feature_settings_impl_check_updates),
                        value = when (updateState) {
                            AboutUpdateState.Checking -> null
                            is AboutUpdateState.Latest -> stringResource(
                                R.string.feature_settings_impl_update_latest,
                            )
                            is AboutUpdateState.Available -> stringResource(
                                R.string.feature_settings_impl_update_available,
                                updateState.release.version,
                            )
                            AboutUpdateState.Failed -> stringResource(
                                R.string.feature_settings_impl_update_failed,
                            )
                        },
                        loading = updateState == AboutUpdateState.Checking,
                        onClick = onUpdateClick,
                    )
                }
                item {
                    SettingsActionRow(
                        title = stringResource(R.string.feature_settings_impl_project_link),
                        onClick = onProjectClick,
                    )
                }
                item {
                    Text(
                        stringResource(R.string.feature_settings_impl_privacy),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.feature_settings_impl_privacy_body),
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                item {
                    Text(
                        stringResource(R.string.feature_settings_impl_license),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.feature_settings_impl_license_body),
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun android.content.Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

private const val PROJECT_URL = "https://github.com/JamesGZM/Resonote"
