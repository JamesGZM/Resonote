package com.resonote.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.resonote.core.designsystem.tokens.ResonoteTokens

/**
 * A containerless action whose feedback stays on its content instead of drawing a rectangular
 * state layer. Use a shaped clickable Surface when the action already has a visible container.
 */
@Composable
fun ResonotePlainAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    shape: Shape = MaterialTheme.shapes.small,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by resolvedInteractionSource.collectIsPressedAsState()
    val isFocused by resolvedInteractionSource.collectIsFocusedAsState()
    val contentAlpha by animateFloatAsState(
        targetValue = when {
            !enabled -> ResonoteTokens.stateLayers.disabledContentOpacity
            isPressed -> ResonoteTokens.stateLayers.pressedContentOpacity
            else -> 1f
        },
        animationSpec = ResonoteTokens.motion.effectsFast(),
        label = "plainActionContentAlpha",
    )
    val focusModifier = if (isFocused) {
        Modifier.border(
            width = ResonoteTokens.borders.focusIndicatorWidth,
            color = MaterialTheme.colorScheme.primary,
            shape = shape,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .sizeIn(
                minWidth = ResonoteTokens.touchTargets.minimum,
                minHeight = ResonoteTokens.touchTargets.minimum,
            )
            .then(focusModifier)
            .clickable(
                interactionSource = resolvedInteractionSource,
                indication = null,
                enabled = enabled,
                role = role,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.graphicsLayer { alpha = contentAlpha },
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
