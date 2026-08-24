package com.resonote.feature.library.impl

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

internal fun Modifier.bleedHorizontally(horizontalPadding: Dp): Modifier = layout { measurable, constraints ->
    val horizontalPaddingPx = horizontalPadding.roundToPx()
    val expandedWidth = constraints.maxWidth + horizontalPaddingPx * 2
    val placeable = measurable.measure(
        constraints.copy(minWidth = expandedWidth, maxWidth = expandedWidth),
    )

    layout(constraints.maxWidth, placeable.height) {
        placeable.placeRelative(-horizontalPaddingPx, 0)
    }
}
