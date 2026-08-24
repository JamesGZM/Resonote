@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.settings.impl

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.AudioFocusPolicy
import com.resonote.core.model.CrossfadeDuration
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.model.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onAboutClick: () -> Unit = {},
    bottomContentPadding: Dp = 32.dp,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarController = LocalResonoteSnackbarController.current
    val saveFailureMessage = stringResource(R.string.feature_settings_impl_save_error)
    val lyricsUnavailableMessage = stringResource(R.string.feature_settings_impl_lyrics_unavailable)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var permissionRevision by remember { mutableStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permissionRevision++ }
    val notificationsEnabled = remember(permissionRevision) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val microphoneGranted = remember(permissionRevision) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
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
        notificationsEnabled = notificationsEnabled,
        microphoneGranted = microphoneGranted,
        onBack = onBack,
        onRetry = viewModel::retry,
        onPlaybackSpeedChange = viewModel::setPlaybackSpeed,
        onOnlinePlaybackQualityChange = viewModel::setOnlinePlaybackQuality,
        onPlaybackModeChange = viewModel::setPlaybackMode,
        onGaplessChange = viewModel::setGaplessEnabled,
        onCrossfadeChange = viewModel::setCrossfadeDuration,
        onLoudnessChange = viewModel::setLoudnessNormalizationEnabled,
        onAudioFocusChange = viewModel::setAudioFocusPolicy,
        onThemeModeChange = viewModel::setThemeMode,
        onDynamicColorChange = viewModel::setDynamicColorEnabled,
        onLyricsClick = { scope.launch { snackbarController?.show(lyricsUnavailableMessage) } },
        onClearCache = viewModel::clearCache,
        onReset = viewModel::resetSettings,
        onNotificationsClick = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        },
        onMicrophoneClick = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
            )
        },
        onAboutClick = onAboutClick,
    )
}

