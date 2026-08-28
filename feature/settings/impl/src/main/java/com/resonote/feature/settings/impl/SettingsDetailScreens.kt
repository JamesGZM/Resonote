@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.settings.impl

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.resonote.core.model.DesktopLyricsControlsTimeout
import com.resonote.core.model.LyricsBackgroundMode
import com.resonote.core.model.LyricsDisplayMode
import com.resonote.core.model.LyricsFontSize
import com.resonote.core.model.LyricsHighlightMode
import com.resonote.core.model.LyricsPreferences
import com.resonote.core.model.LyricsTextAlignment
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackSpeed
import kotlin.math.roundToInt

@Composable
fun PlaybackSettingsRoute(
    onBack: () -> Unit,
    onEqualizerClick: () -> Unit,
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
        onEqualizerClick = onEqualizerClick,
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
    onEqualizerClick: () -> Unit = {},
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
                            title = stringResource(R.string.feature_settings_impl_equalizer),
                            value = state.equalizerPreset.label(),
                            supportingText = stringResource(R.string.feature_settings_impl_equalizer_summary),
                            onClick = onEqualizerClick,
                            modifier = Modifier.testTag("settings-equalizer"),
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

private enum class LyricsSettingsSheet {
    DesktopControlsTimeout,
    DesktopBackgroundColor,
    DesktopForegroundColor,
    DesktopShadowColor,
    DesktopOutlineColor,
    Supplemental,
    Display,
    Highlight,
    Alignment,
    FontSize,
    Background,
}

@Composable
fun DesktopLyricsSettingsRoute(
    onBack: () -> Unit,
    bottomContentPadding: Dp = 32.dp,
    viewModel: LyricsSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    var openSheet by remember { mutableStateOf<LyricsSettingsSheet?>(null) }
    var permissionRevision by remember { mutableStateOf(0) }
    var enableAfterPermission by remember { mutableStateOf(false) }
    var surfaceOpacity by remember(preferences.desktopLyricsSurfaceOpacity) {
        mutableStateOf(preferences.desktopLyricsSurfaceOpacity.toFloat())
    }
    var widthPercent by remember(preferences.desktopLyricsWidthPercent) {
        mutableStateOf(preferences.desktopLyricsWidthPercent.toFloat())
    }
    var fontSizeSp by remember(preferences.desktopLyricsFontSizeSp) {
        mutableStateOf(preferences.desktopLyricsFontSizeSp.toFloat())
    }
    var outlineWidthDp by remember(preferences.desktopLyricsOutlineWidthDp) {
        mutableStateOf(preferences.desktopLyricsOutlineWidthDp)
    }
    var shadowOffsetXDp by remember(preferences.desktopLyricsShadowOffsetXDp) {
        mutableStateOf(preferences.desktopLyricsShadowOffsetXDp)
    }
    var shadowOffsetYDp by remember(preferences.desktopLyricsShadowOffsetYDp) {
        mutableStateOf(preferences.desktopLyricsShadowOffsetYDp)
    }
    var shadowBlurRadiusDp by remember(preferences.desktopLyricsShadowBlurRadiusDp) {
        mutableStateOf(preferences.desktopLyricsShadowBlurRadiusDp)
    }
    val overlayPermissionGranted = remember(permissionRevision) { Settings.canDrawOverlays(context) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    val enableDesktopLyrics = {
        viewModel.setDesktopLyricsEnabled(true)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionRevision++
        val shouldEnable = enableAfterPermission && Settings.canDrawOverlays(context)
        enableAfterPermission = false
        if (shouldEnable) enableDesktopLyrics()
    }
    SettingsPageScaffold(
        title = stringResource(R.string.feature_settings_impl_desktop_lyrics_settings),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).widthIn(max = 720.dp),
            contentPadding = PaddingValues(bottom = bottomContentPadding),
        ) {
            item {
                SettingsSwitchRow(
                    title = stringResource(R.string.feature_settings_impl_desktop_lyrics),
                    checked = preferences.desktopLyricsEnabled && overlayPermissionGranted,
                    onCheckedChange = { enabled ->
                        when {
                            !enabled -> viewModel.setDesktopLyricsEnabled(false)
                            overlayPermissionGranted -> enableDesktopLyrics()
                            else -> {
                                enableAfterPermission = true
                                openOverlayPermissionSettings(context)
                            }
                        }
                    },
                    supportingText = stringResource(
                        if (overlayPermissionGranted) {
                            R.string.feature_settings_impl_desktop_lyrics_body
                        } else {
                            R.string.feature_settings_impl_desktop_lyrics_permission_body
                        },
                    ),
                    switchTestTag = "desktop-lyrics-switch",
                )
            }
            item { SettingsDivider() }
            item {
                SettingsValueRow(
                    stringResource(R.string.feature_settings_impl_desktop_lyrics_background_color),
                    preferences.desktopLyricsBackgroundColorArgb.colorLabel(),
                    { openSheet = LyricsSettingsSheet.DesktopBackgroundColor },
                    Modifier.testTag("desktop-lyrics-background-color"),
                )
            }
            item { SettingsDivider() }
            item {
                SettingsValueRow(
                    stringResource(R.string.feature_settings_impl_desktop_lyrics_foreground_color),
                    preferences.desktopLyricsForegroundColorArgb.colorLabel(),
                    { openSheet = LyricsSettingsSheet.DesktopForegroundColor },
                    Modifier.testTag("desktop-lyrics-foreground-color"),
                )
            }
            item { SettingsDivider() }
            item {
                SettingsSliderRow(
                    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_surface_opacity),
                    valueLabel = stringResource(
                        R.string.feature_settings_impl_desktop_lyrics_percent_value,
                        surfaceOpacity.roundToInt(),
                    ),
                    value = surfaceOpacity,
                    onValueChange = { surfaceOpacity = it },
                    onValueChangeFinished = {
                        viewModel.setDesktopLyricsSurfaceOpacity(surfaceOpacity.roundToInt())
                    },
                    supportingText = stringResource(
                        R.string.feature_settings_impl_desktop_lyrics_surface_opacity_body,
                    ),
                )
            }
            item { SettingsDivider() }
            item {
                SettingsSliderRow(
                    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_width),
                    valueLabel = stringResource(
                        R.string.feature_settings_impl_desktop_lyrics_percent_value,
                        widthPercent.roundToInt(),
                    ),
                    value = widthPercent,
                    onValueChange = { widthPercent = it },
                    onValueChangeFinished = {
                        viewModel.setDesktopLyricsWidthPercent(widthPercent.roundToInt())
                    },
                    valueRange = 40f..100f,
                    steps = 5,
                )
            }
            item { SettingsDivider() }
            item {
                SettingsSliderRow(
                    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_font_size),
                    valueLabel = stringResource(
                        R.string.feature_settings_impl_desktop_lyrics_sp_value,
                        fontSizeSp.roundToInt(),
                    ),
                    value = fontSizeSp,
                    onValueChange = { fontSizeSp = it },
                    onValueChangeFinished = {
                        viewModel.setDesktopLyricsFontSizeSp(fontSizeSp.roundToInt())
                    },
                    valueRange = 16f..40f,
                    steps = 23,
                )
            }
            item { SettingsDivider() }
            item {
                SettingsValueRow(
                    stringResource(R.string.feature_settings_impl_desktop_lyrics_outline_color),
                    preferences.desktopLyricsOutlineColorArgb.colorLabel(),
                    { openSheet = LyricsSettingsSheet.DesktopOutlineColor },
                    Modifier.testTag("desktop-lyrics-outline-color"),
                )
            }
            item { SettingsDivider() }
            item {
                SettingsSliderRow(
                    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_outline_width),
                    valueLabel = outlineWidthDp.dpLabel(),
                    value = outlineWidthDp,
                    onValueChange = { outlineWidthDp = it },
                    onValueChangeFinished = { viewModel.setDesktopLyricsOutlineWidth(outlineWidthDp) },
                    valueRange = 0f..4f,
                    steps = 7,
                )
            }
            item { SettingsDivider() }
            item {
                SettingsValueRow(
                    stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_color),
                    preferences.desktopLyricsShadowColorArgb.colorLabel(),
                    { openSheet = LyricsSettingsSheet.DesktopShadowColor },
                    Modifier.testTag("desktop-lyrics-shadow-color"),
                    supportingText = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_color_body),
                )
            }
            item { SettingsDivider() }
            item {
                SettingsSliderRow(
                    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_x),
                    valueLabel = shadowOffsetXDp.dpLabel(),
                    value = shadowOffsetXDp,
                    onValueChange = { shadowOffsetXDp = it },
                    onValueChangeFinished = { viewModel.setDesktopLyricsShadowOffsetX(shadowOffsetXDp) },
                    valueRange = -8f..8f,
                    steps = 15,
                )
            }
            item { SettingsDivider() }
            item {
                SettingsSliderRow(
                    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_y),
                    valueLabel = shadowOffsetYDp.dpLabel(),
                    value = shadowOffsetYDp,
                    onValueChange = { shadowOffsetYDp = it },
                    onValueChangeFinished = { viewModel.setDesktopLyricsShadowOffsetY(shadowOffsetYDp) },
                    valueRange = -8f..8f,
                    steps = 15,
                )
            }
            item { SettingsDivider() }
            item {
                SettingsSliderRow(
                    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_z),
                    valueLabel = shadowBlurRadiusDp.dpLabel(),
                    value = shadowBlurRadiusDp,
                    onValueChange = { shadowBlurRadiusDp = it },
                    onValueChangeFinished = {
                        viewModel.setDesktopLyricsShadowBlurRadius(shadowBlurRadiusDp)
                    },
                    valueRange = 0f..12f,
                    steps = 11,
                    supportingText = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_z_body),
                )
            }
            item { SettingsDivider() }
            item {
                SettingsValueRow(
                    stringResource(R.string.feature_settings_impl_desktop_lyrics_controls_timeout),
                    preferences.desktopLyricsControlsTimeout.label(),
                    { openSheet = LyricsSettingsSheet.DesktopControlsTimeout },
                    supportingText = stringResource(
                        R.string.feature_settings_impl_desktop_lyrics_controls_timeout_body,
                    ),
                )
            }
            item { SettingsDivider() }
            item {
                SettingsSwitchRow(
                    stringResource(R.string.feature_settings_impl_desktop_lyrics_lock),
                    preferences.desktopLyricsLocked,
                    viewModel::setDesktopLyricsLocked,
                    supportingText = stringResource(R.string.feature_settings_impl_desktop_lyrics_lock_body),
                )
            }
            item { SettingsDivider() }
            item {
                SettingsValueRow(
                    stringResource(R.string.feature_settings_impl_desktop_lyrics_position),
                    stringResource(R.string.feature_settings_impl_desktop_lyrics_reset_position),
                    viewModel::resetDesktopLyricsPosition,
                    supportingText = stringResource(R.string.feature_settings_impl_desktop_lyrics_position_body),
                )
            }
        }
    }
    when (openSheet) {
        LyricsSettingsSheet.DesktopControlsTimeout -> choiceSheet(
            stringResource(R.string.feature_settings_impl_desktop_lyrics_controls_timeout),
            preferences.desktopLyricsControlsTimeout,
            DesktopLyricsControlsTimeout.entries,
            { it.label() },
            {
                openSheet = null
                viewModel.setDesktopLyricsControlsTimeout(it)
            },
            { openSheet = null },
        )
        LyricsSettingsSheet.DesktopBackgroundColor -> DesktopLyricsColorSheet(
            title = stringResource(R.string.feature_settings_impl_desktop_lyrics_background_color),
            subtitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_color_sheet_body),
            colorArgb = preferences.desktopLyricsBackgroundColorArgb,
            onSelect = {
                openSheet = null
                viewModel.setDesktopLyricsBackgroundColor(it)
            },
            onDismiss = { openSheet = null },
        )
        LyricsSettingsSheet.DesktopForegroundColor -> DesktopLyricsColorSheet(
            title = stringResource(R.string.feature_settings_impl_desktop_lyrics_foreground_color),
            subtitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_color_sheet_body),
            colorArgb = preferences.desktopLyricsForegroundColorArgb,
            onSelect = {
                openSheet = null
                viewModel.setDesktopLyricsForegroundColor(it)
            },
            onDismiss = { openSheet = null },
        )
        LyricsSettingsSheet.DesktopShadowColor -> DesktopLyricsColorSheet(
            title = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_color),
            subtitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_color_sheet_body),
            colorArgb = preferences.desktopLyricsShadowColorArgb,
            onSelect = {
                openSheet = null
                viewModel.setDesktopLyricsShadowColor(it)
            },
            onDismiss = { openSheet = null },
        )
        LyricsSettingsSheet.DesktopOutlineColor -> DesktopLyricsColorSheet(
            title = stringResource(R.string.feature_settings_impl_desktop_lyrics_outline_color),
            subtitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_color_sheet_body),
            colorArgb = preferences.desktopLyricsOutlineColorArgb,
            onSelect = {
                openSheet = null
                viewModel.setDesktopLyricsOutlineColor(it)
            },
            onDismiss = { openSheet = null },
        )
        else -> Unit
    }
}

