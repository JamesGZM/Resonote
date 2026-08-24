@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.settings.impl

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import com.resonote.core.model.LyricsBackgroundMode
import com.resonote.core.model.LyricsDisplayMode
import com.resonote.core.model.LyricsFontSize
import com.resonote.core.model.LyricsHighlightMode
import com.resonote.core.model.LyricsSupplementalText
import com.resonote.core.model.LyricsTextAlignment
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackSpeed

@Composable
fun PlaybackSettingsRoute(
    onBack: () -> Unit,
    bottomContentPadding: Dp = 32.dp,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarController = LocalResonoteSnackbarController.current
    val saveFailureMessage = stringResource(R.string.feature_settings_impl_save_error)

    LaunchedEffect((state as? SettingsUiState.Ready)?.saveFailed) {
        if ((state as? SettingsUiState.Ready)?.saveFailed == true) {
            snackbarController?.show(saveFailureMessage)
            viewModel.acknowledgeSaveFailure()
        }
    }

    PlaybackSettingsScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        bottomContentPadding = bottomContentPadding,
        onPlaybackSpeedChange = viewModel::setPlaybackSpeed,
        onOnlinePlaybackQualityChange = viewModel::setOnlinePlaybackQuality,
        onPlaybackModeChange = viewModel::setPlaybackMode,
        onGaplessChange = viewModel::setGaplessEnabled,
        onCrossfadeChange = viewModel::setCrossfadeDuration,
        onLoudnessChange = viewModel::setLoudnessNormalizationEnabled,
        onAudioFocusChange = viewModel::setAudioFocusPolicy,
    )
}

private enum class PlaybackSettingsSheet { Quality, Mode, Crossfade, Speed, AudioFocus }

@Composable
internal fun PlaybackSettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 32.dp,
    onPlaybackSpeedChange: (PlaybackSpeed) -> Unit = {},
    onOnlinePlaybackQualityChange: (OnlinePlaybackQuality) -> Unit = {},
    onPlaybackModeChange: (PlaybackMode) -> Unit = {},
    onGaplessChange: (Boolean) -> Unit = {},
    onCrossfadeChange: (CrossfadeDuration) -> Unit = {},
    onLoudnessChange: (Boolean) -> Unit = {},
    onAudioFocusChange: (AudioFocusPolicy) -> Unit = {},
) {
    var openSheet by remember { mutableStateOf<PlaybackSettingsSheet?>(null) }
    val ready = state as? SettingsUiState.Ready

    SettingsPageScaffold(
        title = stringResource(R.string.feature_settings_impl_playback_section),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (state) {
                SettingsUiState.Loading -> SettingsLoading()
                SettingsUiState.LoadFailed -> SettingsActionRow(
                    title = stringResource(R.string.feature_settings_impl_load_error),
                    value = stringResource(R.string.feature_settings_impl_retry),
                    onClick = onRetry,
                    modifier = Modifier.widthIn(max = 720.dp),
                )
                is SettingsUiState.Ready -> LazyColumn(
                    modifier = Modifier.widthIn(max = 720.dp).testTag("playback-settings-list"),
                    contentPadding = PaddingValues(bottom = bottomContentPadding),
                ) {
                    item {
                        SettingsValueRow(
                            stringResource(R.string.feature_settings_impl_online_quality),
                            state.onlinePlaybackQuality.label(),
                            { openSheet = PlaybackSettingsSheet.Quality },
                            Modifier.testTag("settings-online-quality"),
                            enabled = state.savingKey == null,
                            supportingText = stringResource(R.string.feature_settings_impl_online_quality_body),
                        )
                    }
                    item { SettingsDivider() }
                    item {
                        SettingsValueRow(
                            stringResource(R.string.feature_settings_impl_playback_mode),
                            state.playbackMode.label(),
                            { openSheet = PlaybackSettingsSheet.Mode },
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
                            { openSheet = PlaybackSettingsSheet.Crossfade },
                            enabled = state.savingKey == null,
                        )
                    }
                    item { SettingsDivider() }
                    item {
                        SettingsValueRow(
                            stringResource(R.string.feature_settings_impl_playback_speed),
                            state.playbackSpeed.label(),
                            { openSheet = PlaybackSettingsSheet.Speed },
                            Modifier.testTag("settings-playback-speed"),
                            enabled = state.savingKey == null,
                            supportingText = stringResource(R.string.feature_settings_impl_playback_speed_body),
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
                            { openSheet = PlaybackSettingsSheet.AudioFocus },
                            enabled = state.savingKey == null,
                        )
                    }
                }
            }
        }
    }

    ready?.let { current ->
        when (openSheet) {
            PlaybackSettingsSheet.Quality -> choiceSheet(
                stringResource(R.string.feature_settings_impl_online_quality),
                current.onlinePlaybackQuality,
                OnlinePlaybackQuality.entries,
                { it.label() },
                {
                    openSheet = null
                    onOnlinePlaybackQualityChange(it)
                },
            ) { openSheet = null }
            PlaybackSettingsSheet.Mode -> choiceSheet(
                stringResource(R.string.feature_settings_impl_playback_mode),
                current.playbackMode,
                PlaybackMode.entries,
                { it.label() },
                {
                    openSheet = null
                    onPlaybackModeChange(it)
                },
            ) { openSheet = null }
            PlaybackSettingsSheet.Crossfade -> choiceSheet(
                stringResource(R.string.feature_settings_impl_crossfade),
                current.crossfadeDuration,
                CrossfadeDuration.entries,
                { it.label() },
                {
                    openSheet = null
                    onCrossfadeChange(it)
                },
            ) { openSheet = null }
            PlaybackSettingsSheet.Speed -> choiceSheet(
                stringResource(R.string.feature_settings_impl_playback_speed),
                current.playbackSpeed,
                PlaybackSpeed.entries,
                { it.label() },
                {
                    openSheet = null
                    onPlaybackSpeedChange(it)
                },
            ) { openSheet = null }
            PlaybackSettingsSheet.AudioFocus -> choiceSheet(
                stringResource(R.string.feature_settings_impl_audio_focus),
                current.audioFocusPolicy,
                AudioFocusPolicy.entries,
                { it.label() },
                {
                    openSheet = null
                    onAudioFocusChange(it)
                },
            ) { openSheet = null }
            null -> Unit
        }
    }
}

