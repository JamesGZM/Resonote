@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.settings.impl

import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onPrivacyClick: () -> Unit = {},
    onLicenseClick: () -> Unit = {},
    onLibrariesClick: () -> Unit = {},
    bottomContentPadding: Dp = 32.dp,
    viewModel: AboutSettingsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            .orEmpty()
    }
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(version) { viewModel.checkForUpdates(version) }

    AboutSettingsScreen(
        version = version,
        updateState = updateState,
        onBack = onBack,
        onPrivacyClick = onPrivacyClick,
        onLicenseClick = onLicenseClick,
        onLibrariesClick = onLibrariesClick,
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
        resetting = (settingsState as? SettingsUiState.Ready)?.savingKey == SettingsSaveKey.Reset,
        onReset = settingsViewModel::resetSettings,
    )
}

@Composable
internal fun AboutSettingsScreen(
    version: String,
    updateState: AboutUpdateState,
    onBack: () -> Unit,
    onProjectClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onPrivacyClick: () -> Unit = {},
    onLicenseClick: () -> Unit = {},
    onLibrariesClick: () -> Unit = {},
    bottomContentPadding: Dp = 32.dp,
    resetting: Boolean = false,
    onReset: () -> Unit = {},
) {
    var confirmReset by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = { Text(stringResource(R.string.feature_settings_impl_about_section)) },
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
                contentPadding = PaddingValues(bottom = bottomContentPadding),
            ) {
                item {
                    SettingsVersionRow(
                        title = stringResource(R.string.feature_settings_impl_version_title),
                        version = version,
                        loading = updateState == AboutUpdateState.Checking,
                        updateAvailable = updateState is AboutUpdateState.Available,
                        onClick = onUpdateClick,
                    )
                }
                item { SettingsDivider() }
                item {
                    SettingsActionRow(
                        title = stringResource(R.string.feature_settings_impl_project_link),
                        onClick = onProjectClick,
                    )
                }
                item { SettingsDivider() }
                item {
                    SettingsActionRow(
                        title = stringResource(R.string.feature_settings_impl_privacy),
                        onClick = onPrivacyClick,
                    )
                }
                item { SettingsDivider() }
                item {
                    SettingsActionRow(
                        title = stringResource(R.string.feature_settings_impl_license),
                        onClick = onLicenseClick,
                    )
                }
                item { SettingsDivider() }
                item {
                    SettingsActionRow(
                        title = stringResource(R.string.feature_settings_impl_open_source_libraries),
                        onClick = onLibrariesClick,
                    )
                }
                item { SettingsDivider() }
                item {
                    SettingsActionRow(
                        title = stringResource(R.string.feature_settings_impl_reset),
                        supportingText = stringResource(R.string.feature_settings_impl_reset_summary),
                        onClick = { confirmReset = true },
                        destructive = true,
                        loading = resetting,
                    )
                }
            }
        }
    }
    if (confirmReset) {
        ConfirmationDialog(
            title = stringResource(R.string.feature_settings_impl_reset),
            body = stringResource(R.string.feature_settings_impl_reset_confirm),
            confirm = stringResource(R.string.feature_settings_impl_reset_action),
            onConfirm = {
                confirmReset = false
                onReset()
            },
            onDismiss = { confirmReset = false },
        )
    }
}

private fun android.content.Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

private const val PROJECT_URL = "https://github.com/JamesGZM/Resonote"
