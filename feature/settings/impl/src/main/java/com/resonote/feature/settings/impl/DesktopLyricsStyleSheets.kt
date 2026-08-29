@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.settings.impl

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteBottomSheet
import com.resonote.core.designsystem.component.ResonoteBottomSheetHeader
import com.resonote.core.model.DesktopLyricsDefaults
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun DesktopLyricsWidthSheet(
    widthPercent: Int,
    onWidthChange: (Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) = DesktopLyricsKnobSheet(
    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_width),
    subtitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_width_sheet_body),
    knobTitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_width_knob),
    value = widthPercent.toFloat(),
    defaultValue = DesktopLyricsDefaults.WIDTH_PERCENT.toFloat(),
    valueLabel = { desktopLyricsWidthLabel(it.roundToInt()) },
    onValueChangeFinished = { onWidthChange(it.roundToInt()) },
    valueRange = 40f..100f,
    steps = 59,
    testTag = "desktop-lyrics-width-knob",
    onReset = onReset,
    onDismiss = onDismiss,
)

@Composable
internal fun DesktopLyricsFontSizeSheet(
    fontSizeSp: Int,
    onFontSizeChange: (Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) = DesktopLyricsKnobSheet(
    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_font_size),
    subtitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_font_size_sheet_body),
    knobTitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_font_size_knob),
    value = fontSizeSp.toFloat(),
    defaultValue = DesktopLyricsDefaults.FONT_SIZE_SP.toFloat(),
    valueLabel = { desktopLyricsFontSizeLabel(it.roundToInt()) },
    onValueChangeFinished = { onFontSizeChange(it.roundToInt()) },
    valueRange = 16f..40f,
    steps = 23,
    testTag = "desktop-lyrics-font-size-knob",
    onReset = onReset,
    onDismiss = onDismiss,
)

@Composable
internal fun DesktopLyricsOutlineSheet(
    colorArgb: Int,
    widthDp: Float,
    onColorClick: () -> Unit,
    onWidthChange: (Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var width by remember(widthDp) { mutableFloatStateOf(widthDp) }

    ResonoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .testTag("desktop-lyrics-outline-sheet"),
        ) {
            ResonoteBottomSheetHeader(
                title = stringResource(R.string.feature_settings_impl_desktop_lyrics_outline),
                subtitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_outline_sheet_body),
                actions = {
                    RestoreDefaultAction {
                        width = DesktopLyricsDefaults.OUTLINE_WIDTH_DP
                        onReset()
                    }
                },
            )
            SettingsColorRow(
                title = stringResource(R.string.feature_settings_impl_desktop_lyrics_outline_color),
                color = Color(colorArgb),
                onClick = onColorClick,
                modifier = Modifier.testTag("desktop-lyrics-outline-color-picker"),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                RotarySettingKnob(
                    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_outline_thickness),
                    valueLabel = desktopLyricsOutlineLabel(width),
                    value = width,
                    onValueChange = { width = it },
                    onValueChangeFinished = onWidthChange,
                    valueRange = 0f..4f,
                    steps = 39,
                    knobSize = 136.dp,
                    modifier = Modifier.testTag("desktop-lyrics-outline-width-knob"),
                )
            }
        }
    }
}

@Composable
private fun DesktopLyricsKnobSheet(
    title: String,
    subtitle: String,
    knobTitle: String,
    value: Float,
    defaultValue: Float,
    valueLabel: @Composable (Float) -> String,
    onValueChangeFinished: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    testTag: String,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var currentValue by remember(value) { mutableFloatStateOf(value) }

    ResonoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp)) {
            ResonoteBottomSheetHeader(
                title = title,
                subtitle = subtitle,
                actions = {
                    RestoreDefaultAction {
                        currentValue = defaultValue
                        onReset()
                    }
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                RotarySettingKnob(
                    title = knobTitle,
                    valueLabel = valueLabel(currentValue),
                    value = currentValue,
                    onValueChange = { currentValue = it },
                    onValueChangeFinished = onValueChangeFinished,
                    valueRange = valueRange,
                    steps = steps,
                    knobSize = 136.dp,
                    modifier = Modifier.testTag(testTag),
                )
            }
        }
    }
}

