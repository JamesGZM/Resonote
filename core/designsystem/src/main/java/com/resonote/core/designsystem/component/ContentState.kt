package com.resonote.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.R
import com.resonote.core.designsystem.tokens.ResonoteTokens

@Immutable
enum class ResonoteContentPhase { LOADING, EMPTY, ERROR, CONTENT }

@Composable
fun ResonoteContentStateLayout(
    phase: ResonoteContentPhase,
    modifier: Modifier = Modifier,
    loading: @Composable () -> Unit = { ResonoteLoadingState() },
    empty: @Composable () -> Unit = { ResonoteEmptyState() },
    error: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        when (phase) {
            ResonoteContentPhase.LOADING -> loading()
            ResonoteContentPhase.EMPTY -> empty()
            ResonoteContentPhase.ERROR -> error()
            ResonoteContentPhase.CONTENT -> content()
        }
    }
}

@Composable
fun ResonoteLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().testTag("resonote-loading-state"),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ResonoteEmptyState(
    message: String = stringResource(R.string.core_designsystem_empty_message),
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String = stringResource(R.string.core_designsystem_empty_title),
    action: (@Composable () -> Unit)? = null,
    illustration: (@Composable () -> Unit)? = null,
) {
    ResonoteMessageState(
        visual = illustration ?: {
            ResonoteStateIllustration(
                drawableRes = R.drawable.resonote_empty_illustration,
                icon = icon,
            )
        },
        message = message,
        modifier = modifier.testTag("resonote-empty-state"),
        title = title,
        action = action,
    )
}

@Composable
fun ResonoteErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String = stringResource(R.string.core_designsystem_error_title),
    message: String = stringResource(R.string.core_designsystem_error_message),
    retryLabel: String = stringResource(R.string.core_designsystem_retry),
    action: (@Composable () -> Unit)? = null,
    illustration: (@Composable () -> Unit)? = null,
) {
    ResonoteMessageState(
        visual = illustration ?: {
            ResonoteStateIllustration(
                drawableRes = R.drawable.resonote_error_illustration,
                icon = icon,
            )
        },
        title = title,
        message = message,
        modifier = modifier.testTag("resonote-error-state"),
        action = action ?: {
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = ResonoteTokens.shapes.full,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                border = BorderStroke(
                    width = ResonoteTokens.borders.hairline,
                    color = MaterialTheme.colorScheme.primary,
                ),
                contentPadding = PaddingValues(horizontal = 24.dp),
            ) {
                Text(
                    text = retryLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}

@Composable
private fun ResonoteMessageState(
    visual: @Composable () -> Unit,
    message: String,
    modifier: Modifier,
    title: String,
    action: (@Composable () -> Unit)?,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp).heightIn(min = 188.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            visual()
            Spacer(Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (message.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            if (action != null) {
                Spacer(Modifier.height(16.dp))
                action()
            }
        }
    }
}

@Composable
private fun ResonoteStateIllustration(drawableRes: Int, icon: ImageVector?) {
    if (icon == null) {
        Icon(
            painter = painterResource(drawableRes),
            contentDescription = null,
            modifier = Modifier.width(128.dp).height(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    } else {
        Box(
            modifier = Modifier.width(128.dp).height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
