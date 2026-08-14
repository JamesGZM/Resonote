@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.settings.impl

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.ThemeMode

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
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

    SettingsScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onPlaybackSpeedChange = viewModel::setPlaybackSpeed,
        onOnlinePlaybackQualityChange = viewModel::setOnlinePlaybackQuality,
        onThemeModeChange = viewModel::setThemeMode,
        onDynamicColorChange = viewModel::setDynamicColorEnabled,
    )
}

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlaybackSpeedChange: (PlaybackSpeed) -> Unit,
    onOnlinePlaybackQualityChange: (OnlinePlaybackQuality) -> Unit = {},
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    supportsDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    modifier: Modifier = Modifier,
) {
    var speedDialogOpen by remember { mutableStateOf(false) }
    var qualityDialogOpen by remember { mutableStateOf(false) }
    var themeDialogOpen by remember { mutableStateOf(false) }
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("settings-list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "intro") { SettingsIntroCard() }
            if (state is SettingsUiState.Ready) {
                item(key = "appearance-label") {
                    SettingsSectionLabel(R.string.feature_settings_impl_appearance_section)
                }
                item(key = "theme-mode") {
                    ThemeModeSettingsCard(
                        state = state,
                        onClick = { themeDialogOpen = true },
                    )
                }
                if (supportsDynamicColor) {
                    item(key = "dynamic-color") {
                        DynamicColorSettingsCard(
                            state = state,
                            onCheckedChange = onDynamicColorChange,
                        )
                    }
                }
            }
            item(key = "playback-label") {
                SettingsSectionLabel(R.string.feature_settings_impl_playback_section)
            }
            item(key = "playback-speed") {
                when (state) {
                    SettingsUiState.Loading -> PlaybackLoadingCard()
                    SettingsUiState.LoadFailed -> PlaybackLoadFailureCard(onRetry)
                    is SettingsUiState.Ready -> PlaybackSpeedSettingsCard(
                        state = state,
                        onClick = { speedDialogOpen = true },
                    )
                }
            }
            if (state is SettingsUiState.Ready) {
                item(key = "online-quality") {
                    OnlineQualitySettingsCard(
                        state = state,
                        onClick = { qualityDialogOpen = true },
                    )
                }
            }
        }
    }

    if (speedDialogOpen && ready != null) {
        PlaybackSpeedDialog(
            selected = ready.playbackSpeed,
            onSelect = {
                speedDialogOpen = false
                onPlaybackSpeedChange(it)
            },
            onDismiss = { speedDialogOpen = false },
        )
    }
    if (qualityDialogOpen && ready != null) {
        OnlineQualityDialog(
            selected = ready.onlinePlaybackQuality,
            onSelect = {
                qualityDialogOpen = false
                onOnlinePlaybackQualityChange(it)
            },
            onDismiss = { qualityDialogOpen = false },
        )
    }
    if (themeDialogOpen && ready != null) {
        ThemeModeDialog(
            selected = ready.themePreferences.themeMode,
            onSelect = {
                themeDialogOpen = false
                onThemeModeChange(it)
            },
            onDismiss = { themeDialogOpen = false },
        )
    }
}

@Composable
private fun SettingsSectionLabel(textRes: Int) {
    Text(
        text = stringResource(textRes),
        modifier = Modifier.padding(horizontal = 4.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ThemeModeSettingsCard(
    state: SettingsUiState.Ready,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = !state.isSaving,
        modifier = Modifier.fillMaxWidth().testTag("settings-theme-mode"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.DarkMode, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    text = stringResource(R.string.feature_settings_impl_theme_mode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = state.themePreferences.themeMode.label(),
                    modifier = Modifier.padding(top = 3.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun DynamicColorSettingsCard(
    state: SettingsUiState.Ready,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("settings-dynamic-color"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.feature_settings_impl_dynamic_color),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.feature_settings_impl_dynamic_color_body),
                    modifier = Modifier.padding(top = 3.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = state.themePreferences.dynamicColorEnabled,
                enabled = !state.isSaving,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun SettingsIntroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                        ),
                    ),
                )
                .padding(22.dp),
        ) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.26f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(end = 76.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.feature_settings_impl_intro_eyebrow),
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = stringResource(R.string.feature_settings_impl_intro_title),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.feature_settings_impl_intro_body),
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun PlaybackSpeedSettingsCard(
    state: SettingsUiState.Ready,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = !state.isSaving,
        modifier = Modifier.fillMaxWidth().testTag("settings-playback-speed"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    text = stringResource(R.string.feature_settings_impl_playback_speed),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.feature_settings_impl_playback_speed_body),
                    modifier = Modifier.padding(top = 3.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(
                        text = state.playbackSpeed.label(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun OnlineQualitySettingsCard(
    state: SettingsUiState.Ready,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = !state.isSaving,
        modifier = Modifier.fillMaxWidth().testTag("settings-online-quality"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    text = stringResource(R.string.feature_settings_impl_online_quality),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.feature_settings_impl_online_quality_body),
                    modifier = Modifier.padding(top = 3.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = state.onlinePlaybackQuality.label(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun PlaybackLoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(modifier = Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Text(
                text = stringResource(R.string.feature_settings_impl_loading),
                modifier = Modifier.padding(start = 14.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlaybackLoadFailureCard(onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Text(
                text = stringResource(R.string.feature_settings_impl_load_error),
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.feature_settings_impl_retry))
            }
        }
    }
}

@Composable
private fun PlaybackSpeedDialog(
    selected: PlaybackSpeed,
    onSelect: (PlaybackSpeed) -> Unit,
    onDismiss: () -> Unit,
) {
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
private fun OnlineQualityDialog(
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
private fun ThemeModeDialog(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
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
private fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.feature_settings_impl_theme_system
        ThemeMode.LIGHT -> R.string.feature_settings_impl_theme_light
        ThemeMode.DARK -> R.string.feature_settings_impl_theme_dark
        ThemeMode.AMOLED -> R.string.feature_settings_impl_theme_amoled
    },
)

@Composable
private fun OnlinePlaybackQuality.label(): String = stringResource(
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
private fun PlaybackSpeed.label(): String = stringResource(
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
