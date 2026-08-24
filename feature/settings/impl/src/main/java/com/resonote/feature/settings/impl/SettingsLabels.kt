package com.resonote.feature.settings.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.resonote.core.model.AudioFocusPolicy
import com.resonote.core.model.CrossfadeDuration
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.model.ThemeMode

internal enum class AppLanguage(val languageTag: String) {
    English("en"),
    SimplifiedChinese("zh-CN"),
}

@Composable
internal fun AppLanguage.label(): String = stringResource(
    when (this) {
        AppLanguage.English -> R.string.feature_settings_impl_language_english
        AppLanguage.SimplifiedChinese -> R.string.feature_settings_impl_language_simplified_chinese
    },
)

@Composable
internal fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.feature_settings_impl_theme_system
        ThemeMode.LIGHT -> R.string.feature_settings_impl_theme_light
        ThemeMode.DARK -> R.string.feature_settings_impl_theme_dark
        ThemeMode.AMOLED -> R.string.feature_settings_impl_theme_amoled
    },
)

@Composable
internal fun OnlinePlaybackQuality.label(): String = stringResource(
    when (this) {
        OnlinePlaybackQuality.Standard -> R.string.feature_settings_impl_quality_standard
        OnlinePlaybackQuality.HighQuality -> R.string.feature_settings_impl_quality_high
        OnlinePlaybackQuality.Lossless -> R.string.feature_settings_impl_quality_lossless
        OnlinePlaybackQuality.HighResolution -> R.string.feature_settings_impl_quality_hi_res
        OnlinePlaybackQuality.ViperAtmos -> R.string.feature_settings_impl_quality_atmos
        OnlinePlaybackQuality.ViperClear -> R.string.feature_settings_impl_quality_clear
        OnlinePlaybackQuality.ViperTape -> R.string.feature_settings_impl_quality_tape
    },
)

@Composable
internal fun PlaybackSpeed.label(): String = stringResource(
    R.string.feature_settings_impl_speed_value,
    when (this) {
        PlaybackSpeed.Half -> "0.5"
        PlaybackSpeed.ThreeQuarters -> "0.75"
        PlaybackSpeed.Normal -> "1"
        PlaybackSpeed.OneAndQuarter -> "1.25"
        PlaybackSpeed.OneAndHalf -> "1.5"
        PlaybackSpeed.Double -> "2"
    },
)

@Composable
internal fun PlaybackMode.label(): String = stringResource(
    when (this) {
        PlaybackMode.ListLoop -> R.string.feature_settings_impl_mode_list_loop
        PlaybackMode.Shuffle -> R.string.feature_settings_impl_mode_shuffle
        PlaybackMode.SingleLoop -> R.string.feature_settings_impl_mode_single_loop
        PlaybackMode.Sequential -> R.string.feature_settings_impl_mode_sequential
    },
)

@Composable
internal fun CrossfadeDuration.label(): String = stringResource(
    when (this) {
        CrossfadeDuration.Off -> R.string.feature_settings_impl_off
        CrossfadeDuration.ThreeSeconds -> R.string.feature_settings_impl_crossfade_3
        CrossfadeDuration.FiveSeconds -> R.string.feature_settings_impl_crossfade_5
        CrossfadeDuration.EightSeconds -> R.string.feature_settings_impl_crossfade_8
    },
)

@Composable
internal fun AudioFocusPolicy.label(): String = stringResource(
    when (this) {
        AudioFocusPolicy.AllowAll -> R.string.feature_settings_impl_focus_all
        AudioFocusPolicy.AllowMedia -> R.string.feature_settings_impl_focus_media
        AudioFocusPolicy.Disallow -> R.string.feature_settings_impl_focus_disallow
    },
)
