package com.resonote.feature.settings.impl

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        item(key = "low-band") {
            EqualizerBandControl(
                copy = lowCopy,
                value = lowDb,
                enabled = enabled,
                onValueChange = {
                    lowDb = it
                    customEditing = true
                },
                onValueChangeFinished = {
                    onGainsChange(lowDb.roundToInt(), midDb.roundToInt(), highDb.roundToInt())
                },
                modifier = Modifier.fillMaxWidth().height(136.dp),
                testTag = "equalizer-low",
            )
        }
        item(key = "mid-band") {
            EqualizerBandControl(
                copy = midCopy,
                value = midDb,
                enabled = enabled,
                onValueChange = {
                    midDb = it
                    customEditing = true
                },
                onValueChangeFinished = {
                    onGainsChange(lowDb.roundToInt(), midDb.roundToInt(), highDb.roundToInt())
                },
                modifier = Modifier.fillMaxWidth().height(136.dp),
                testTag = "equalizer-mid",
            )
        }
        item(key = "high-band") {
            EqualizerBandControl(
                copy = highCopy,
                value = highDb,
                enabled = enabled,
                onValueChange = {
                    highDb = it
                    customEditing = true
                },
                onValueChangeFinished = {
                    onGainsChange(lowDb.roundToInt(), midDb.roundToInt(), highDb.roundToInt())
                },
                modifier = Modifier.fillMaxWidth().height(136.dp),
                testTag = "equalizer-high",
            )
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

@Composable
private fun EqualizerBandControl(
    copy: EqualizerBandCopy,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = copy.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = copy.range,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                text = gainLabel(value.roundToInt(), includeUnit = true),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        ResonoteEqualizerSlider(
            value = value,
            enabled = enabled,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .testTag(testTag),
        )
    }
}

@Composable
private fun ResonoteEqualizerSlider(
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant
    val notch = MaterialTheme.colorScheme.onPrimary
    val zeroMarker = MaterialTheme.colorScheme.onSurfaceVariant
    val snappedValue = value.roundToInt().coerceIn(-12, 12)

    Box(
        modifier = modifier
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(snappedValue.toFloat(), -12f..12f, 23)
                if (!enabled) disabled()
                setProgress { target ->
                    if (!enabled) return@setProgress false
                    onValueChange(target.roundToInt().coerceIn(-12, 12).toFloat())
                    onValueChangeFinished()
                    true
                }
            }
            .pointerInput(enabled, onValueChange, onValueChangeFinished) {
                if (!enabled) return@pointerInput
                fun updateValue(x: Float) {
                    val fraction = (x / size.width).coerceIn(0f, 1f)
                    onValueChange((-12f + fraction * 24f).roundToInt().toFloat())
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    updateValue(down.position.x)
                    drag(down.id) { change ->
                        change.consume()
                        updateValue(change.position.x)
                    }
                    onValueChangeFinished()
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val startX = 2.dp.toPx()
            val endX = size.width - 2.dp.toPx()
            val trackY = size.height * 0.42f
            val zeroX = (startX + endX) / 2f
            val valueX = startX + (endX - startX) * ((snappedValue + 12f) / 24f)
            val trackColor = if (enabled) track else track.copy(alpha = 0.45f)
            val activeColor = if (enabled) primary else primary.copy(alpha = 0.38f)

            drawLine(
                color = trackColor,
                start = Offset(startX, trackY),
                end = Offset(endX, trackY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            repeat(9) { index ->
                val x = startX + (endX - startX) * index / 8f
                val tickHalfHeight = if (index % 2 == 0) 5.dp.toPx() else 3.dp.toPx()
                drawLine(
                    color = trackColor,
                    start = Offset(x, trackY - tickHalfHeight),
                    end = Offset(x, trackY + tickHalfHeight),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            drawLine(
                color = activeColor,
                start = Offset(zeroX, trackY),
                end = Offset(valueX, trackY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
            )

            val diamondRadius = 4.5.dp.toPx()
            val diamond = Path().apply {
                moveTo(zeroX, trackY - diamondRadius)
                lineTo(zeroX + diamondRadius, trackY)
                lineTo(zeroX, trackY + diamondRadius)
                lineTo(zeroX - diamondRadius, trackY)
                close()
            }
            drawPath(diamond, zeroMarker.copy(alpha = 0.8f))

            val thumbWidth = 14.dp.toPx()
            val thumbHeight = 28.dp.toPx()
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(valueX - thumbWidth / 2f, trackY - thumbHeight / 2f),
                size = Size(thumbWidth, thumbHeight),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
            drawLine(
                color = notch.copy(alpha = if (enabled) 0.85f else 0.45f),
                start = Offset(valueX, trackY - 6.dp.toPx()),
                end = Offset(valueX, trackY + 6.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(-12, -6, 0, 6, 12).forEach { mark ->
                Text(
                    text = gainLabel(mark),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
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
