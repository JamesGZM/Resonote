package com.resonote.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

@Stable
class ResonoteShimmer internal constructor(
    internal val offset: State<Float>,
    internal val base: Color,
    internal val highlight: Color,
)

@Composable
fun rememberResonoteShimmer(label: String = "resonote-skeleton"): ResonoteShimmer {
    val transition = rememberInfiniteTransition(label = label)
    val offset = transition.animateFloat(
        initialValue = -300f,
        targetValue = 1_200f,
        animationSpec = infiniteRepeatable(animation = tween(1_200), repeatMode = RepeatMode.Restart),
        label = "$label-offset",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    return remember(offset, base, highlight) { ResonoteShimmer(offset, base, highlight) }
}

fun Modifier.resonoteShimmer(shimmer: ResonoteShimmer, shape: Shape): Modifier = clip(shape).drawBehind {
    val offset = shimmer.offset.value
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(shimmer.base, shimmer.highlight, shimmer.base),
            start = Offset(offset - 300f, 0f),
            end = Offset(offset, 300f),
        ),
    )
}
