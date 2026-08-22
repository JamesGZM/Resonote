@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.local.impl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaImportFailure
import java.util.Locale

@Composable
internal fun NoResultsState(query: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(36.dp))
        Text(
            stringResource(R.string.feature_local_impl_no_results, query.trim()),
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun DeleteFailureCard(onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.feature_local_impl_delete_failed),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_local_impl_dismiss)) }
        }
    }
}

@Composable
internal fun LocalMusicSort.label(): String = stringResource(
    when (this) {
        LocalMusicSort.ImportedNewest -> R.string.feature_local_impl_sort_imported
        LocalMusicSort.Title -> R.string.feature_local_impl_sort_title
        LocalMusicSort.Artist -> R.string.feature_local_impl_sort_artist
        LocalMusicSort.Duration -> R.string.feature_local_impl_sort_duration
    },
)

@Composable
internal fun LocalDirectoryImportFailure.label(): String = stringResource(
    when (this) {
        LocalDirectoryImportFailure.NoFiles -> R.string.feature_local_impl_directory_empty
        LocalDirectoryImportFailure.InvalidTree -> R.string.feature_local_impl_directory_invalid
        LocalDirectoryImportFailure.PermissionDenied -> R.string.feature_local_impl_directory_permission
        LocalDirectoryImportFailure.Unavailable -> R.string.feature_local_impl_directory_unavailable
    },
)

internal fun LocalImportUiState.isBusy(): Boolean = this is LocalImportUiState.ScanningDirectory ||
    this is LocalImportUiState.Running ||
    this is LocalImportUiState.AwaitingDuplicate

@Composable
internal fun LocalMediaImportFailure.label(): String = stringResource(
    when (this) {
        LocalMediaImportFailure.InvalidSource -> R.string.feature_local_impl_failure_invalid
        LocalMediaImportFailure.PermissionDenied -> R.string.feature_local_impl_failure_permission
        LocalMediaImportFailure.SourceUnavailable -> R.string.feature_local_impl_failure_unavailable
        LocalMediaImportFailure.EmptyFile -> R.string.feature_local_impl_failure_empty
        LocalMediaImportFailure.UnsupportedFormat -> R.string.feature_local_impl_failure_unsupported
        LocalMediaImportFailure.MetadataUnavailable -> R.string.feature_local_impl_failure_metadata
        LocalMediaImportFailure.InsufficientStorage -> R.string.feature_local_impl_failure_storage
        LocalMediaImportFailure.HashFailed -> R.string.feature_local_impl_failure_hash
        LocalMediaImportFailure.SourceChanged -> R.string.feature_local_impl_failure_changed
        LocalMediaImportFailure.StorageUnavailable -> R.string.feature_local_impl_failure_storage
        LocalMediaImportFailure.IndexUnavailable -> R.string.feature_local_impl_failure_index
    },
)

internal fun LocalMedia.formatLabel(): String = fileExtension?.uppercase(Locale.ROOT)
    ?: mimeType?.substringAfter('/')?.uppercase(Locale.ROOT)
    ?: "AUDIO"

internal fun LocalMedia.supportingLabel(): String {
    val details = listOfNotNull(
        sampleRateHz?.let { if (it % 1_000 == 0) "${it / 1_000} kHz" else "${it / 1_000.0} kHz" },
        bitDepth?.let { "$it-bit" },
        bitrateBitsPerSecond?.let { "${it / 1_000} kbps" },
    )
    return (listOf(artist ?: albumTitle ?: displayName) + details).joinToString(" · ")
}

internal fun Long.durationLabel(): String {
    val totalSeconds = (this / 1_000).coerceAtLeast(0)
    return "%d:%02d".format(Locale.ROOT, totalSeconds / 60, totalSeconds % 60)
}

internal fun Long.fileSize(): String = when {
    this >= 1_073_741_824 -> "%.1f GB".format(Locale.ROOT, this / 1_073_741_824.0)
    this >= 1_048_576 -> "%.1f MB".format(Locale.ROOT, this / 1_048_576.0)
    this >= 1_024 -> "%.1f KB".format(Locale.ROOT, this / 1_024.0)
    else -> "$this B"
}
