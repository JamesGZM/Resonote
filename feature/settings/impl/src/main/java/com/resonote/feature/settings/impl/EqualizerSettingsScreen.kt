package com.resonote.feature.settings.impl

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonoteLoadingState
import com.resonote.core.designsystem.component.ResonoteRotaryKnob
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
                    top = padding.calculateTopPadding(),
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
    val presets = remember {
        listOf(EqualizerPreset.Off, EqualizerPreset.Custom) +
            EqualizerPreset.entries.filterNot { it == EqualizerPreset.Off || it == EqualizerPreset.Custom }
    }
    val lowCopy = EqualizerBandCopy.from(stringResource(R.string.feature_settings_impl_equalizer_low))
    val midCopy = EqualizerBandCopy.from(stringResource(R.string.feature_settings_impl_equalizer_mid))
    val highCopy = EqualizerBandCopy.from(stringResource(R.string.feature_settings_impl_equalizer_high))
    val enabled = state.savingKey == null

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("equalizer-settings-list"),
        contentPadding = contentPadding,
    ) {
        item(key = "response-chart") {
            EqualizerResponseChart(
                lowDb = lowDb,
                midDb = midDb,
                highDb = highDb,
                lowLabel = lowCopy.title,
                midLabel = midCopy.title,
                highLabel = highCopy.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .padding(start = 20.dp, top = 8.dp, end = 20.dp),
            )
        }
        item(key = "presets") {
            EqualizerPresetTabs(
                presets = presets,
                selectedPreset = selectedPreset,
                enabled = enabled,
                onPresetChange = { preset ->
                    if (preset == EqualizerPreset.Custom) {
                        customEditing = true
                    } else {
                        lowDb = preset.lowDb.toFloat()
                        midDb = preset.midDb.toFloat()
                        highDb = preset.highDb.toFloat()
                        customEditing = false
                    }
                    onPresetChange(preset)
                },
                modifier = Modifier.height(52.dp),
            )
        }
        item(key = "bands") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 18.dp),
            ) {
                ResonoteRotaryKnob(
                    title = lowCopy.title,
                    valueLabel = gainLabel(lowDb.roundToInt()),
                    value = lowDb,
                    enabled = enabled,
                    onValueChange = {
                        lowDb = it
                        customEditing = true
                    },
                    onValueChangeFinished = {
                        onGainsChange(it.roundToInt(), midDb.roundToInt(), highDb.roundToInt())
                    },
                    valueRange = -12f..12f,
                    steps = 23,
                    modifier = Modifier.weight(1f).testTag("equalizer-low"),
                )
                ResonoteRotaryKnob(
                    title = midCopy.title,
                    valueLabel = gainLabel(midDb.roundToInt()),
                    value = midDb,
                    enabled = enabled,
                    onValueChange = {
                        midDb = it
                        customEditing = true
                    },
                    onValueChangeFinished = {
                        onGainsChange(lowDb.roundToInt(), it.roundToInt(), highDb.roundToInt())
                    },
                    valueRange = -12f..12f,
                    steps = 23,
                    modifier = Modifier.weight(1f).testTag("equalizer-mid"),
                )
                ResonoteRotaryKnob(
                    title = highCopy.title,
                    valueLabel = gainLabel(highDb.roundToInt()),
                    value = highDb,
                    enabled = enabled,
                    onValueChange = {
                        highDb = it
                        customEditing = true
                    },
                    onValueChangeFinished = {
                        onGainsChange(lowDb.roundToInt(), midDb.roundToInt(), it.roundToInt())
                    },
                    valueRange = -12f..12f,
                    steps = 23,
                    modifier = Modifier.weight(1f).testTag("equalizer-high"),
                )
            }
        }
    }
}

@Composable
private fun EqualizerResponseChart(
    lowDb: Float,
    midDb: Float,
    highDb: Float,
    lowLabel: String,
    midLabel: String,
    highLabel: String,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val guide = MaterialTheme.colorScheme.outlineVariant
    val labels = listOf(lowLabel to lowDb, midLabel to midDb, highLabel to highDb)

    Column(modifier.testTag("equalizer-response-chart")) {
        Row(Modifier.fillMaxWidth().height(40.dp)) {
            labels.forEach { (label, value) ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = gainLabel(value.roundToInt(), includeUnit = true),
                        color = primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val lowX = size.width / 6f
            val midX = size.width / 2f
            val highX = size.width * 5f / 6f
            val zeroY = size.height * 0.58f
            val amplitude = size.height * 0.34f
            fun yFor(value: Float) = zeroY - (value.coerceIn(-12f, 12f) / 12f) * amplitude

            val lowY = yFor(lowDb)
            val midY = yFor(midDb)
            val highY = yFor(highDb)
            val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx()))

            listOf(lowX, midX, highX).forEach { x ->
                drawLine(
                    color = guide.copy(alpha = 0.7f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash,
                )
            }
            drawLine(
                color = guide,
                start = Offset(0f, zeroY),
                end = Offset(size.width, zeroY),
                strokeWidth = 1.dp.toPx(),
            )

            val curve = Path().apply {
                moveTo(0f, lowY)
                lineTo(lowX, lowY)
                cubicTo(
                    lowX + size.width * 0.12f,
                    lowY,
                    midX - size.width * 0.14f,
                    midY,
                    midX,
                    midY,
                )
                cubicTo(
                    midX + size.width * 0.14f,
                    midY,
                    highX - size.width * 0.12f,
                    highY,
                    highX,
                    highY,
                )
                lineTo(size.width, highY)
            }
            val fill = Path().apply {
                moveTo(0f, zeroY)
                lineTo(0f, lowY)
                lineTo(lowX, lowY)
                cubicTo(
                    lowX + size.width * 0.12f,
                    lowY,
                    midX - size.width * 0.14f,
                    midY,
                    midX,
                    midY,
                )
                cubicTo(
                    midX + size.width * 0.14f,
                    midY,
                    highX - size.width * 0.12f,
                    highY,
                    highX,
                    highY,
                )
                lineTo(size.width, highY)
                lineTo(size.width, zeroY)
                close()
            }
            drawPath(fill, primary.copy(alpha = 0.08f))
            drawPath(
                path = curve,
                color = primary,
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun EqualizerPresetTabs(
    presets: List<EqualizerPreset>,
    selectedPreset: EqualizerPreset,
    enabled: Boolean,
    onPresetChange: (EqualizerPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth().testTag("equalizer-presets"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        items(presets, key = EqualizerPreset::name) { preset ->
            val selected = preset == selectedPreset
            Surface(
                onClick = { onPresetChange(preset) },
                enabled = enabled,
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .height(40.dp)
                    .testTag("equalizer-preset-${preset.name}")
                    .semantics {
                        this.selected = selected
                        role = Role.Tab
                    },
                shape = RoundedCornerShape(20.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.background
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = preset.label(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private data class EqualizerBandCopy(val title: String, val range: String) {
    companion object {
        fun from(value: String): EqualizerBandCopy {
            val parts = value.split(" · ", limit = 2)
            return EqualizerBandCopy(
                title = parts.first(),
                range = parts.getOrElse(1) { "" },
            )
        }
    }
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

private fun gainLabel(value: Int, includeUnit: Boolean = false): String {
    val signed = if (value > 0) "+$value" else "$value"
    return if (includeUnit) "$signed dB" else signed
}
