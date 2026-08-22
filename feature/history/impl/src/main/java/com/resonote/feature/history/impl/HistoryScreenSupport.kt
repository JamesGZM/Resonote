@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.history.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.ContentFailure

@Composable
internal fun SignedOutState(onLoginRequest: () -> Unit) {
    MessageState(
        icon = { Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(42.dp)) },
        title = stringResource(R.string.feature_history_impl_online_login_title),
        body = stringResource(R.string.feature_history_impl_online_login_body),
        action = {
            ResonoteButton(
                label = stringResource(R.string.feature_history_impl_login),
                onClick = onLoginRequest,
            )
        },
    )
}

@Composable
internal fun OnlineFailureState(failure: ContentFailure, onRetry: () -> Unit) {
    MessageState(
        icon = { Icon(Icons.Rounded.CloudOff, contentDescription = null, modifier = Modifier.size(42.dp)) },
        title = stringResource(R.string.feature_history_impl_online_error_title),
        body = failure.message(),
        action = {
            ResonoteButton(
                label = stringResource(R.string.feature_history_impl_retry),
                onClick = onRetry,
            )
        },
    )
}

@Composable
internal fun DeviceFailureState() {
    MessageState(
        icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = null, modifier = Modifier.size(42.dp)) },
        title = stringResource(R.string.feature_history_impl_device_error_title),
        body = stringResource(R.string.feature_history_impl_device_error_body),
    )
}

@Composable
internal fun EmptyState(title: String, body: String) {
    MessageState(
        icon = { Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(42.dp)) },
        title = title,
        body = body,
    )
}

@Composable
internal fun MessageState(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary,
        ) { Box(contentAlignment = Alignment.Center) { icon() } }
        Text(
            title,
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            body,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        action?.let { Box(modifier = Modifier.padding(top = 22.dp)) { it() } }
    }
}

@Composable
internal fun LoadingState(label: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 58.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            stringResource(label),
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun MutationFailureCard(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("history-mutation-failure"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.feature_history_impl_mutation_failed),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_history_impl_dismiss)) }
        }
    }
}

@Composable
internal fun ContentFailure.message(): String = stringResource(
    when (this) {
        ContentFailure.AuthenticationRequired -> R.string.feature_history_impl_error_auth
        ContentFailure.Network -> R.string.feature_history_impl_error_network
        ContentFailure.RiskBlocked,
        is ContentFailure.RiskVerificationRequired,
        -> R.string.feature_history_impl_error_risk
        ContentFailure.ServiceRejected, ContentFailure.Protocol -> R.string.feature_history_impl_error_generic
    },
)

internal fun AudioQuality.label(): String = when (this) {
    AudioQuality.Standard -> "SQ"
    AudioQuality.HighQuality -> "HQ"
    AudioQuality.HighResolution -> "Hi-Res"
    AudioQuality.Lossless -> "LOSSLESS"
}

internal fun Long.durationLabel(): String {
    if (this <= 0) return "—:—"
    val totalSeconds = this / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
