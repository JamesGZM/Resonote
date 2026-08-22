@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.settings.impl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.model.ThemeMode

@Composable
internal fun PlaybackSpeedDialog(selected: PlaybackSpeed, onSelect: (PlaybackSpeed) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Speed, contentDescription = null) },
        title = { Text(stringResource(R.string.feature_settings_impl_playback_speed)) },
        text = {
            Column {
                PlaybackSpeed.entries.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = speed == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(speed) },
                            )
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = speed == selected, onClick = null)
                        Text(speed.label(), modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.feature_settings_impl_cancel))
            }
        },
    )
}

@Composable
internal fun OnlineQualityDialog(
    selected: OnlinePlaybackQuality,
    onSelect: (OnlinePlaybackQuality) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
        title = { Text(stringResource(R.string.feature_settings_impl_online_quality)) },
        text = {
            Column {
                OnlinePlaybackQuality.entries.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = quality == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(quality) },
                            )
                            .padding(horizontal = 4.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = quality == selected, onClick = null)
                        Text(quality.label(), modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.feature_settings_impl_cancel))
            }
        },
    )
}

@Composable
internal fun ThemeModeDialog(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DarkMode, contentDescription = null) },
        title = { Text(stringResource(R.string.feature_settings_impl_theme_mode)) },
        text = {
            Column {
                ThemeMode.entries.forEach { themeMode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = themeMode == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(themeMode) },
                            )
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = themeMode == selected, onClick = null)
                        Text(themeMode.label(), modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.feature_settings_impl_cancel))
            }
        },
    )
}

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
