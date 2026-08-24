@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.discover.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonotePullToRefreshBox
import com.resonote.core.designsystem.component.ResonoteTabbedToolbar
import com.resonote.core.model.Album
import com.resonote.core.model.AlbumRegion
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistSummary
import com.resonote.core.model.Ranking

@Composable
fun DiscoverRoute(
    bottomContentPadding: Dp,
    playingMediaId: String?,
    requestedSection: DiscoverSection? = null,
    onRequestedSectionConsumed: () -> Unit = {},
    onPlaylistClick: (PlaylistSummary) -> Unit,
    onRankingClick: (Ranking) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onPlaySongs: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarController = LocalResonoteSnackbarController.current
    val refreshFailureMessage = stringResource(R.string.feature_discover_impl_discover_refresh_failed)
    LaunchedEffect(viewModel, snackbarController) {
        viewModel.refreshFailures.collect { snackbarController?.show(refreshFailureMessage) }
    }
    LaunchedEffect(requestedSection) {
        requestedSection?.let {
            viewModel.selectSection(it)
            onRequestedSectionConsumed()
        }
    }
    DiscoverScreen(
        state = state,
        bottomContentPadding = bottomContentPadding,
        playingMediaId = playingMediaId,
        onSelectSection = viewModel::selectSection,
        onSelectPlaylistParent = viewModel::selectPlaylistParent,
        onSelectPlaylistCategory = viewModel::selectPlaylistCategory,
        onSelectAlbumRegion = viewModel::selectAlbumRegion,
        onRetryCategories = viewModel::retryCategories,
        onRetry = viewModel::retryCurrent,
        onRefresh = viewModel::refreshCurrent,
        onLoadMore = viewModel::loadMore,
        onPlaylistClick = onPlaylistClick,
        onRankingClick = onRankingClick,
        onAlbumClick = onAlbumClick,
        onPlaySongs = onPlaySongs,
        onSongClick = onSongClick,
        onSongMoreClick = onSongMoreClick,
    )
}

@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    bottomContentPadding: Dp,
    playingMediaId: String?,
    onSelectSection: (DiscoverSection) -> Unit,
    onSelectPlaylistParent: (Int?) -> Unit,
    onSelectPlaylistCategory: (Int) -> Unit,
    onSelectAlbumRegion: (AlbumRegion?) -> Unit,
    onRetryCategories: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPlaylistClick: (PlaylistSummary) -> Unit,
    onRankingClick: (Ranking) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onPlaySongs: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val stateHolder = rememberSaveableStateHolder()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTabbedToolbar(
                labels = DiscoverSection.entries.map { it.label() },
                selectedIndex = state.selectedSection.ordinal,
                onSelected = { onSelectSection(DiscoverSection.entries[it]) },
            )
        },
    ) { padding ->
        ResonotePullToRefreshBox(
            isRefreshing = state.refreshingSection == state.selectedSection,
            onRefresh = onRefresh,
            enabled = state.selectedSection.hasContent(state),
            modifier = Modifier.fillMaxSize().padding(padding).testTag("discover-pull-to-refresh"),
        ) {
            stateHolder.SaveableStateProvider(state.selectedSection.name) {
                when (state.selectedSection) {
                    DiscoverSection.PLAYLISTS -> PlaylistPane(
                        state = state,
                        bottomContentPadding = bottomContentPadding,
                        onSelectParent = onSelectPlaylistParent,
                        onSelectCategory = onSelectPlaylistCategory,
                        onRetryCategories = onRetryCategories,
                        onRetry = onRetry,
                        onLoadMore = onLoadMore,
                        onPlaylistClick = onPlaylistClick,
                    )
                    DiscoverSection.RANKINGS -> RankingPane(
                        rankings = state.rankings,
                        bottomContentPadding = bottomContentPadding,
                        onRetry = onRetry,
                        onRankingClick = onRankingClick,
                    )
                    DiscoverSection.ALBUMS -> AlbumPane(
                        albums = state.albums,
                        selectedRegion = state.selectedAlbumRegion,
                        bottomContentPadding = bottomContentPadding,
                        onSelectRegion = onSelectAlbumRegion,
                        onRetry = onRetry,
                        onAlbumClick = onAlbumClick,
                    )
                    DiscoverSection.SONGS -> SongPane(
                        songs = state.songs,
                        playingMediaId = playingMediaId,
                        bottomContentPadding = bottomContentPadding,
                        onRetry = onRetry,
                        onLoadMore = onLoadMore,
                        onPlaySongs = onPlaySongs,
                        onSongClick = onSongClick,
                        onSongMoreClick = onSongMoreClick,
                    )
                }
            }
        }
    }
}
