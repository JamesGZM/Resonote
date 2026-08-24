@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.recognition.impl

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

@Composable
internal fun IdleContent(onStart: () -> Unit) {
    RecognitionAction(
        label = stringResource(R.string.feature_recognition_impl_start),
        onClick = onStart,
        icon = Icons.Rounded.Mic,
    )
}

@Composable
internal fun PermissionContent(permanently: Boolean, onStart: () -> Unit, onOpenSettings: () -> Unit) {
    RecognitionAction(
        label = stringResource(
            if (permanently) {
                R.string.feature_recognition_impl_open_settings
            } else {
                R.string.feature_recognition_impl_request_permission
            },
        ),
        onClick = if (permanently) onOpenSettings else onStart,
        icon = if (permanently) Icons.Rounded.Settings else Icons.Rounded.Mic,
    )
}

@Composable
internal fun RecordingContent(onStop: () -> Unit) {
    RecognitionAction(
        label = stringResource(R.string.feature_recognition_impl_stop),
        onClick = onStop,
        icon = Icons.Rounded.Pause,
    )
}

@Composable
internal fun RecognizingContent() {
    LinearProgressIndicator(
        modifier = Modifier.width(180.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f),
    )
}

@Composable
internal fun RecognitionRecordBackground(
    amplitude: Float,
    active: Boolean,
    showListeningField: Boolean = true,
    animate: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val latestAmplitude by rememberUpdatedState(amplitude)
    val simulation = remember { RecognitionRippleSimulation() }
    val rippleFrame = remember { mutableStateOf(RecognitionRippleFrame()) }

    LaunchedEffect(active, animate) {
        simulation.reset()
        rippleFrame.value = RecognitionRippleFrame()
        if (!active) return@LaunchedEffect

        if (!animate) {
            repeat(54) { frameIndex ->
                val previewEnvelope = 0.38f + abs(sin(frameIndex * 0.53f)) * 0.62f
                rippleFrame.value = simulation.step(
                    deltaSeconds = 1f / 60f,
                    inputAmplitude = latestAmplitude * previewEnvelope,
                )
            }
            return@LaunchedEffect
        }

        var previousFrameNanos = withFrameNanos { it }
        while (currentCoroutineContext().isActive) {
            withFrameNanos { frameNanos ->
                val deltaSeconds = (frameNanos - previousFrameNanos) / 1_000_000_000f
                rippleFrame.value = simulation.step(
                    deltaSeconds = deltaSeconds,
                    inputAmplitude = latestAmplitude,
                )
                previousFrameNanos = frameNanos
            }
        }
    }

    Canvas(modifier.testTag("recognition-ripples")) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(primary, primary.copy(alpha = 0.9f), primaryContainer),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiaryContainer.copy(alpha = 0.72f), Color.Transparent),
                center = Offset(size.width * 0.88f, size.height * 0.82f),
                radius = size.width * 0.9f,
            ),
            radius = size.width,
            center = Offset(size.width * 0.88f, size.height * 0.82f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryContainer.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(size.width * 0.88f, size.height * 0.1f),
                radius = size.width * 0.72f,
            ),
            radius = size.width * 0.72f,
            center = Offset(size.width * 0.88f, size.height * 0.1f),
        )

        if (!showListeningField) return@Canvas

        val frame = rippleFrame.value
        val center = Offset(size.width * 0.5f, size.height * 0.475f)
        val maximumRadius = min(size.width * 0.44f, size.height * 0.22f)
        val visualEnergy = frame.envelope.coerceIn(0f, 1f)
        val driverDisplacement = frame.driverDisplacement.coerceIn(-0.16f, 0.28f)
        val driverMotion = frame.driverMotion.coerceIn(0f, 1f)
        val baseOrbRadius = maximumRadius * 0.4f
        val orbRadius = (
            baseOrbRadius * (1f + visualEnergy * 0.025f + driverDisplacement * 0.48f)
            ).coerceIn(baseOrbRadius * 0.92f, baseOrbRadius * 1.15f)
        val innerWaveRadius = orbRadius * 1.06f
        val outerWaveRadius = maximumRadius * 1.02f

        repeat(7) { index ->
            val fraction = index / 6f
            drawCircle(
                color = onPrimary.copy(
                    alpha = 0.035f + fraction * 0.055f + visualEnergy * 0.025f,
                ),
                radius = maximumRadius * (0.52f + fraction * 0.46f),
                center = center,
                style = Stroke(width = (0.75f + fraction * 0.22f).dp.toPx()),
            )
        }

        frame.pulses.forEach { pulse ->
            val progress =
                (pulse.ageSeconds / RecognitionRippleSimulation.RIPPLE_LIFETIME_SECONDS).coerceIn(0f, 1f)
            val travel = 1f - (1f - progress).pow(1.18f)
            val radius = innerWaveRadius + (outerWaveRadius - innerWaveRadius) * travel
            val crest = sin(PI.toFloat() * progress).coerceAtLeast(0f).pow(0.72f)
            val fade = crest * (1f - progress * 0.48f)
            val alpha = (pulse.energy * fade).coerceIn(0f, 1f)
            if (alpha <= 0.002f) return@forEach

            val glowWidth = (4.5f + pulse.energy * 7f - progress * 2f).coerceAtLeast(2f).dp.toPx()
            drawCircle(
                color = tertiaryContainer.copy(alpha = alpha * 0.1f),
                radius = radius,
                center = center,
                style = Stroke(width = glowWidth),
            )
            drawCircle(
                color = onPrimary.copy(alpha = alpha * 0.82f),
                radius = radius,
                center = center,
                style = Stroke(
                    width = (1f + pulse.energy * 1.25f * (1f - progress * 0.35f)).dp.toPx(),
                    cap = StrokeCap.Round,
                ),
            )
            val echoRadius = (radius - maximumRadius * (0.02f + pulse.energy * 0.018f))
                .coerceAtLeast(innerWaveRadius)
            drawCircle(
                color = primaryContainer.copy(alpha = alpha * 0.26f),
                radius = echoRadius,
                center = center,
                style = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        val haloRadius = orbRadius * (1.52f + visualEnergy * 0.16f + driverMotion * 0.08f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    tertiaryContainer.copy(
                        alpha = 0.1f + visualEnergy * 0.22f + driverMotion * 0.12f,
                    ),
                    Color.Transparent,
                ),
                center = center + Offset(orbRadius * 0.24f, orbRadius * 0.28f),
                radius = haloRadius,
            ),
            radius = haloRadius,
            center = center,
        )
        if (driverMotion > 0.002f) {
            drawCircle(
                color = onPrimary.copy(alpha = driverMotion * 0.22f),
                radius = orbRadius * (1.07f + driverMotion * 0.06f),
                center = center,
                style = Stroke(
                    width = (0.8f + driverMotion * 1.4f).dp.toPx(),
                    cap = StrokeCap.Round,
                ),
            )
        }
        if (visualEnergy > 0f || driverMotion > 0f) {
            drawCircle(
                color = primaryContainer.copy(
                    alpha = 0.06f + visualEnergy * 0.09f + driverMotion * 0.12f,
                ),
                radius = orbRadius * (1.06f + driverMotion * 0.08f),
                center = center,
                style = Stroke(width = (0.8f + visualEnergy * 0.6f + driverMotion).dp.toPx()),
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    tertiaryContainer.copy(alpha = 0.92f),
                    primaryContainer.copy(alpha = 0.78f),
                    primary.copy(alpha = 0.24f),
                ),
                center = center + Offset(orbRadius * 0.34f, orbRadius * 0.34f),
                radius = orbRadius * 1.35f,
            ),
            radius = orbRadius,
            center = center,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    onPrimary.copy(alpha = 0.1f + driverMotion * 0.08f),
                    Color.Transparent,
                ),
                center = center - Offset(orbRadius * 0.2f, orbRadius * 0.22f),
                radius = orbRadius * 0.82f,
            ),
            radius = orbRadius * 0.82f,
            center = center,
        )
        drawCircle(
            color = onPrimary.copy(alpha = 0.12f + driverMotion * 0.1f),
            radius = orbRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

@Composable
internal fun RecognitionAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(56.dp).testTag("recognition-action"),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
        Text(
            label,
            modifier = Modifier.padding(top = 10.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