private fun Int.colorLabel(): String = "#%06X".format(this and 0xFFFFFF)

@Composable
private fun Float.dpLabel(): String = stringResource(
    R.string.feature_settings_impl_desktop_lyrics_dp_value,
    this,
)

@Composable
fun LyricsSettingsRoute(
    onBack: () -> Unit,
    onDesktopLyricsClick: () -> Unit = {},
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
                    stringResource(R.string.feature_settings_impl_desktop_lyrics_settings),
                    stringResource(
                        if (preferences.desktopLyricsEnabled) {
                            R.string.feature_settings_impl_on
                        } else {
                            R.string.feature_settings_impl_off
                        },
                    ),
                    onDesktopLyricsClick,
                    supportingText = stringResource(R.string.feature_settings_impl_desktop_lyrics_controller_body),
                    modifier = Modifier.testTag("desktop-lyrics-settings"),
                )
            }
            item { SettingsDivider() }
            item {
                SettingsValueRow(
                    stringResource(R.string.feature_settings_impl_lyrics_supplemental),
                    preferences.supplementalTextLabel(),
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
        LyricsSettingsSheet.DesktopControlsTimeout,
        LyricsSettingsSheet.DesktopBackgroundColor,
        LyricsSettingsSheet.DesktopForegroundColor,
        LyricsSettingsSheet.DesktopShadowColor,
        LyricsSettingsSheet.DesktopOutlineColor,
        -> Unit
        LyricsSettingsSheet.Supplemental -> LyricsSupplementalTextSheet(
            translationEnabled = preferences.translationEnabled,
            transliterationEnabled = preferences.transliterationEnabled,
            onTranslationEnabledChange = viewModel::setTranslationEnabled,
            onTransliterationEnabledChange = viewModel::setTransliterationEnabled,
            onDismiss = { openSheet = null },
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

@Composable
private fun DesktopLyricsControlsTimeout.label() = stringResource(
    when (this) {
        DesktopLyricsControlsTimeout.ThreeSeconds -> R.string.feature_settings_impl_desktop_lyrics_timeout_3
        DesktopLyricsControlsTimeout.FiveSeconds -> R.string.feature_settings_impl_desktop_lyrics_timeout_5
        DesktopLyricsControlsTimeout.EightSeconds -> R.string.feature_settings_impl_desktop_lyrics_timeout_8
    },
)

@Composable
private fun LyricsPreferences.supplementalTextLabel() = stringResource(
    when {
        translationEnabled && transliterationEnabled ->
            R.string.feature_settings_impl_lyrics_translation_and_transliteration
        translationEnabled -> R.string.feature_settings_impl_lyrics_translation
        transliterationEnabled -> R.string.feature_settings_impl_lyrics_transliteration
        else -> R.string.feature_settings_impl_lyrics_supplemental_off
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
    val overlayGranted = remember(permissionRevision) { Settings.canDrawOverlays(context) }

    PermissionsSettingsScreen(
        onBack = onBack,
        notificationsEnabled = notificationsEnabled,
        microphoneGranted = microphoneGranted,
        overlayGranted = overlayGranted,
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
        onOverlayClick = { openOverlayPermissionSettings(context) },
    )
}

@Composable
internal fun PermissionsSettingsScreen(
    onBack: () -> Unit,
    notificationsEnabled: Boolean,
    microphoneGranted: Boolean,
    overlayGranted: Boolean,
    bottomContentPadding: Dp = 32.dp,
    onNotificationsClick: () -> Unit = {},
    onMicrophoneClick: () -> Unit = {},
    onOverlayClick: () -> Unit = {},
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
            item { SettingsDivider() }
            item {
                SettingsValueRow(
                    stringResource(R.string.feature_settings_impl_display_over_other_apps),
                    permissionLabel(overlayGranted),
                    onOverlayClick,
                )
            }
        }
    }
}

private fun openOverlayPermissionSettings(context: android.content.Context) {
    val overlayIntent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
    )
    runCatching { context.startActivity(overlayIntent) }.getOrElse {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
        )
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