private enum class LyricsSettingsSheet { Supplemental, Display, Highlight, Alignment, FontSize, Background }

@Composable
fun LyricsSettingsRoute(
    onBack: () -> Unit,
    bottomContentPadding: Dp = 32.dp,
    viewModel: LyricsSettingsViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    var openSheet by remember { mutableStateOf<LyricsSettingsSheet?>(null) }
    SettingsPageScaffold(
        title = stringResource(R.string.feature_settings_impl_lyrics_section),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).widthIn(max = 720.dp),
            contentPadding = PaddingValues(bottom = bottomContentPadding),
        ) {
            item {
                SettingsValueRow(
                    stringResource(R.string.feature_settings_impl_lyrics_supplemental),
                    preferences.supplementalText.label(),
                    { openSheet = LyricsSettingsSheet.Supplemental },
                )
            }
            item { SettingsDivider() }
            item {
                SettingsValueRow(
                    stringResource(
                        R.string.feature_settings_impl_lyrics_display,
                    ),
                    preferences.displayMode.label(),
                    {
                        openSheet =
                            LyricsSettingsSheet.Display
                    },
                )
            }
            item { SettingsDivider() }
            item {
                SettingsValueRow(
                    stringResource(
                        R.string.feature_settings_impl_lyrics_highlight,
                    ),
                    preferences.highlightMode.label(),
                    {
                        openSheet =
                            LyricsSettingsSheet.Highlight
                    },
                )
            }
            item { SettingsDivider() }
            item {
                SettingsValueRow(
                    stringResource(
                        R.string.feature_settings_impl_lyrics_alignment,
                    ),
                    preferences.textAlignment.label(),
                    {
                        openSheet =
                            LyricsSettingsSheet.Alignment
                    },
                )
            }
            item { SettingsDivider() }
            item {
                SettingsValueRow(
                    stringResource(
                        R.string.feature_settings_impl_lyrics_font_size,
                    ),
                    preferences.fontSize.label(),
                    {
                        openSheet =
                            LyricsSettingsSheet.FontSize
                    },
                )
            }
            item { SettingsDivider() }
            item {
                SettingsValueRow(
                    stringResource(
                        R.string.feature_settings_impl_lyrics_background,
                    ),
                    preferences.backgroundMode.label(),
                    {
                        openSheet =
                            LyricsSettingsSheet.Background
                    },
                )
            }
        }
    }
    when (openSheet) {
        LyricsSettingsSheet.Supplemental -> choiceSheet(
            stringResource(
                R.string.feature_settings_impl_lyrics_supplemental,
            ),
            preferences.supplementalText,
            LyricsSupplementalText.entries,
            {
                it.label()
            },
            {
                openSheet =
                    null
                viewModel.update(preferences.copy(supplementalText = it))
            },
            { openSheet = null },
        )
        LyricsSettingsSheet.Display -> choiceSheet(
            stringResource(
                R.string.feature_settings_impl_lyrics_display,
            ),
            preferences.displayMode,
            LyricsDisplayMode.entries,
            {
                it.label()
            },
            {
                openSheet =
                    null
                viewModel.update(preferences.copy(displayMode = it))
            },
            { openSheet = null },
        )
        LyricsSettingsSheet.Highlight -> choiceSheet(
            stringResource(
                R.string.feature_settings_impl_lyrics_highlight,
            ),
            preferences.highlightMode,
            LyricsHighlightMode.entries,
            {
                it.label()
            },
            {
                openSheet =
                    null
                viewModel.update(preferences.copy(highlightMode = it))
            },
            { openSheet = null },
        )
        LyricsSettingsSheet.Alignment -> choiceSheet(
            stringResource(
                R.string.feature_settings_impl_lyrics_alignment,
            ),
            preferences.textAlignment,
            LyricsTextAlignment.entries,
            {
                it.label()
            },
            {
                openSheet =
                    null
                viewModel.update(preferences.copy(textAlignment = it))
            },
            { openSheet = null },
        )
        LyricsSettingsSheet.FontSize -> choiceSheet(
            stringResource(
                R.string.feature_settings_impl_lyrics_font_size,
            ),
            preferences.fontSize,
            LyricsFontSize.entries,
            {
                it.label()
            },
            {
                openSheet =
                    null
                viewModel.update(preferences.copy(fontSize = it))
            },
            { openSheet = null },
        )
        LyricsSettingsSheet.Background -> choiceSheet(
            stringResource(
                R.string.feature_settings_impl_lyrics_background,
            ),
            preferences.backgroundMode,
            LyricsBackgroundMode.entries,
            {
                it.label()
            },
            {
                openSheet =
                    null
                viewModel.update(preferences.copy(backgroundMode = it))
            },
            { openSheet = null },
        )
        null -> Unit
    }
}

