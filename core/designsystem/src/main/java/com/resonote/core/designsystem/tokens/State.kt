package com.resonote.core.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ResonoteStateLayers internal constructor(
    val hoverOpacity: Float = 0.08f,
    val focusOpacity: Float = 0.10f,
    val pressedOpacity: Float = 0.10f,
    val draggedOpacity: Float = 0.16f,
    val disabledContentOpacity: Float = 0.38f,
    val disabledContainerOpacity: Float = 0.12f,
    val iconLayerSize: Dp = 40.dp,
    val touchTarget: Dp = 48.dp,
)
