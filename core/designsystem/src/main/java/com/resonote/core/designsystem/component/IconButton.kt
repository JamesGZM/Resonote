package com.resonote.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.tokens.ResonoteTokens

@Composable
fun ResonoteIconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) = ResonoteIconButtonImpl(
    style = ResonoteIconButtonStyle.STANDARD,
    label = label,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    icon = icon,
)

@Composable
fun ResonoteFilledIconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    icon: @Composable () -> Unit,
) = ResonoteIconButtonImpl(
    style = ResonoteIconButtonStyle.FILLED,
    label = label,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    colors = colors,
    icon = icon,
)

@Composable
fun ResonoteCompactFilledIconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    icon: @Composable () -> Unit,
) {
    ResonoteIconButtonTooltip(label = label, modifier = modifier) {
        val containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
        val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(ResonoteTokens.touchTargets.minimum)
                .semantics { contentDescription = label },
            enabled = enabled,
            shape = ResonoteTokens.shapes.full,
            color = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = contentColor,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = CompactIconButtonEdgeInset, bottom = CompactIconButtonEdgeInset),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Surface(
                    modifier = Modifier.size(CompactIconButtonContainerSize),
                    shape = ResonoteTokens.shapes.full,
                    color = containerColor,
                    contentColor = contentColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier.size(CompactIconButtonGlyphSize),
                            contentAlignment = Alignment.Center,
                            content = { icon() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResonoteTonalIconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) = ResonoteIconButtonImpl(
    style = ResonoteIconButtonStyle.TONAL,
    label = label,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    icon = icon,
)

@Composable
fun ResonoteOutlinedIconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) = ResonoteIconButtonImpl(
    style = ResonoteIconButtonStyle.OUTLINED,
    label = label,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    icon = icon,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResonoteIconToggleButton(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
    checkedIcon: @Composable () -> Unit,
) {
    ResonoteIconButtonTooltip(label = label, modifier = modifier) {
        IconToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = label },
            enabled = enabled,
            shape = ResonoteTokens.shapes.full,
            colors = IconButtonDefaults.iconToggleButtonColors(
                checkedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                checkedContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            ResonoteIconContent(if (checked) checkedIcon else icon)
        }
    }
}

private enum class ResonoteIconButtonStyle {
    STANDARD,
    FILLED,
    TONAL,
    OUTLINED,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResonoteIconButtonImpl(
    style: ResonoteIconButtonStyle,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    colors: IconButtonColors? = null,
    icon: @Composable () -> Unit,
) {
    ResonoteIconButtonTooltip(label = label, modifier = modifier) {
        val buttonModifier = Modifier.semantics { contentDescription = label }
        when (style) {
            ResonoteIconButtonStyle.STANDARD -> IconButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = ResonoteTokens.shapes.full,
            ) { ResonoteIconContent(icon) }

            ResonoteIconButtonStyle.FILLED -> FilledIconButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = ResonoteTokens.shapes.full,
                colors = colors ?: IconButtonDefaults.filledIconButtonColors(),
            ) { ResonoteIconContent(icon) }

            ResonoteIconButtonStyle.TONAL -> FilledTonalIconButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = ResonoteTokens.shapes.full,
            ) { ResonoteIconContent(icon) }

            ResonoteIconButtonStyle.OUTLINED -> OutlinedIconButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = ResonoteTokens.shapes.full,
            ) { ResonoteIconContent(icon) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResonoteIconButtonTooltip(label: String, modifier: Modifier, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Above,
        ),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        modifier = modifier,
        content = content,
    )
}

@Composable
private fun ResonoteIconContent(icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(ResonoteTokens.icons.default),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

private val CompactIconButtonContainerSize = 28.dp
private val CompactIconButtonGlyphSize = 16.dp
private val CompactIconButtonEdgeInset = 8.dp
