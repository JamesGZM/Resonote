@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.settings.impl

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.model.ThemeMode

@Composable
fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
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
