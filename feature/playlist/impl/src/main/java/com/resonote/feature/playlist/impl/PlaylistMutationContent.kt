@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.playlist.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteDestructiveButton
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong

@Composable
internal fun RemoveSongDialog(
    song: OnlineSong,
    removing: Boolean,
    failure: ContentFailure?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
        title = { Text(stringResource(R.string.feature_playlist_impl_remove_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.feature_playlist_impl_remove_body, song.title))
                failure?.let {
                    Text(
                        text = removalFailureMessage(it),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !removing) {
                Text(stringResource(R.string.feature_playlist_impl_remove_cancel))
            }
        },
        confirmButton = {
            ResonoteDestructiveButton(
                label = stringResource(R.string.feature_playlist_impl_remove_confirm),
                loadingLabel = stringResource(R.string.feature_playlist_impl_removing),
                loading = removing,
                enabled = !removing,
                onClick = onConfirm,
            )
        },
    )
}

@Composable
internal fun removalFailureMessage(failure: ContentFailure): String = when (failure) {
    ContentFailure.AuthenticationRequired -> stringResource(R.string.feature_playlist_impl_remove_error_auth)
    ContentFailure.Network -> stringResource(R.string.feature_playlist_impl_remove_error_network)
    ContentFailure.ServiceRejected -> stringResource(R.string.feature_playlist_impl_remove_error_service)
    is ContentFailure.RiskVerificationRequired,
    ContentFailure.RiskBlocked,
    -> stringResource(R.string.feature_playlist_impl_remove_error_risk)
    ContentFailure.Protocol -> stringResource(R.string.feature_playlist_impl_remove_error_protocol)
}