@Composable private fun LyricsSupplementalText.label() = stringResource(
    if (this ==
        LyricsSupplementalText.Translation
    ) {
        R.string.feature_settings_impl_lyrics_translation
    } else {
        R.string.feature_settings_impl_lyrics_transliteration
    },
)

@Composable private fun LyricsDisplayMode.label() = stringResource(
    if (this ==
        LyricsDisplayMode.Scrolling
    ) {
        R.string.feature_settings_impl_lyrics_scrolling
    } else {
        R.string.feature_settings_impl_lyrics_single_line
    },
)

@Composable private fun LyricsHighlightMode.label() = stringResource(
    if (this ==
        LyricsHighlightMode.Word
    ) {
        R.string.feature_settings_impl_lyrics_word
    } else {
        R.string.feature_settings_impl_lyrics_line
    },
)

@Composable private fun LyricsTextAlignment.label() = stringResource(
    if (this ==
        LyricsTextAlignment.Center
    ) {
        R.string.feature_settings_impl_lyrics_center
    } else {
        R.string.feature_settings_impl_lyrics_start
    },
)

@Composable private fun LyricsFontSize.label() = stringResource(
    when (this) {
        LyricsFontSize.Small -> R.string.feature_settings_impl_small
        LyricsFontSize.Medium -> R.string.feature_settings_impl_medium
        LyricsFontSize.Large -> R.string.feature_settings_impl_large
    },
)

@Composable private fun LyricsBackgroundMode.label() = stringResource(
    when (this) {
        LyricsBackgroundMode.Palette -> R.string.feature_settings_impl_lyrics_palette
        LyricsBackgroundMode.Artwork -> R.string.feature_settings_impl_lyrics_artwork
        LyricsBackgroundMode.Off -> R.string.feature_settings_impl_off
    },
)

@Composable
fun PermissionsSettingsRoute(onBack: () -> Unit, bottomContentPadding: Dp = 32.dp) {
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

    PermissionsSettingsScreen(
        onBack = onBack,
        notificationsEnabled = notificationsEnabled,
        microphoneGranted = microphoneGranted,
        bottomContentPadding = bottomContentPadding,
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
    )
}

@Composable
internal fun PermissionsSettingsScreen(
    onBack: () -> Unit,
    notificationsEnabled: Boolean,
    microphoneGranted: Boolean,
    bottomContentPadding: Dp = 32.dp,
    onNotificationsClick: () -> Unit = {},
    onMicrophoneClick: () -> Unit = {},
) {
    SettingsPageScaffold(
        title = stringResource(R.string.feature_settings_impl_permissions_section),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).widthIn(max = 720.dp),
            contentPadding = PaddingValues(bottom = bottomContentPadding),
        ) {
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
        }
    }
}

@Composable
internal fun SettingsPageScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = { Text(title) },
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
        content = content,
    )
}

@Composable
private fun SettingsLoading() {
    Text(
        stringResource(R.string.feature_settings_impl_loading),
        modifier = Modifier.padding(24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
private fun permissionLabel(granted: Boolean): String = stringResource(
    if (granted) R.string.feature_settings_impl_allowed else R.string.feature_settings_impl_not_allowed,
)
