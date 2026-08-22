@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.cloud.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
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
import com.resonote.core.model.ContentFailure

@Composable
internal fun PlaybackIssueCard(issue: CloudPlaybackIssue, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("cloud-playback-error"),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CloudOff, contentDescription = null)
                Text(
                    text = when (issue) {
                        CloudPlaybackIssue.Unavailable ->
                            stringResource(R.string.feature_cloud_impl_cloud_playback_unavailable)
                        is CloudPlaybackIssue.Failed -> issue.failure.playbackMessage()
                    },
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(modifier = Modifier.align(Alignment.End).padding(top = 6.dp)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_cloud_impl_cloud_dismiss)) }
                TextButton(onClick = onRetry) { Text(stringResource(R.string.feature_cloud_impl_cloud_playback_retry)) }
            }
        }
    }
}

@Composable
internal fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            stringResource(R.string.feature_cloud_impl_cloud_loading),
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun ErrorState(failure: ContentFailure, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 52.dp, horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, modifier = Modifier.size(42.dp))
        Text(
            stringResource(R.string.feature_cloud_impl_cloud_error_title),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            failure.message(),
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        ResonoteButton(
            label = stringResource(R.string.feature_cloud_impl_cloud_retry),
            onClick = onRetry,
            modifier = Modifier.padding(top = 22.dp),
        )
    }
}

@Composable
internal fun EmptyState(title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 50.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.Cloud, contentDescription = null, modifier = Modifier.size(40.dp))
        Text(
            title,
            modifier = Modifier.padding(top = 14.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            body,
            modifier = Modifier.padding(top = 7.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun LoadingLine(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        Text(label, modifier = Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun CloudSort.label(): String = stringResource(
    when (this) {
        CloudSort.UploadOrder -> R.string.feature_cloud_impl_cloud_sort_upload
        CloudSort.Title -> R.string.feature_cloud_impl_cloud_sort_title
        CloudSort.Artist -> R.string.feature_cloud_impl_cloud_sort_artist
        CloudSort.Duration -> R.string.feature_cloud_impl_cloud_sort_duration
    },
)

@Composable
internal fun ContentFailure.message(): String = stringResource(
    when (this) {
        ContentFailure.AuthenticationRequired -> R.string.feature_cloud_impl_cloud_error_auth
        ContentFailure.Network -> R.string.feature_cloud_impl_cloud_error_network
        ContentFailure.RiskBlocked,
        is ContentFailure.RiskVerificationRequired,
        -> R.string.feature_cloud_impl_cloud_error_risk
        ContentFailure.ServiceRejected, ContentFailure.Protocol -> R.string.feature_cloud_impl_cloud_error_generic
    },
)

@Composable
internal fun ContentFailure.playbackMessage(): String = when (this) {
    ContentFailure.AuthenticationRequired -> stringResource(R.string.feature_cloud_impl_cloud_error_auth)
    ContentFailure.Network -> stringResource(R.string.feature_cloud_impl_cloud_error_network)
    ContentFailure.RiskBlocked,
    is ContentFailure.RiskVerificationRequired,
    -> stringResource(R.string.feature_cloud_impl_cloud_error_risk)
    ContentFailure.ServiceRejected,
    ContentFailure.Protocol,
    -> stringResource(R.string.feature_cloud_impl_cloud_playback_failed)
}

internal fun Long.durationLabel(): String {
    if (this <= 0) return "—:—"
    val totalSeconds = this / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

internal fun Long.fileSize(): String {
    val safe = coerceAtLeast(0)
    if (safe < 1_024) return "$safe B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = safe.toDouble() / 1_024
    var index = 0
    while (value >= 1_024 && index < units.lastIndex) {
        value /= 1_024
        index++
    }
    return if (value >= 100) "%.0f %s".format(value, units[index]) else "%.1f %s".format(value, units[index])
}
