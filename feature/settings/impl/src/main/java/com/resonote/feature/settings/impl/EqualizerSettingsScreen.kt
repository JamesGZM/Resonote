package com.resonote.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonoteLoadingState
import com.resonote.core.model.EqualizerPreset
import kotlin.math.roundToInt

@Composable
fun EqualizerSettingsRoute(
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
    EqualizerSettingsScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onPresetChange = viewModel::setEqualizerPreset,
        onGainsChange = viewModel::setEqualizerGains,
        bottomContentPadding = bottomContentPadding,
    )
}

@Composable
internal fun EqualizerSettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPresetChange: (EqualizerPreset) -> Unit,
    onGainsChange: (Int, Int, Int) -> Unit,
    bottomContentPadding: Dp = 32.dp,
    modifier: Modifier = Modifier,
) {
    SettingsPageScaffold(
        title = stringResource(R.string.feature_settings_impl_equalizer),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        when (state) {
            SettingsUiState.Loading -> ResonoteLoadingState(Modifier.padding(padding))
            SettingsUiState.LoadFailed -> SettingsActionRow(
                title = stringResource(R.string.feature_settings_impl_load_error),
                value = stringResource(R.string.feature_settings_impl_retry),
                onClick = onRetry,
                modifier = Modifier.padding(padding),
            )
            is SettingsUiState.Ready -> EqualizerEditor(
                state = state,
                onPresetChange = onPresetChange,
                onGainsChange = onGainsChange,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    end = 20.dp,
                    bottom = bottomContentPadding,
                ),
            )
        }
    }
}

@Composable
private fun EqualizerEditor(
    state: SettingsUiState.Ready,
    onPresetChange: (EqualizerPreset) -> Unit,
    onGainsChange: (Int, Int, Int) -> Unit,
    contentPadding: PaddingValues,
) {
    var lowDb by remember(state.equalizerLowDb) { mutableFloatStateOf(state.equalizerLowDb.toFloat()) }
    var midDb by remember(state.equalizerMidDb) { mutableFloatStateOf(state.equalizerMidDb.toFloat()) }
    var highDb by remember(state.equalizerHighDb) { mutableFloatStateOf(state.equalizerHighDb.toFloat()) }
    var customEditing by remember(state.equalizerPreset) {
        mutableStateOf(state.equalizerPreset == EqualizerPreset.Custom)
    }
    val selectedPreset = if (customEditing) EqualizerPreset.Custom else state.equalizerPreset
    val presetRows = EqualizerPreset.entries.filterNot { it == EqualizerPreset.Custom }.chunked(2)

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("equalizer-settings-list"),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.feature_settings_impl_equalizer_presets),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.feature_settings_impl_equalizer_presets_body),
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        presetRows.forEach { presets ->
            item(key = presets.joinToString { it.name }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    presets.forEach { preset ->
                        EqualizerPresetCard(
                            preset = preset,
                            selected = preset == selectedPreset,
                            enabled = state.savingKey == null,
                            onClick = {
                                lowDb = preset.lowDb.toFloat()
                                midDb = preset.midDb.toFloat()
                                highDb = preset.highDb.toFloat()
                                customEditing = false
                                onPresetChange(preset)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(R.string.feature_settings_impl_equalizer_custom),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            selectedPreset.label(),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Text(
                        stringResource(R.string.feature_settings_impl_equalizer_custom_body),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    EqualizerBandSlider(
                        label = stringResource(R.string.feature_settings_impl_equalizer_low),
                        value = lowDb,
                        enabled = state.savingKey == null,
                        onValueChange = {
                            lowDb = it
                            customEditing = true
                        },
                        onValueChangeFinished = {
                            onGainsChange(lowDb.roundToInt(), midDb.roundToInt(), highDb.roundToInt())
                        },
                    )
                    EqualizerBandSlider(
                        label = stringResource(R.string.feature_settings_impl_equalizer_mid),
                        value = midDb,
                        enabled = state.savingKey == null,
                        onValueChange = {
                            midDb = it
                            customEditing = true
                        },
                        onValueChangeFinished = {
                            onGainsChange(lowDb.roundToInt(), midDb.roundToInt(), highDb.roundToInt())
                        },
                    )
                    EqualizerBandSlider(
                        label = stringResource(R.string.feature_settings_impl_equalizer_high),
                        value = highDb,
                        enabled = state.savingKey == null,
                        onValueChange = {
                            highDb = it
                            customEditing = true
                        },
                        onValueChangeFinished = {
                            onGainsChange(lowDb.roundToInt(), midDb.roundToInt(), highDb.roundToInt())
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EqualizerPresetCard(
    preset: EqualizerPreset,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 82.dp).testTag("equalizer-preset-${preset.name}"),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(preset.label(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                preset.gainsLabel(),
                modifier = Modifier.padding(top = 4.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun EqualizerBandSlider(
    label: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(gainLabel(value.roundToInt()), color = MaterialTheme.colorScheme.primary)
    }
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        enabled = enabled,
        valueRange = -12f..12f,
        steps = 23,
    )
}

@Composable
internal fun EqualizerPreset.label(): String = stringResource(
    when (this) {
        EqualizerPreset.Off -> R.string.feature_settings_impl_eq_preset_off
        EqualizerPreset.Flat -> R.string.feature_settings_impl_eq_preset_flat
        EqualizerPreset.BassBoost -> R.string.feature_settings_impl_eq_preset_bass
        EqualizerPreset.Pop -> R.string.feature_settings_impl_eq_preset_pop
        EqualizerPreset.Rock -> R.string.feature_settings_impl_eq_preset_rock
        EqualizerPreset.Jazz -> R.string.feature_settings_impl_eq_preset_jazz
        EqualizerPreset.Classical -> R.string.feature_settings_impl_eq_preset_classical
        EqualizerPreset.Vocal -> R.string.feature_settings_impl_eq_preset_vocal
        EqualizerPreset.Custom -> R.string.feature_settings_impl_eq_preset_custom
    },
)

private fun EqualizerPreset.gainsLabel(): String = if (!enabled) {
    "—"
} else {
    listOf(lowDb, midDb, highDb).joinToString("  ") { gainLabel(it) }
}

private fun gainLabel(value: Int): String = if (value > 0) "+$value" else "$value"
