package com.resonote.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.tokens.ResonoteTokens

@Composable
fun ResonoteButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingLabel: String = label,
    leadingIcon: (@Composable () -> Unit)? = null,
) = ResonoteButtonImpl(
    style = ResonoteButtonStyle.FILLED,
    label = label,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    loadingLabel = loadingLabel,
    leadingIcon = leadingIcon,
)

@Composable
fun ResonoteTonalButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingLabel: String = label,
    leadingIcon: (@Composable () -> Unit)? = null,
) = ResonoteButtonImpl(
    style = ResonoteButtonStyle.TONAL,
    label = label,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    loadingLabel = loadingLabel,
    leadingIcon = leadingIcon,
)

@Composable
fun ResonoteOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingLabel: String = label,
    leadingIcon: (@Composable () -> Unit)? = null,
) = ResonoteButtonImpl(
    style = ResonoteButtonStyle.OUTLINED,
    label = label,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    loadingLabel = loadingLabel,
    leadingIcon = leadingIcon,
)

@Composable
fun ResonoteTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingLabel: String = label,
    leadingIcon: (@Composable () -> Unit)? = null,
) = ResonoteButtonImpl(
    style = ResonoteButtonStyle.TEXT,
    label = label,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    loadingLabel = loadingLabel,
    leadingIcon = leadingIcon,
)

@Composable
fun ResonoteDestructiveButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingLabel: String = label,
    leadingIcon: (@Composable () -> Unit)? = null,
) = ResonoteButtonImpl(
    style = ResonoteButtonStyle.DESTRUCTIVE,
    label = label,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    loadingLabel = loadingLabel,
    leadingIcon = leadingIcon,
)

@Composable
fun ResonoteDestructiveTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingLabel: String = label,
    leadingIcon: (@Composable () -> Unit)? = null,
) = ResonoteButtonImpl(
    style = ResonoteButtonStyle.DESTRUCTIVE_TEXT,
    label = label,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    loadingLabel = loadingLabel,
    leadingIcon = leadingIcon,
)

private enum class ResonoteButtonStyle {
    FILLED,
    TONAL,
    OUTLINED,
    TEXT,
    DESTRUCTIVE,
    DESTRUCTIVE_TEXT,
}

@Composable
private fun ResonoteButtonImpl(
    style: ResonoteButtonStyle,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    loading: Boolean,
    loadingLabel: String,
    leadingIcon: (@Composable () -> Unit)?,
) {
    val contentPadding = if (leadingIcon == null) {
        PaddingValues(horizontal = 24.dp)
    } else {
        PaddingValues(start = 16.dp, end = 24.dp)
    }
    val buttonModifier = modifier.then(
        if (loading) Modifier.semantics { stateDescription = loadingLabel } else Modifier,
    )
    val content: @Composable RowScope.() -> Unit = {
        ResonoteButtonContent(
            label = label,
            loading = loading,
            loadingLabel = loadingLabel,
            leadingIcon = leadingIcon,
        )
    }

    when (style) {
        ResonoteButtonStyle.FILLED -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = ResonoteTokens.shapes.full,
            contentPadding = contentPadding,
            content = content,
        )

        ResonoteButtonStyle.TONAL -> FilledTonalButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = ResonoteTokens.shapes.full,
            contentPadding = contentPadding,
            content = content,
        )

        ResonoteButtonStyle.OUTLINED -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = ResonoteTokens.shapes.full,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            border = BorderStroke(
                width = ResonoteTokens.borders.hairline,
                color = if (enabled && !loading) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = ResonoteTokens.stateLayers.disabledContainerOpacity,
                    )
                },
            ),
            contentPadding = contentPadding,
            content = content,
        )

        ResonoteButtonStyle.TEXT -> TextButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = ResonoteTokens.shapes.full,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            contentPadding = contentPadding,
            content = content,
        )

        ResonoteButtonStyle.DESTRUCTIVE -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = ResonoteTokens.shapes.full,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            contentPadding = contentPadding,
            content = content,
        )

        ResonoteButtonStyle.DESTRUCTIVE_TEXT -> TextButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = ResonoteTokens.shapes.full,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            contentPadding = contentPadding,
            content = content,
        )
    }
}

@Composable
private fun ResonoteButtonContent(
    label: String,
    loading: Boolean,
    loadingLabel: String,
    leadingIcon: (@Composable () -> Unit)?,
) {
    Box(contentAlignment = Alignment.Center) {
        ButtonContentRow(
            label = label,
            visible = !loading,
            leading = leadingIcon,
        )
        ButtonContentRow(
            label = loadingLabel,
            visible = loading,
            leading = {
                CircularProgressIndicator(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp,
                )
            },
        )
    }
}

@Composable
private fun ButtonContentRow(
    label: String,
    visible: Boolean,
    leading: (@Composable () -> Unit)?,
) {
    Row(
        modifier = Modifier
            .alpha(if (visible) 1f else 0f)
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics {}),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(
                modifier = Modifier.size(ButtonDefaults.IconSize),
                contentAlignment = Alignment.Center,
            ) {
                leading()
            }
            Spacer(Modifier.width(ResonoteTokens.spacing.space2))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
