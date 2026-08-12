@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.local.impl

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteDestructiveTextButton
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonoteOutlinedButton
import com.resonote.core.designsystem.component.ResonoteTextField
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaImportFailure
import java.util.Locale

@Composable
fun LocalMusicRoute(
    playingMediaId: String?,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onPlayAll: (List<LocalMedia>) -> Unit,
    onPlayMedia: (LocalMedia) -> Unit,
    pendingImportRequestId: Long? = null,
    pendingImportUris: List<String> = emptyList(),
    onPendingImportAccepted: (Long) -> Unit = {},
    viewModel: LocalMusicViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(pendingImportRequestId, state.importState) {
        val requestId = pendingImportRequestId ?: return@LaunchedEffect
        if (viewModel.importUris(pendingImportUris)) onPendingImportAccepted(requestId)
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.importUris(uris.map { it.toString() })
    }
    LocalMusicScreen(
        state = state,
        playingMediaId = playingMediaId,
        bottomContentPadding = bottomContentPadding,
        onBack = onBack,
        onPickFiles = { picker.launch(arrayOf("audio/*")) },
        onQueryChange = viewModel::updateQuery,
        onSortChange = viewModel::updateSort,
        onPlayAll = { onPlayAll(state.visibleMedia) },
        onPlayMedia = onPlayMedia,
        onCancelImport = viewModel::cancelImport,
        onResolveDuplicate = viewModel::resolveDuplicate,
        onDismissImportResult = viewModel::dismissImportResult,
        onRequestDelete = viewModel::requestDelete,
        onDismissDelete = viewModel::dismissDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDeleteFailure = viewModel::dismissDeleteFailure,
    )
}

@Composable
internal fun LocalMusicScreen(
    state: LocalMusicUiState,
    playingMediaId: String?,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onPickFiles: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (LocalMusicSort) -> Unit,
    onPlayAll: () -> Unit,
    onPlayMedia: (LocalMedia) -> Unit,
    onCancelImport: () -> Unit,
    onResolveDuplicate: (Boolean) -> Unit,
    onDismissImportResult: () -> Unit,
    onRequestDelete: (LocalMedia) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteFailure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = { Text(stringResource(R.string.feature_local_impl_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(R.string.feature_local_impl_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onPickFiles,
                        enabled = state.importState !is LocalImportUiState.Running &&
                            state.importState !is LocalImportUiState.AwaitingDuplicate,
                    ) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.feature_local_impl_import))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("local-music-list"),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = bottomContentPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "summary") { LocalLibraryCard(state.media) }

            when (val importState = state.importState) {
                LocalImportUiState.Idle -> Unit
                is LocalImportUiState.Running -> item(key = "import-progress") {
                    ImportProgressCard(importState, onCancelImport)
                }
                is LocalImportUiState.AwaitingDuplicate -> Unit
                is LocalImportUiState.Completed -> item(key = "import-result") {
                    ImportResultCard(importState, onDismissImportResult)
                }
            }

            if (state.deleteFailed) {
                item(key = "delete-error") { DeleteFailureCard(onDismissDeleteFailure) }
            }

            if (!state.isLoading && state.media.isNotEmpty()) {
                item(key = "tools") {
                    LocalMusicTools(state, onQueryChange, onSortChange)
                }
                if (state.visibleMedia.isNotEmpty()) {
                    item(key = "play-all") {
                        ResonoteButton(
                            label = stringResource(
                                R.string.feature_local_impl_play_all,
                                state.visibleMedia.size,
                            ),
                            onClick = onPlayAll,
                            leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            when {
                state.isLoading -> item(key = "loading") { LoadingState() }
                state.media.isEmpty() -> item(key = "empty") { EmptyState(onPickFiles) }
                state.visibleMedia.isEmpty() -> item(key = "no-results") {
                    NoResultsState(state.query)
                }
                else -> items(state.visibleMedia, key = { it.id.value }) { media ->
                    LocalMediaRow(
                        media = media,
                        isPlaying = playingMediaId == media.id.value,
                        isDeleting = state.deletingMediaId == media.id.value,
                        onPlay = { onPlayMedia(media) },
                        onDelete = { onRequestDelete(media) },
                    )
                }
            }
        }
    }

    (state.importState as? LocalImportUiState.AwaitingDuplicate)?.let { duplicate ->
        DuplicateDialog(
            duplicate = duplicate,
            onCancel = { onResolveDuplicate(false) },
            onImportCopy = { onResolveDuplicate(true) },
        )
    }
    state.pendingDelete?.let { media ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.feature_local_impl_delete_title)) },
            text = { Text(stringResource(R.string.feature_local_impl_delete_body, media.title)) },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text(stringResource(R.string.feature_local_impl_cancel))
                }
            },
            confirmButton = {
                ResonoteDestructiveTextButton(
                    label = stringResource(R.string.feature_local_impl_delete_confirm),
                    onClick = onConfirmDelete,
                )
            },
        )
    }
}

