@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.local.impl

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteDestructiveTextButton
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.LocalMedia

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
    val directoryPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { viewModel.importDirectory(it.toString()) }
    }
    LocalMusicScreen(
        state = state,
        playingMediaId = playingMediaId,
        bottomContentPadding = bottomContentPadding,
        onBack = onBack,
        onPickFiles = { picker.launch(arrayOf("audio/*")) },
        onPickDirectory = { directoryPicker.launch(null) },
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
    onPickDirectory: () -> Unit,
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
                title = {
                    LocalSearchInput(
                        value = state.query,
                        onValueChange = onQueryChange,
                        placeholder = stringResource(R.string.feature_local_impl_search_hint),
                        clearLabel = stringResource(R.string.feature_local_impl_clear_search),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(R.string.feature_local_impl_back),
                        )
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
            if (!state.isLoading && state.media.isNotEmpty()) {
                item(key = "summary") {
                    LocalLibrarySummary(
                        media = state.media,
                        importEnabled = !state.importState.isBusy(),
                        onPickFiles = onPickFiles,
                        onPickDirectory = onPickDirectory,
                    )
                }
            }

            when (val importState = state.importState) {
                LocalImportUiState.Idle -> Unit
                LocalImportUiState.ScanningDirectory -> item(key = "directory-scan") {
                    DirectoryScanCard(onCancelImport)
                }
                is LocalImportUiState.Running -> item(key = "import-progress") {
                    ImportProgressCard(importState, onCancelImport)
                }
                is LocalImportUiState.AwaitingDuplicate -> Unit
                is LocalImportUiState.Completed -> item(key = "import-result") {
                    ImportResultCard(importState, onDismissImportResult)
                }
                is LocalImportUiState.DirectoryFailed -> item(key = "directory-error") {
                    DirectoryFailureCard(importState.reason, onDismissImportResult)
                }
            }

            if (state.deleteFailed) {
                item(key = "delete-error") { DeleteFailureCard(onDismissDeleteFailure) }
            }

            if (!state.isLoading && state.media.isNotEmpty()) {
                item(key = "tools") {
                    LocalMusicTools(state, onSortChange, onPlayAll)
                }
            }

            when {
                state.isLoading -> item(key = "loading") { LoadingState() }
                state.media.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        onPickFiles = onPickFiles,
                        onPickDirectory = onPickDirectory,
                        modifier = Modifier.fillParentMaxHeight(0.55f),
                    )
                }
                state.visibleMedia.isEmpty() -> item(key = "no-results") {
                    NoResultsState(
                        query = state.query,
                        modifier = Modifier.fillParentMaxHeight(0.55f),
                    )
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
