@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.cloud.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteOutlinedButton
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.CloudStorage
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.ContentFailure

@Composable
fun CloudRoute(
    playingMediaId: String?,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onPlayRequest: (CloudPlaybackRequest) -> Unit,
    onAppendTracks: (List<CloudTrack>) -> Unit,
    viewModel: CloudViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.playbackRequests.collect(onPlayRequest)
    }
    CloudScreen(
        state = state,
        playingMediaId = playingMediaId,
        bottomContentPadding = bottomContentPadding,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onQueryChange = viewModel::updateQuery,
        onSortChange = viewModel::updateSort,
        onViewModeChange = viewModel::updateViewMode,
        onPlayAll = viewModel::playAll,
        onPlayTrack = viewModel::playTrack,
        onAppendTracks = onAppendTracks,
        onLoadMore = viewModel::loadMore,
        onRetryMore = viewModel::retryMore,
        onRetryPlayback = viewModel::retryPlayback,
        onDismissPlaybackIssue = viewModel::dismissPlaybackIssue,
    )
}

@Composable
internal fun CloudScreen(
    state: CloudUiState,
    playingMediaId: String?,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (CloudSort) -> Unit,
    onViewModeChange: (CloudViewMode) -> Unit,
    onPlayAll: () -> Unit,
    onPlayTrack: (String) -> Unit,
    onAppendTracks: (List<CloudTrack>) -> Unit,
    onLoadMore: () -> Unit,
    onRetryMore: () -> Unit,
    onRetryPlayback: () -> Unit,
    onDismissPlaybackIssue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = { Text(stringResource(R.string.feature_cloud_impl_cloud_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(R.string.feature_cloud_impl_cloud_back),
                        )
                    }
                },
                actions = {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Rounded.Refresh, stringResource(R.string.feature_cloud_impl_cloud_refresh))
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("cloud-list"),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "vault") {
                CloudVaultCard(state.storage, state.total.takeIf { it > 0 } ?: state.tracks.size)
            }

            when {
                state.initialLoading -> item(key = "loading") { LoadingState() }
                state.failure != null -> item(key = "error") { ErrorState(state.failure, onRefresh) }
                else -> cloudContent(
                    state = state,
                    playingMediaId = playingMediaId,
                    onQueryChange = onQueryChange,
                    onSortChange = onSortChange,
                    onViewModeChange = onViewModeChange,
                    onPlayAll = onPlayAll,
                    onPlayTrack = onPlayTrack,
                    onAppendTracks = onAppendTracks,
                    onLoadMore = onLoadMore,
                    onRetryMore = onRetryMore,
                    onRetryPlayback = onRetryPlayback,
                    onDismissPlaybackIssue = onDismissPlaybackIssue,
                )
            }
        }
    }
}

