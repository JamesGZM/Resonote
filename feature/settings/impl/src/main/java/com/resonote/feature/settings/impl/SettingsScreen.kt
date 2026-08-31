@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.settings.impl

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.ThemeMode

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onPlaybackClick: () -> Unit = {},
    onLyricsClick: () -> Unit = {},
    onPermissionsClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    bottomContentPadding: Dp = 32.dp,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarController = LocalResonoteSnackbarController.current
    val saveFailureMessage = stringResource(R.string.feature_settings_impl_save_error)
    val language = if (LocalConfiguration.current.locales[0].language == "zh") {
        AppLanguage.SimplifiedChinese
    } else {
        AppLanguage.English
    }

    LaunchedEffect((state as? SettingsUiState.Ready)?.saveFailed) {
        if ((state as? SettingsUiState.Ready)?.saveFailed == true) {
            snackbarController?.show(saveFailureMessage)
            viewModel.acknowledgeSaveFailure()
        }
    }

    SettingsScreen(
        state = state,
        bottomContentPadding = bottomContentPadding,
        onBack = onBack,
        onRetry = viewModel::retry,
        onPlaybackClick = onPlaybackClick,
        onLyricsClick = onLyricsClick,
        onPermissionsClick = onPermissionsClick,
        onDownloadsClick = onDownloadsClick,
        onAboutClick = onAboutClick,
        onThemeModeChange = viewModel::setThemeMode,
        onDynamicColorChange = viewModel::setDynamicColorEnabled,
        onClearCache = viewModel::clearCache,
        language = language,
        onLanguageChange = { selected ->
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selected.languageTag))
        },
        onLogout = viewModel::logout,
    )
}

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 32.dp,
    onPlaybackClick: () -> Unit = {},
    onLyricsClick: () -> Unit = {},
    onPermissionsClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    onClearCache: () -> Unit = {},
    language: AppLanguage = AppLanguage.English,
    onLanguageChange: (AppLanguage) -> Unit = {},
    onLogout: () -> Unit = {},
    supportsDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
) {
    var showAppearance by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var confirmClearCache by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val ready = state as? SettingsUiState.Ready

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = { Text(stringResource(R.string.feature_settings_impl_title)) },
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
    ) { scaffoldPadding ->
        Box(Modifier.fillMaxSize().padding(scaffoldPadding), contentAlignment = Alignment.TopCenter) {
            when (state) {
                SettingsUiState.Loading -> Text(
                    stringResource(R.string.feature_settings_impl_loading),
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingsUiState.LoadFailed -> SettingsActionRow(
                    title = stringResource(R.string.feature_settings_impl_load_error),
                    value = stringResource(R.string.feature_settings_impl_retry),
                    onClick = onRetry,
                    modifier = Modifier.widthIn(max = 720.dp),
                )
                is SettingsUiState.Ready -> LazyColumn(
                    modifier = Modifier.fillMaxHeight().widthIn(max = 720.dp).testTag("settings-list"),
                    contentPadding = PaddingValues(top = 8.dp, bottom = bottomContentPadding),
                ) {
                    item {
                        SettingsNavigationRow(
                            title = stringResource(R.string.feature_settings_impl_appearance_section),
                            supportingText = stringResource(R.string.feature_settings_impl_appearance_summary),
                            icon = Icons.Rounded.Palette,
                            onClick = { showAppearance = true },
                            modifier = Modifier.testTag("settings-appearance"),
                        )
                    }
                    item { SettingsDivider() }
                    item {
                        SettingsNavigationRow(
                            title = stringResource(R.string.feature_settings_impl_language),
                            supportingText = stringResource(R.string.feature_settings_impl_language_summary),
                            value = language.label(),
                            icon = Icons.Rounded.Language,
                            onClick = { showLanguage = true },
                            modifier = Modifier.testTag("settings-language"),
                        )
                    }
                    item { SettingsDivider() }
                    item {
                        SettingsNavigationRow(
                            title = stringResource(R.string.feature_settings_impl_playback_section),
                            supportingText = stringResource(R.string.feature_settings_impl_playback_summary),
                            icon = Icons.Rounded.PlayCircle,
                            onClick = onPlaybackClick,
                            modifier = Modifier.testTag("settings-playback"),
                        )
                    }
                    item { SettingsDivider() }
                    item {
                        SettingsNavigationRow(
                            title = stringResource(R.string.feature_settings_impl_lyrics_section),
                            supportingText = stringResource(R.string.feature_settings_impl_lyrics_summary),
                            icon = Icons.Rounded.Lyrics,
                            onClick = onLyricsClick,
                        )
                    }
                    item { SettingsDivider() }
                    item {
                        SettingsNavigationRow(
                            title = stringResource(R.string.feature_settings_impl_download_management),
                            supportingText = stringResource(R.string.feature_settings_impl_download_management_summary),
                            icon = Icons.Rounded.Download,
                            onClick = onDownloadsClick,
                            modifier = Modifier.testTag("settings-downloads"),
                        )
                    }
                    item { SettingsDivider() }
                    item {
                        SettingsNavigationRow(
                            title = stringResource(R.string.feature_settings_impl_cache_title),
                            supportingText = stringResource(R.string.feature_settings_impl_cache_body),
                            value = state.cacheBytes?.let(::formatBytes)
                                ?: stringResource(R.string.feature_settings_impl_calculating),
                            icon = Icons.Rounded.Storage,
                            onClick = { confirmClearCache = true },
                            loading = state.isClearingCache,
                        )
                    }
                    item { SettingsDivider() }
                    item {
                        SettingsNavigationRow(
                            title = stringResource(R.string.feature_settings_impl_permissions_section),
                            supportingText = stringResource(R.string.feature_settings_impl_permissions_summary),
                            icon = Icons.Rounded.Security,
                            onClick = onPermissionsClick,
                        )
                    }
                    item { SettingsDivider() }
                    item {
                        SettingsNavigationRow(
                            title = stringResource(R.string.feature_settings_impl_about_section),
                            supportingText = stringResource(R.string.feature_settings_impl_about_summary),
                            icon = Icons.Rounded.Info,
                            onClick = onAboutClick,
                        )
                    }
                    if (state.isAuthenticated) {
                        item { Spacer(Modifier.height(24.dp)) }
                        item {
                            OutlinedButton(
                                onClick = { confirmLogout = true },
                                enabled = state.savingKey != SettingsSaveKey.Logout,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .testTag("settings-logout"),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                            ) {
                                Text(
                                    stringResource(R.string.feature_settings_impl_logout),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAppearance && ready != null) {
        AppearanceSettingsSheet(
            themeMode = ready.themePreferences.themeMode,
            dynamicColorEnabled = ready.themePreferences.dynamicColorEnabled,
            supportsDynamicColor = supportsDynamicColor,
            enabled = ready.savingKey == null,
            onThemeModeChange = onThemeModeChange,
            onDynamicColorChange = onDynamicColorChange,
            onDismiss = { showAppearance = false },
        )
    }
    if (showLanguage) {
        SettingsSingleChoiceSheet(
            title = stringResource(R.string.feature_settings_impl_language),
            selected = language,
            options = AppLanguage.entries.map { it to it.label() },
            onSelect = { selected ->
                showLanguage = false
                onLanguageChange(selected)
            },
            onDismiss = { showLanguage = false },
        )
    }
    if (confirmClearCache) {
        ConfirmationDialog(
            title = stringResource(R.string.feature_settings_impl_clear_cache),
            body = stringResource(R.string.feature_settings_impl_clear_cache_confirm),
            confirm = stringResource(R.string.feature_settings_impl_clear),
            onConfirm = {
                confirmClearCache = false
                onClearCache()
            },
            onDismiss = { confirmClearCache = false },
        )
    }
    if (confirmLogout) {
        ConfirmationDialog(
            title = stringResource(R.string.feature_settings_impl_logout),
            body = stringResource(R.string.feature_settings_impl_logout_confirm),
            confirm = stringResource(R.string.feature_settings_impl_logout),
            confirmTestTag = "settings-logout-confirm",
            onConfirm = {
                confirmLogout = false
                onLogout()
            },
            onDismiss = { confirmLogout = false },
        )
    }
}

@Composable
internal fun ConfirmationDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmTestTag: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = confirmTestTag?.let { Modifier.testTag(it) } ?: Modifier,
            ) {
                Text(confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_settings_impl_cancel)) }
        },
    )
}

internal fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