@Composable
private fun LocalLibraryCard(media: List<LocalMedia>) {
    val totalBytes = media.sumOf(LocalMedia::sizeBytes)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.62f),
                        ),
                    ),
                )
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.LibraryMusic, contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(
                        stringResource(R.string.feature_local_impl_on_device),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.feature_local_impl_summary, media.size, totalBytes.fileSize()),
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.feature_local_impl_private_copy),
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalMusicTools(
    state: LocalMusicUiState,
    onQueryChange: (String) -> Unit,
    onSortChange: (LocalMusicSort) -> Unit,
) {
    var sortExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ResonoteTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = stringResource(R.string.feature_local_impl_search),
            placeholder = stringResource(R.string.feature_local_impl_search_hint),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingAction = if (state.query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.feature_local_impl_clear_search))
                    }
                }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Box {
            FilterChip(
                selected = state.sort != LocalMusicSort.ImportedNewest,
                onClick = { sortExpanded = true },
                label = { Text(stringResource(R.string.feature_local_impl_sort, state.sort.label())) },
                trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null) },
            )
            DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                LocalMusicSort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = { Text(sort.label()) },
                        onClick = {
                            onSortChange(sort)
                            sortExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalMediaRow(
    media: LocalMedia,
    isPlaying: Boolean,
    isDeleting: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        ResonoteMusicItem(
            title = media.title,
            supportingText = media.supportingLabel(),
            duration = media.durationMillis.durationLabel(),
            onClick = onPlay,
            onMoreClick = { menuExpanded = true },
            artworkState = if (media.artworkUri == null) ResonoteArtworkState.MISSING else ResonoteArtworkState.LOADED,
            artwork = {
                AsyncImage(
                    model = media.artworkUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            },
            qualityLabel = media.formatLabel(),
            isPlaying = isPlaying,
            enabled = !isDeleting,
        )
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.feature_local_impl_delete)) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onDelete()
                },
            )
        }
        if (isDeleting) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).size(24.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun ImportProgressCard(state: LocalImportUiState.Running, onCancel: () -> Unit) {
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
private fun ImportResultCard(state: LocalImportUiState.Completed, onDismiss: () -> Unit) {
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
private fun DuplicateDialog(
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
private fun LoadingState() {
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
private fun EmptyState(onPickFiles: () -> Unit) {
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
    }
}

@Composable
private fun NoResultsState(query: String) {
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
private fun DeleteFailureCard(onDismiss: () -> Unit) {
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
private fun LocalMusicSort.label(): String = stringResource(
    when (this) {
        LocalMusicSort.ImportedNewest -> R.string.feature_local_impl_sort_imported
        LocalMusicSort.Title -> R.string.feature_local_impl_sort_title
        LocalMusicSort.Artist -> R.string.feature_local_impl_sort_artist
        LocalMusicSort.Duration -> R.string.feature_local_impl_sort_duration
    },
)

@Composable
private fun LocalMediaImportFailure.label(): String = stringResource(
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

private fun LocalMedia.formatLabel(): String {
    return fileExtension?.uppercase(Locale.ROOT)
        ?: mimeType?.substringAfter('/')?.uppercase(Locale.ROOT)
        ?: "AUDIO"
}

private fun LocalMedia.supportingLabel(): String {
    val details = listOfNotNull(
        sampleRateHz?.let { if (it % 1_000 == 0) "${it / 1_000} kHz" else "${it / 1_000.0} kHz" },
        bitDepth?.let { "$it-bit" },
        bitrateBitsPerSecond?.let { "${it / 1_000} kbps" },
    )
    return (listOf(artist ?: albumTitle ?: displayName) + details).joinToString(" · ")
}

private fun Long.durationLabel(): String {
    val totalSeconds = (this / 1_000).coerceAtLeast(0)
    return "%d:%02d".format(Locale.ROOT, totalSeconds / 60, totalSeconds % 60)
}

private fun Long.fileSize(): String = when {
    this >= 1_073_741_824 -> "%.1f GB".format(Locale.ROOT, this / 1_073_741_824.0)
    this >= 1_048_576 -> "%.1f MB".format(Locale.ROOT, this / 1_048_576.0)
    this >= 1_024 -> "%.1f KB".format(Locale.ROOT, this / 1_024.0)
    else -> "$this B"
}
