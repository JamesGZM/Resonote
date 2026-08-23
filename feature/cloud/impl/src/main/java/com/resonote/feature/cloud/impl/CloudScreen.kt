@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.cloud.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteOutlinedButton
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.CloudTrack

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
                state.failure != null -> item(key = "error") {
                    ErrorState(
                        failure = state.failure,
                        onRetry = onRefresh,
                        modifier = Modifier.fillParentMaxHeight(0.55f),
                    )
                }
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
                modifier = Modifier.fillParentMaxHeight(0.55f),
            )
        }
        state.visibleTracks.isEmpty() && !state.isIndexing -> item(key = "no-results") {
            EmptyState(
                title = stringResource(R.string.feature_cloud_impl_cloud_no_results_title),
                body = stringResource(R.string.feature_cloud_impl_cloud_no_results_body, state.query.trim()),
                modifier = Modifier.fillParentMaxHeight(0.55f),
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