@Composable
internal fun DesktopLyricsShadowSheet(
    colorArgb: Int,
    offsetXDp: Float,
    offsetYDp: Float,
    blurRadiusDp: Float,
    onColorClick: () -> Unit,
    onOffsetXChange: (Float) -> Unit,
    onOffsetYChange: (Float) -> Unit,
    onBlurRadiusChange: (Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var horizontal by remember(offsetXDp) { mutableFloatStateOf(offsetXDp) }
    var vertical by remember(offsetYDp) { mutableFloatStateOf(offsetYDp) }
    var softness by remember(blurRadiusDp) { mutableFloatStateOf(blurRadiusDp) }

    ResonoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .testTag("desktop-lyrics-shadow-sheet"),
        ) {
            ResonoteBottomSheetHeader(
                title = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow),
                subtitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_sheet_body),
                actions = {
                    RestoreDefaultAction {
                        horizontal = DesktopLyricsDefaults.SHADOW_OFFSET_X_DP
                        vertical = DesktopLyricsDefaults.SHADOW_OFFSET_Y_DP
                        softness = DesktopLyricsDefaults.SHADOW_BLUR_RADIUS_DP
                        onReset()
                    }
                },
            )
            SettingsColorRow(
                title = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_color),
                color = Color(colorArgb),
                onClick = onColorClick,
                modifier = Modifier.testTag("desktop-lyrics-shadow-color-picker"),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                RotarySettingKnob(
                    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_x),
                    valueLabel = desktopLyricsDecimalLabel(horizontal),
                    value = horizontal,
                    onValueChange = { horizontal = it },
                    onValueChangeFinished = onOffsetXChange,
                    valueRange = -8f..8f,
                    steps = 159,
                    modifier = Modifier.weight(1f).testTag("desktop-lyrics-shadow-x-knob"),
                )
                RotarySettingKnob(
                    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_y),
                    valueLabel = desktopLyricsDecimalLabel(vertical),
                    value = vertical,
                    onValueChange = { vertical = it },
                    onValueChangeFinished = onOffsetYChange,
                    valueRange = -8f..8f,
                    steps = 159,
                    modifier = Modifier.weight(1f).testTag("desktop-lyrics-shadow-y-knob"),
                )
                RotarySettingKnob(
                    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_z),
                    valueLabel = desktopLyricsDecimalLabel(softness),
                    value = softness,
                    onValueChange = { softness = it },
                    onValueChangeFinished = onBlurRadiusChange,
                    valueRange = 0f..12f,
                    steps = 119,
                    modifier = Modifier.weight(1f).testTag("desktop-lyrics-shadow-softness-knob"),
                )
            }
        }
    }
}

@Composable
private fun RotarySettingKnob(
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    knobSize: Dp = 104.dp,
    modifier: Modifier = Modifier,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant
    val centerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val indicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
    val coercedValue = value.coerceIn(valueRange)
    val span = valueRange.endInclusive - valueRange.start
    val fraction = (coercedValue - valueRange.start) / span

    fun snap(target: Float): Float {
        val intervals = steps + 1
        val targetFraction = ((target - valueRange.start) / span).coerceIn(0f, 1f)
        return valueRange.start + (targetFraction * intervals).roundToInt() * span / intervals
    }

    Column(
        modifier = modifier.semantics {
            contentDescription = title
            stateDescription = valueLabel
            progressBarRangeInfo = ProgressBarRangeInfo(coercedValue, valueRange, steps)
            setProgress { target ->
                val snapped = snap(target)
                onValueChange(snapped)
                onValueChangeFinished(snapped)
                true
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Canvas(
            Modifier.size(knobSize)
                .pointerInput(valueRange, steps) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        var latestValue = coercedValue

                        fun update(position: Offset) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            var angle = atan2(position.y - center.y, position.x - center.x) * 180f / PI.toFloat()
                            if (angle < 0f) angle += 360f
                            if (angle < KNOB_START_ANGLE) angle += 360f
                            val targetFraction = ((angle - KNOB_START_ANGLE) / KNOB_SWEEP_ANGLE).coerceIn(0f, 1f)
                            latestValue = snap(valueRange.start + span * targetFraction)
                            onValueChange(latestValue)
                        }

                        try {
                            update(down.position)
                            drag(down.id) { change ->
                                change.consume()
                                update(change.position)
                            }
                        } finally {
                            onValueChangeFinished(latestValue)
                        }
                    }
                },
        ) {
            val strokeWidth = 7.dp.toPx()
            val radius = size.minDimension / 2f - 14.dp.toPx()
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
            drawArc(
                color = inactiveColor,
                startAngle = KNOB_START_ANGLE,
                sweepAngle = KNOB_SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = activeColor,
                startAngle = KNOB_START_ANGLE,
                sweepAngle = KNOB_SWEEP_ANGLE * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
            drawCircle(centerColor, radius = radius - 9.dp.toPx())
            val indicatorAngle = (KNOB_START_ANGLE + KNOB_SWEEP_ANGLE * fraction) * PI.toFloat() / 180f
            val indicatorStart = radius * 0.36f
            val indicatorEnd = radius * 0.66f
            drawLine(
                color = indicatorColor,
                start = center + Offset(cos(indicatorAngle) * indicatorStart, sin(indicatorAngle) * indicatorStart),
                end = center + Offset(cos(indicatorAngle) * indicatorEnd, sin(indicatorAngle) * indicatorEnd),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        Text(
            text = valueLabel,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun desktopLyricsWidthLabel(value: Int): String =
    stringResource(R.string.feature_settings_impl_desktop_lyrics_integer_value, value)

@Composable
internal fun desktopLyricsFontSizeLabel(value: Int): String =
    stringResource(R.string.feature_settings_impl_desktop_lyrics_integer_value, value)

@Composable
internal fun desktopLyricsOutlineLabel(value: Float): String = desktopLyricsDecimalLabel(value)

@Composable
internal fun desktopLyricsShadowLabel(offsetX: Float, offsetY: Float, blur: Float): String = stringResource(
    R.string.feature_settings_impl_desktop_lyrics_shadow_summary,
    desktopLyricsDecimalLabel(offsetX),
    desktopLyricsDecimalLabel(offsetY),
    desktopLyricsDecimalLabel(blur),
)

@Composable
private fun desktopLyricsDecimalLabel(value: Float): String = stringResource(
    R.string.feature_settings_impl_desktop_lyrics_decimal_value,
    if (abs(value) < 0.05f) 0f else value,
)

private const val KNOB_START_ANGLE = 135f
private const val KNOB_SWEEP_ANGLE = 270f