private fun LazyListScope.cloudContent(
    state: CloudUiState,
    playingMediaId: String?,
    onQueryChange: (String) -> Unit,
    onSortChange: (CloudSort) -> Unit,
    onViewModeChange: (CloudViewMode) -> Unit,
    onPlayAll: () -> Unit,
    onPlayTrack: (String) -> Unit,
    onAppendTracks: (List<CloudTrack>) -> Unit,
    onLoadMore: () -> Unit,
    onRetryMore: () -> Unit,
    onRetryPlayback: () -> Unit,
    onDismissPlaybackIssue: () -> Unit,
) {
    item(key = "tools") {
        CloudTools(
            state = state,
            onQueryChange = onQueryChange,
            onSortChange = onSortChange,
            onViewModeChange = onViewModeChange,
        )
    }
    if (state.playback is CloudPlaybackUiState.Failed) {
        item(key = "playback-error") {
            PlaybackIssueCard(
                issue = state.playback.issue,
                onRetry = onRetryPlayback,
                onDismiss = onDismissPlaybackIssue,
            )
        }
    }
    if (state.visibleTracks.isNotEmpty()) {
        item(key = "actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResonoteButton(
                    label = stringResource(R.string.feature_cloud_impl_cloud_play_all),
                    onClick = onPlayAll,
                    enabled = !state.isIndexing && state.playback !is CloudPlaybackUiState.Resolving,
                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                )
                ResonoteOutlinedButton(
                    label = stringResource(R.string.feature_cloud_impl_cloud_append_all),
                    onClick = { onAppendTracks(state.visibleTracks) },
                    enabled = !state.isIndexing,
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    when {
        state.tracks.isEmpty() -> item(key = "empty") {
            EmptyState(
                title = stringResource(R.string.feature_cloud_impl_cloud_empty_title),
                body = stringResource(R.string.feature_cloud_impl_cloud_empty_body),
            )
        }
        state.visibleTracks.isEmpty() && !state.isIndexing -> item(key = "no-results") {
            EmptyState(
                title = stringResource(R.string.feature_cloud_impl_cloud_no_results_title),
                body = stringResource(R.string.feature_cloud_impl_cloud_no_results_body, state.query.trim()),
            )
        }
        state.viewMode == CloudViewMode.List -> itemsIndexed(
            items = state.visibleTracks,
            key = { index, track -> "track-${track.hash}-$index" },
        ) { _, track ->
            CloudTrackRow(
                track = track,
                isPlaying = track.hash == playingMediaId,
                isResolving = (state.playback as? CloudPlaybackUiState.Resolving)?.trackHash == track.hash,
                onPlay = { onPlayTrack(track.hash) },
                onAppend = { onAppendTracks(listOf(track)) },
            )
        }
        else -> itemsIndexed(
            items = state.visibleTracks.chunked(2),
            key = { index, row -> "grid-${row.joinToString("-") { it.hash }}-$index" },
        ) { _, rowTracks ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowTracks.forEach { track ->
                    CloudTrackGridCard(
                        track = track,
                        isPlaying = track.hash == playingMediaId,
                        isResolving = (state.playback as? CloudPlaybackUiState.Resolving)?.trackHash == track.hash,
                        onPlay = { onPlayTrack(track.hash) },
                        onAppend = { onAppendTracks(listOf(track)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowTracks.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    if (state.isIndexing) {
        item(key = "indexing") {
            LoadingLine(stringResource(R.string.feature_cloud_impl_cloud_indexing))
        }
    } else if (state.isLoadingMore) {
        item(key = "loading-more") { LoadingLine(stringResource(R.string.feature_cloud_impl_cloud_load_more)) }
    } else if (state.loadMoreFailure != null) {
        item(key = "load-more-error") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.feature_cloud_impl_cloud_load_more_error),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onRetryMore) { Text(stringResource(R.string.feature_cloud_impl_cloud_retry)) }
            }
        }
    } else if (state.hasMore && state.query.isBlank()) {
        item(key = "load-more") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onLoadMore) { Text(stringResource(R.string.feature_cloud_impl_cloud_load_more)) }
            }
        }
    }
}

@Composable
private fun CloudVaultCard(storage: CloudStorage?, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("cloud-vault"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ),
                )
                .padding(22.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Cloud, contentDescription = null, modifier = Modifier.size(27.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(
                            stringResource(R.string.feature_cloud_impl_cloud_vault),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.feature_cloud_impl_cloud_total_tracks, total),
                            modifier = Modifier.padding(top = 3.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (storage != null && storage.maxBytes > 0) {
                    val used = storage.usedBytes.coerceIn(0, storage.maxBytes)
                    val available = (storage.maxBytes - used).coerceAtLeast(0)
                    LinearProgressIndicator(
                        progress = { used.toFloat() / storage.maxBytes.toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(top = 22.dp).height(7.dp).clip(CircleShape),
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 9.dp)) {
                        Text(
                            stringResource(
                                R.string.feature_cloud_impl_cloud_storage_used,
                                used.fileSize(),
                                storage.maxBytes.fileSize(),
                            ),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            stringResource(R.string.feature_cloud_impl_cloud_storage_available, available.fileSize()),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.feature_cloud_impl_cloud_storage_unknown),
                        modifier = Modifier.padding(top = 18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudTools(
    state: CloudUiState,
    onQueryChange: (String) -> Unit,
    onSortChange: (CloudSort) -> Unit,
    onViewModeChange: (CloudViewMode) -> Unit,
) {
    var sortExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.feature_cloud_impl_cloud_search_label)) },
            placeholder = { Text(stringResource(R.string.feature_cloud_impl_cloud_search_placeholder)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = if (state.query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.feature_cloud_impl_cloud_clear_search))
                    }
                }
            } else {
                null
            },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                FilterChip(
                    selected = state.sort != CloudSort.UploadOrder,
                    onClick = { sortExpanded = true },
                    label = { Text(stringResource(R.string.feature_cloud_impl_cloud_sort, state.sort.label())) },
                    trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null) },
                )
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    CloudSort.entries.forEach { sort ->
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
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onViewModeChange(CloudViewMode.List) }) {
                Icon(
                    Icons.AutoMirrored.Rounded.List,
                    stringResource(R.string.feature_cloud_impl_cloud_list_view),
                    tint =
                    if (state.viewMode == CloudViewMode.List) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(onClick = { onViewModeChange(CloudViewMode.Grid) }) {
                Icon(
                    Icons.Rounded.GridView,
                    stringResource(R.string.feature_cloud_impl_cloud_grid_view),
                    tint =
                    if (state.viewMode == CloudViewMode.Grid) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun CloudTrackRow(
    track: CloudTrack,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit,
    onAppend: () -> Unit,
) {
    Card(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
            if (isPlaying) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CloudArtwork(track, Modifier.size(56.dp))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(track.artist, track.album)
                        .filter(String::isNotBlank)
                        .joinToString(" · ")
                        .ifBlank {
                            stringResource(R.string.feature_cloud_impl_cloud_unknown_artist)
                        },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.durationMillis.durationLabel(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (isResolving) {
                CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(22.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onAppend) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.feature_cloud_impl_cloud_append_track, track.title))
                }
            }
        }
    }
}

@Composable
private fun CloudTrackGridCard(
    track: CloudTrack,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit,
    onAppend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onPlay,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor =
            if (isPlaying) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column {
            CloudArtwork(track, Modifier.fillMaxWidth().aspectRatio(1f))
            Column(modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 6.dp, bottom = 8.dp)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.artist?.takeIf(String::isNotBlank)
                        ?: stringResource(R.string.feature_cloud_impl_cloud_unknown_artist),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        track.durationMillis.durationLabel(),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    if (isResolving) {
                        CircularProgressIndicator(modifier = Modifier.padding(10.dp).size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = onAppend) {
                            Icon(
                                Icons.Rounded.Add,
                                stringResource(R.string.feature_cloud_impl_cloud_append_track, track.title),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudArtwork(track: CloudTrack, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Cloud,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.size(25.dp),
        )
        track.coverUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PlaybackIssueCard(issue: CloudPlaybackIssue, onRetry: () -> Unit, onDismiss: () -> Unit) {
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
private fun LoadingState() {
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
private fun ErrorState(failure: ContentFailure, onRetry: () -> Unit) {
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
private fun EmptyState(title: String, body: String) {
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
private fun LoadingLine(label: String) {
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
private fun CloudSort.label(): String = stringResource(
    when (this) {
        CloudSort.UploadOrder -> R.string.feature_cloud_impl_cloud_sort_upload
        CloudSort.Title -> R.string.feature_cloud_impl_cloud_sort_title
        CloudSort.Artist -> R.string.feature_cloud_impl_cloud_sort_artist
        CloudSort.Duration -> R.string.feature_cloud_impl_cloud_sort_duration
    },
)

@Composable
private fun ContentFailure.message(): String = stringResource(
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
private fun ContentFailure.playbackMessage(): String = when (this) {
    ContentFailure.AuthenticationRequired -> stringResource(R.string.feature_cloud_impl_cloud_error_auth)
    ContentFailure.Network -> stringResource(R.string.feature_cloud_impl_cloud_error_network)
    ContentFailure.RiskBlocked,
    is ContentFailure.RiskVerificationRequired,
    -> stringResource(R.string.feature_cloud_impl_cloud_error_risk)
    ContentFailure.ServiceRejected,
    ContentFailure.Protocol,
    -> stringResource(R.string.feature_cloud_impl_cloud_playback_failed)
}

private fun Long.durationLabel(): String {
    if (this <= 0) return "—:—"
    val totalSeconds = this / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun Long.fileSize(): String {
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
