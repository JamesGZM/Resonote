@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.cloud.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.resonote.core.designsystem.component.ResonoteLoadMoreEffect
import com.resonote.core.designsystem.component.ResonoteLoadMoreFooter
import com.resonote.core.designsystem.component.ResonoteLoadMoreState
import com.resonote.core.designsystem.component.ResonotePullToRefreshBox
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
    onPlayAll: () -> Unit,
    onPlayTrack: (String) -> Unit,
    onAppendTracks: (List<CloudTrack>) -> Unit,
    onLoadMore: () -> Unit,
    onRetryMore: () -> Unit,
    onRetryPlayback: () -> Unit,
    onDismissPlaybackIssue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    ResonoteLoadMoreEffect(
        listState = listState,
        itemCount = state.visibleTracks.size,
        enabled = state.query.isBlank() &&
            state.hasMore &&
            !state.isLoadingMore &&
            state.loadMoreFailure == null &&
            !state.initialLoading,
        onLoadMore = onLoadMore,
    )
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = {
                    CloudSearchInput(
                        value = state.query,
                        onValueChange = onQueryChange,
                        placeholder = stringResource(R.string.feature_cloud_impl_cloud_search_placeholder),
                        clearLabel = stringResource(R.string.feature_cloud_impl_cloud_clear_search),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(R.string.feature_cloud_impl_cloud_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        ResonotePullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            enabled = state.tracks.isNotEmpty(),
            modifier = Modifier.fillMaxSize().padding(padding).testTag("cloud-pull-to-refresh"),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("cloud-list"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 10.dp,
                    end = 16.dp,
                    bottom = bottomContentPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!state.initialLoading && state.failure == null) {
                    item(key = "summary") {
                        CloudLibrarySummary(state.storage, state.total.takeIf { it > 0 } ?: state.tracks.size)
                    }
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
                        onPlayAll = onPlayAll,
                        onPlayTrack = onPlayTrack,
                        onAppendTracks = onAppendTracks,
                        onRetryMore = onRetryMore,
                        onRetryPlayback = onRetryPlayback,
                        onDismissPlaybackIssue = onDismissPlaybackIssue,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.cloudContent(
    state: CloudUiState,
    playingMediaId: String?,
    onQueryChange: (String) -> Unit,
    onSortChange: (CloudSort) -> Unit,
    onPlayAll: () -> Unit,
    onPlayTrack: (String) -> Unit,
    onAppendTracks: (List<CloudTrack>) -> Unit,
    onRetryMore: () -> Unit,
    onRetryPlayback: () -> Unit,
    onDismissPlaybackIssue: () -> Unit,
) {
    item(key = "tools") {
        CloudTools(
            state = state,
            onSortChange = onSortChange,
            onPlayAll = onPlayAll,
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
        else -> itemsIndexed(
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
    }
    if (state.isIndexing) {
        item(key = "indexing") {
            LoadingLine(stringResource(R.string.feature_cloud_impl_cloud_indexing))
        }
    } else if (state.isLoadingMore || state.loadMoreFailure != null) {
        item(key = "load-more") {
            ResonoteLoadMoreFooter(
                state = if (state.isLoadingMore) {
                    ResonoteLoadMoreState.LOADING
                } else {
                    ResonoteLoadMoreState.ERROR
                },
                onRetry = onRetryMore,
            )
        }
    }
}