private enum class SettingsSheet { Theme, Quality, Mode, Crossfade, Speed, AudioFocus }

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlaybackSpeedChange: (PlaybackSpeed) -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 32.dp,
    notificationsEnabled: Boolean = true,
    microphoneGranted: Boolean = false,
    onOnlinePlaybackQualityChange: (OnlinePlaybackQuality) -> Unit = {},
    onPlaybackModeChange: (PlaybackMode) -> Unit = {},
    onGaplessChange: (Boolean) -> Unit = {},
    onCrossfadeChange: (CrossfadeDuration) -> Unit = {},
    onLoudnessChange: (Boolean) -> Unit = {},
    onAudioFocusChange: (AudioFocusPolicy) -> Unit = {},
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    onLyricsClick: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onReset: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMicrophoneClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    supportsDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
) {
    var openSheet by remember { mutableStateOf<SettingsSheet?>(null) }
    var confirmClearCache by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
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
                    contentPadding = PaddingValues(bottom = bottomContentPadding),
                ) {
                    item { SettingsSectionLabel(stringResource(R.string.feature_settings_impl_appearance_section)) }
                    item {
                        SettingsValueRow(
                            stringResource(R.string.feature_settings_impl_theme_mode),
                            state.themePreferences.themeMode.label(),
                            { openSheet = SettingsSheet.Theme },
                            Modifier.testTag("settings-theme-mode"),
                            enabled = state.savingKey == null,
                        )
                    }
                    if (supportsDynamicColor) {
                        item { SettingsDivider() }
                        item {
                            SettingsSwitchRow(
                                stringResource(R.string.feature_settings_impl_dynamic_color),
                                state.themePreferences.dynamicColorEnabled,
                                onDynamicColorChange,
                                Modifier.testTag("settings-dynamic-color"),
                                enabled = state.savingKey == null,
                            )
                        }
                    }

                    item { SettingsSectionLabel(stringResource(R.string.feature_settings_impl_playback_section)) }
                    item {
                        SettingsValueRow(
                            stringResource(R.string.feature_settings_impl_online_quality),
                            state.onlinePlaybackQuality.label(),
                            { openSheet = SettingsSheet.Quality },
                            Modifier.testTag("settings-online-quality"),
                            enabled = state.savingKey == null,
                        )
                    }
                    item { SettingsDivider() }
                    item {
                        SettingsValueRow(
                            stringResource(R.string.feature_settings_impl_playback_mode),
                            state.playbackMode.label(),
                            { openSheet = SettingsSheet.Mode },
                            enabled = state.savingKey == null,
                        )
                    }
                    item { SettingsDivider() }
                    item {
                        SettingsSwitchRow(
                            stringResource(R.string.feature_settings_impl_gapless),
                            state.gaplessEnabled,
                            onGaplessChange,
                            enabled = state.savingKey == null,
                        )
                    }
                    item { SettingsDivider() }
                    item {
                        SettingsValueRow(
                            stringResource(R.string.feature_settings_impl_crossfade),
                            state.crossfadeDuration.label(),
                            { openSheet = SettingsSheet.Crossfade },
                            enabled = state.savingKey == null,
                        )
                    }
                    item { SettingsDivider() }
                    item {
                        SettingsValueRow(
                            stringResource(R.string.feature_settings_impl_playback_speed),
                            state.playbackSpeed.label(),
                            { openSheet = SettingsSheet.Speed },
                            Modifier.testTag("settings-playback-speed"),
                            enabled = state.savingKey == null,
                        )
                    }
                    item { SettingsDivider() }
                    item {
                        SettingsSwitchRow(
                            stringResource(R.string.feature_settings_impl_loudness),
                            state.loudnessNormalizationEnabled,
                            onLoudnessChange,
                            enabled = state.savingKey == null,
                        )
                    }
                    item { SettingsDivider() }
                    item {
                        SettingsValueRow(
                            stringResource(R.string.feature_settings_impl_audio_focus),
                            state.audioFocusPolicy.label(),
                            { openSheet = SettingsSheet.AudioFocus },
                            enabled = state.savingKey == null,
                        )
                    }

                    item { SettingsSectionLabel(stringResource(R.string.feature_settings_impl_lyrics_section)) }
                    item {
                        SettingsActionRow(
                            stringResource(R.string.feature_settings_impl_lyrics_settings),
                            onLyricsClick,
                            value = stringResource(R.string.feature_settings_impl_coming_soon),
                        )
                    }

                    item { SettingsSectionLabel(stringResource(R.string.feature_settings_impl_cache_section)) }
                    item {
                        SettingsActionRow(
                            stringResource(R.string.feature_settings_impl_playback_cache),
                            { confirmClearCache = true },
                            value =
                            state.cacheBytes?.let(::formatBytes)
                                ?: stringResource(R.string.feature_settings_impl_calculating),
                            supportingText = stringResource(R.string.feature_settings_impl_cache_body),
                            loading = state.isClearingCache,
                        )
                    }

                    item { SettingsSectionLabel(stringResource(R.string.feature_settings_impl_permissions_section)) }
                    item {
                        SettingsValueRow(
                            stringResource(R.string.feature_settings_impl_notifications),
                            permissionLabel(notificationsEnabled),
                            onNotificationsClick,
                        )
                    }
                    item { SettingsDivider() }
                    item {
                        SettingsValueRow(
                            stringResource(R.string.feature_settings_impl_microphone),
                            permissionLabel(microphoneGranted),
                            onMicrophoneClick,
                        )
                    }

                    item { SettingsSectionLabel(stringResource(R.string.feature_settings_impl_general_section)) }
                    item {
                        SettingsActionRow(
                            stringResource(R.string.feature_settings_impl_reset),
                            { confirmReset = true },
                            destructive = true,
                            loading = state.savingKey == SettingsSaveKey.Reset,
                        )
                    }

                    item { SettingsSectionLabel(stringResource(R.string.feature_settings_impl_about_section)) }
                    item { SettingsActionRow(stringResource(R.string.feature_settings_impl_about), onAboutClick) }
                }
            }
        }
    }

    ready?.let { current ->
        when (openSheet) {
            SettingsSheet.Theme -> choiceSheet(
                stringResource(
                    R.string.feature_settings_impl_theme_mode,
                ),
                current.themePreferences.themeMode,
                ThemeMode.entries,
                {
                    it.label()
                },
                {
                    openSheet =
                        null
                    onThemeModeChange(it)
                },
            ) { openSheet = null }
            SettingsSheet.Quality -> choiceSheet(
                stringResource(
                    R.string.feature_settings_impl_online_quality,
                ),
                current.onlinePlaybackQuality,
                OnlinePlaybackQuality.entries,
                {
                    it.label()
                },
                {
                    openSheet =
                        null
                    onOnlinePlaybackQualityChange(it)
                },
            ) { openSheet = null }
            SettingsSheet.Mode -> choiceSheet(
                stringResource(
                    R.string.feature_settings_impl_playback_mode,
                ),
                current.playbackMode,
                PlaybackMode.entries,
                {
                    it.label()
                },
                {
                    openSheet =
                        null
                    onPlaybackModeChange(it)
                },
            ) { openSheet = null }
            SettingsSheet.Crossfade -> choiceSheet(
                stringResource(
                    R.string.feature_settings_impl_crossfade,
                ),
                current.crossfadeDuration,
                CrossfadeDuration.entries,
                {
                    it.label()
                },
                {
                    openSheet =
                        null
                    onCrossfadeChange(it)
                },
            ) { openSheet = null }
            SettingsSheet.Speed -> choiceSheet(
                stringResource(
                    R.string.feature_settings_impl_playback_speed,
                ),
                current.playbackSpeed,
                PlaybackSpeed.entries,
                {
                    it.label()
                },
                {
                    openSheet =
                        null
                    onPlaybackSpeedChange(it)
                },
            ) { openSheet = null }
            SettingsSheet.AudioFocus -> choiceSheet(
                stringResource(
                    R.string.feature_settings_impl_audio_focus,
                ),
                current.audioFocusPolicy,
                AudioFocusPolicy.entries,
                {
                    it.label()
                },
                {
                    openSheet =
                        null
                    onAudioFocusChange(it)
                },
            ) { openSheet = null }
            null -> Unit
        }
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

@Composable
private fun <T> choiceSheet(
    title: String,
    selected: T,
    values: List<T>,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = mutableListOf<Pair<T, String>>()
    for (value in values) options += value to label(value)
    SettingsSingleChoiceSheet(title, selected, options, onSelect, onDismiss)
}

@Composable
private fun ConfirmationDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_settings_impl_cancel)) }
        },
    )
}

@Composable
private fun permissionLabel(granted: Boolean): String = stringResource(
    if (granted) R.string.feature_settings_impl_allowed else R.string.feature_settings_impl_not_allowed,
)

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
