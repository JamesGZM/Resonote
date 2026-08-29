package com.resonote.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ResonoteRotaryKnob(
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean = true,
    knobSize: Dp = 104.dp,
    modifier: Modifier = Modifier,
) {
    val activeColor = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.38f)
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (enabled) 1f else 0.45f)
    val centerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (enabled) 1f else 0.55f)
    val indicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.45f)
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
            if (!enabled) disabled()
            setProgress { target ->
                if (!enabled) return@setProgress false
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
                .pointerInput(enabled, valueRange, steps) {
                    if (!enabled) return@pointerInput
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
            color = activeColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

private const val KNOB_START_ANGLE = 135f
private const val KNOB_SWEEP_ANGLE = 270f
