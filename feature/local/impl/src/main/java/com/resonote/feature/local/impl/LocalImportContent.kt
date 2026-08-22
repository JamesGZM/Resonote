@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.local.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteOutlinedButton

@Composable
internal fun ImportProgressCard(state: LocalImportUiState.Running, onCancel: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth().testTag("local-import-progress"),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                stringResource(R.string.feature_local_impl_importing),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(
                    R.string.feature_local_impl_import_progress,
                    state.completed,
                    state.total,
                    state.imported,
                    state.failed,
                ),
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            LinearProgressIndicator(
                progress = { if (state.total == 0) 0f else state.completed.toFloat() / state.total },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(6.dp).clip(CircleShape),
            )
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End).padding(top = 6.dp)) {
                Text(stringResource(R.string.feature_local_impl_cancel_remaining))
            }
        }
    }
}

@Composable
internal fun ImportResultCard(state: LocalImportUiState.Completed, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth().testTag("local-import-result"),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    stringResource(R.string.feature_local_impl_import_complete),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(
                        R.string.feature_local_impl_import_result,
                        state.imported,
                        state.skipped,
                        state.failures.size,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (state.failures.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 5.dp)) {
                        for ((failure, count) in state.failures.groupingBy { it }.eachCount()) {
                            Text(
                                "${failure.label()} ×$count",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, stringResource(R.string.feature_local_impl_dismiss))
            }
        }
    }
}

@Composable
internal fun DuplicateDialog(
    duplicate: LocalImportUiState.AwaitingDuplicate,
    onCancel: () -> Unit,
    onImportCopy: () -> Unit,
) {
    val existing = duplicate.existing.first()
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Rounded.AudioFile, contentDescription = null) },
        title = { Text(stringResource(R.string.feature_local_impl_duplicate_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.feature_local_impl_duplicate_body, duplicate.candidate.title))
                Text(
                    stringResource(
                        R.string.feature_local_impl_duplicate_existing,
                        existing.title,
                        existing.artist ?: stringResource(R.string.feature_local_impl_unknown_artist),
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.feature_local_impl_skip)) }
        },
        confirmButton = {
            TextButton(onClick = onImportCopy) {
                Text(stringResource(R.string.feature_local_impl_import_copy))
            }
        },
    )
}

@Composable
internal fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            stringResource(R.string.feature_local_impl_loading),
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun EmptyState(onPickFiles: () -> Unit, onPickDirectory: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(34.dp))
            }
        }
        Text(
            stringResource(R.string.feature_local_impl_empty_title),
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.feature_local_impl_empty_body),
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        ResonoteButton(
            label = stringResource(R.string.feature_local_impl_choose_files),
            onClick = onPickFiles,
            modifier = Modifier.padding(top = 22.dp),
            leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
        )
        ResonoteOutlinedButton(
            label = stringResource(R.string.feature_local_impl_choose_directory),
            onClick = onPickDirectory,
            modifier = Modifier.padding(top = 10.dp),
            leadingIcon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null) },
        )
    }
}

@Composable
internal fun DirectoryScanCard(onCancel: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth().testTag("local-directory-scan"),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    stringResource(R.string.feature_local_impl_scanning_directory),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.feature_local_impl_scanning_directory_body),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(onClick = onCancel) { Text(stringResource(R.string.feature_local_impl_cancel)) }
        }
    }
}

@Composable
internal fun DirectoryFailureCard(reason: LocalDirectoryImportFailure, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().testTag("local-directory-error"),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.FolderOpen, contentDescription = null)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    stringResource(R.string.feature_local_impl_directory_failed),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    reason.label(),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, stringResource(R.string.feature_local_impl_dismiss))
            }
        }
    }
}
