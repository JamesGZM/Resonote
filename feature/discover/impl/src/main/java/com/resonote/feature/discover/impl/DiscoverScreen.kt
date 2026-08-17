@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.discover.impl

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonoteArtwork
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteEmptyState
import com.resonote.core.designsystem.component.ResonoteErrorState
import com.resonote.core.designsystem.component.ResonoteFilterPill
import com.resonote.core.designsystem.component.ResonoteLoadMoreEffect
import com.resonote.core.designsystem.component.ResonoteLoadMoreFooter
import com.resonote.core.designsystem.component.ResonoteLoadMoreState
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonotePlainAction
import com.resonote.core.designsystem.component.ResonotePlaylistItem
import com.resonote.core.designsystem.component.ResonotePlaylistMetadata
import com.resonote.core.designsystem.component.ResonotePullToRefreshBox
import com.resonote.core.designsystem.component.ResonoteRemoteArtwork
import com.resonote.core.designsystem.component.ResonoteSectionHeader
import com.resonote.core.designsystem.component.ResonoteShimmer
import com.resonote.core.designsystem.component.ResonoteTabbedToolbar
import com.resonote.core.designsystem.component.rememberResonoteShimmer
import com.resonote.core.designsystem.component.resonoteShimmer
import com.resonote.core.model.Album
import com.resonote.core.model.AlbumRegion
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistCategory
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

@Composable
private fun PlaylistPane(
    state: DiscoverUiState,
    bottomContentPadding: Dp,
    onSelectParent: (Int?) -> Unit,
    onSelectCategory: (Int) -> Unit,
    onRetryCategories: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPlaylistClick: (PlaylistSummary) -> Unit,
) {
    val listState = rememberLazyListState()
    val shimmer = rememberResonoteShimmer("discover-playlists-skeleton")
    val page = state.playlists as? DiscoverPageState.Content
    ResonoteLoadMoreEffect(
        listState = listState,
        itemCount = page?.items?.size ?: 0,
        enabled = page?.let { it.hasMore && !it.isLoadingMore && it.loadMoreFailure == null } == true,
        onLoadMore = onLoadMore,
    )
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag("discover-playlists"),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
    ) {
        item(key = "category-filters") {
            PlaylistFilters(
                categories = state.categories,
                selectedParentId = state.selectedParentCategoryId,
                selectedCategoryId = state.selectedPlaylistCategoryId,
                onSelectParent = onSelectParent,
                onSelectCategory = onSelectCategory,
                onRetry = onRetryCategories,
            )
        }
        when (val page = state.playlists) {
            DiscoverPageState.Idle,
            DiscoverPageState.Loading,
            -> item(key = "loading") { PlaylistSkeleton(shimmer) }
            DiscoverPageState.Empty -> item(key = "empty") {
                ResonoteEmptyState(
                    title = stringResource(R.string.feature_discover_impl_discover_empty_playlists),
                    message = stringResource(R.string.feature_discover_impl_discover_empty_playlists_supporting),
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                )
            }
            is DiscoverPageState.Error -> item(key = "error") { PaneError(page.failure, onRetry) }
            is DiscoverPageState.Content -> {
                itemsIndexed(
                    items = page.items.chunked(2),
                    key = { index, row -> "row-${row.first().id}-$index" },
                ) { index, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(
                            start = 16.dp,
                            top = if (index == 0) 16.dp else 8.dp,
                            end = 16.dp,
                            bottom = 8.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { playlist ->
                            ResonotePlaylistItem(
                                metadata = ResonotePlaylistMetadata(
                                    title = playlist.title,
                                    playCount = playlist.playCount?.compactCount(),
                                ),
                                onClick = { onPlaylistClick(playlist) },
                                modifier = Modifier.weight(1f),
                                artworkState = if (playlist.coverUrl.isNullOrBlank()) {
                                    ResonoteArtworkState.MISSING
                                } else {
                                    ResonoteArtworkState.LOADED
                                },
                                artworkUrl = playlist.coverUrl,
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (page.isLoadingMore || page.loadMoreFailure != null) {
                    item(key = "load-more") {
                        ResonoteLoadMoreFooter(
                            state = if (page.isLoadingMore) {
                                ResonoteLoadMoreState.LOADING
                            } else {
                                ResonoteLoadMoreState.ERROR
                            },
                            onRetry = onLoadMore,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistFilters(
    categories: DiscoverLoadState<List<PlaylistCategory>>,
    selectedParentId: Int?,
    selectedCategoryId: Int,
    onSelectParent: (Int?) -> Unit,
    onSelectCategory: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    Column {
        when (categories) {
            DiscoverLoadState.Idle,
            DiscoverLoadState.Loading,
            -> LinearFilterLoading(Modifier.padding(vertical = 8.dp))
            DiscoverLoadState.Empty -> Unit
            is DiscoverLoadState.Error -> TextButton(
                onClick = onRetry,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            ) { Text(stringResource(R.string.feature_discover_impl_discover_categories_unavailable)) }
            is DiscoverLoadState.Content -> {
                LazyRow(
                    modifier = Modifier.selectableGroup().padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "recommended") {
                        ResonoteFilterPill(
                            label = stringResource(R.string.feature_discover_impl_discover_recommended),
                            selected = selectedParentId == null,
                            onClick = { onSelectParent(null) },
                        )
                    }
                    items(
                        items = categories.value,
                        key = { "parent-${it.tagId}" },
                    ) { category ->
                        ResonoteFilterPill(
                            label = category.name,
                            selected = category.tagId == selectedParentId,
                            onClick = { onSelectParent(category.tagId) },
                        )
                    }
                }
                categories.value.firstOrNull { it.tagId == selectedParentId }
                    ?.children?.takeIf(List<PlaylistCategory>::isNotEmpty)?.let { children ->
                        LazyRow(
                            modifier = Modifier.selectableGroup().padding(vertical = 4.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            itemsIndexed(
                                items = children,
                                key = { index, category -> "child-${category.tagId}-$index" },
                            ) { _, category ->
                                ResonoteFilterPill(
                                    label = category.name,
                                    selected = category.tagId == selectedCategoryId,
                                    onClick = { onSelectCategory(category.tagId) },
                                )
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun RankingPane(
    rankings: DiscoverLoadState<List<Ranking>>,
    bottomContentPadding: Dp,
    onRetry: () -> Unit,
    onRankingClick: (Ranking) -> Unit,
) {
    val listState = rememberLazyListState()
    val shimmer = rememberResonoteShimmer("discover-rankings-skeleton")
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag("discover-rankings"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp).plusBottom(bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (rankings) {
            DiscoverLoadState.Idle,
            DiscoverLoadState.Loading,
            -> items(6) { RankingSkeleton(shimmer) }
            DiscoverLoadState.Empty -> item {
                ResonoteEmptyState(
                    title = stringResource(R.string.feature_discover_impl_discover_empty_rankings),
                    message = stringResource(R.string.feature_discover_impl_discover_empty_rankings_supporting),
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                )
            }
            is DiscoverLoadState.Error -> item { PaneError(rankings.failure, onRetry) }
            is DiscoverLoadState.Content -> items(
                items = rankings.value,
                key = Ranking::id,
            ) { ranking ->
                RankingCard(ranking, onClick = { onRankingClick(ranking) })
            }
        }
    }
}

@Composable
private fun AlbumPane(
    albums: DiscoverLoadState<List<Album>>,
    selectedRegion: AlbumRegion?,
    bottomContentPadding: Dp,
    onSelectRegion: (AlbumRegion?) -> Unit,
    onRetry: () -> Unit,
    onAlbumClick: (Album) -> Unit,
) {
    val listState = rememberLazyListState()
    val shimmer = rememberResonoteShimmer("discover-albums-skeleton")
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag("discover-albums"),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
    ) {
        item(key = "regions") {
            val regions = AlbumRegion.entries
            LazyRow(
                modifier = Modifier.selectableGroup().padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "all") {
                    ResonoteFilterPill(
                        label = stringResource(R.string.feature_discover_impl_discover_all),
                        selected = selectedRegion == null,
                        onClick = { onSelectRegion(null) },
                    )
                }
                items(
                    items = regions,
                    key = AlbumRegion::name,
                ) { region ->
                    ResonoteFilterPill(
                        label = region.label(),
                        selected = selectedRegion == region,
                        onClick = { onSelectRegion(region) },
                    )
                }
            }
        }
        when (albums) {
            DiscoverLoadState.Idle,
            DiscoverLoadState.Loading,
            -> item { AlbumGridSkeleton(shimmer) }
            DiscoverLoadState.Empty -> item {
                ResonoteEmptyState(
                    title = stringResource(R.string.feature_discover_impl_discover_empty_albums),
                    message = stringResource(R.string.feature_discover_impl_discover_empty_albums_supporting),
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                )
            }
            is DiscoverLoadState.Error -> item { PaneError(albums.failure, onRetry) }
            is DiscoverLoadState.Content -> {
                val filtered = albums.value.filter { selectedRegion == null || it.region == selectedRegion }
                if (filtered.isEmpty()) {
                    item {
                        ResonoteEmptyState(
                            title = stringResource(R.string.feature_discover_impl_discover_empty_albums),
                            message = stringResource(R.string.feature_discover_impl_discover_empty_albums_supporting),
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                        )
                    }
                } else {
                    itemsIndexed(
                        items = filtered.chunked(2),
                        key = { index, row -> "album-row-${row.first().id}-$index" },
                    ) { index, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(
                                start = 16.dp,
                                top = if (index == 0) 16.dp else 8.dp,
                                end = 16.dp,
                                bottom = 8.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { album -> AlbumCard(album, { onAlbumClick(album) }, Modifier.weight(1f)) }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongPane(
    songs: DiscoverPageState<OnlineSong>,
    playingMediaId: String?,
    bottomContentPadding: Dp,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPlaySongs: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
) {
    val listState = rememberLazyListState()
    val shimmer = rememberResonoteShimmer("discover-songs-skeleton")
    val page = songs as? DiscoverPageState.Content
    ResonoteLoadMoreEffect(
        listState = listState,
        itemCount = page?.items?.size ?: 0,
        enabled = page?.let { it.hasMore && !it.isLoadingMore && it.loadMoreFailure == null } == true,
        onLoadMore = onLoadMore,
    )
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag("discover-songs"),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
    ) {
        when (songs) {
            DiscoverPageState.Idle,
            DiscoverPageState.Loading,
            -> item { SongListSkeleton(shimmer) }
            DiscoverPageState.Empty -> item {
                ResonoteEmptyState(
                    title = stringResource(R.string.feature_discover_impl_discover_empty_songs),
                    message = stringResource(R.string.feature_discover_impl_discover_empty_songs_supporting),
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                )
            }
            is DiscoverPageState.Error -> item { PaneError(songs.failure, onRetry) }
            is DiscoverPageState.Content -> {
                item(key = "play-all") {
                    ResonoteSectionHeader(
                        title = stringResource(R.string.feature_discover_impl_discover_new_songs_title),
                        supportingText = stringResource(R.string.feature_discover_impl_discover_new_songs_supporting),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        trailingContent = {
                            ResonotePlainAction(onClick = { onPlaySongs(songs.items) }) {
                                Text(
                                    text = stringResource(R.string.feature_discover_impl_discover_play_all),
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        },
                    )
                }
                itemsIndexed(
                    items = songs.items,
                    key = { index, song -> "${song.hash}-$index" },
                ) { _, song ->
                    ResonoteMusicItem(
                        title = song.title,
                        supportingText = song.artist.orEmpty(),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        duration = song.durationMillis.durationLabel(),
                        qualityLabel = song.quality.label(),
                        isVip = song.vip,
                        isPlaying = song.hash == playingMediaId,
                        artworkUrl = song.coverUrl,
                        onClick = { onSongClick(song) },
                        onMoreClick = onSongMoreClick?.let { callback -> { callback(song) } },
                    )
                }
                if (songs.isLoadingMore || songs.loadMoreFailure != null) {
                    item(key = "load-more") {
                        ResonoteLoadMoreFooter(
                            state = if (songs.isLoadingMore) {
                                ResonoteLoadMoreState.LOADING
                            } else {
                                ResonoteLoadMoreState.ERROR
                            },
                            onRetry = onLoadMore,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingCard(ranking: Ranking, onClick: () -> Unit) {
    ResonotePlainAction(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(60.dp).clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.BarChart,
                    contentDescription = stringResource(
                        R.string.feature_discover_impl_discover_ranking_artwork,
                        ranking.title,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!ranking.coverUrl.isNullOrBlank()) {
                    ResonoteRemoteArtwork(
                        model = ranking.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        fallback = {},
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(
                ranking.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ResonotePlainAction(onClick = onClick, modifier = modifier) {
        Column {
            ResonoteArtwork(
                state = if (album.coverUrl.isNullOrBlank()) {
                    ResonoteArtworkState.MISSING
                } else {
                    ResonoteArtworkState.LOADED
                },
                contentDescription = stringResource(R.string.feature_discover_impl_discover_album_artwork, album.name),
                modifier = Modifier.fillMaxWidth().height(164.dp),
            ) {
                ResonoteRemoteArtwork(
                    model = album.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                album.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(
                    R.string.feature_discover_impl_discover_album_metadata,
                    album.artist.orEmpty(),
                    album.songCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LinearFilterLoading(modifier: Modifier = Modifier) {
    val shimmer = rememberResonoteShimmer("discover-filter-skeleton")
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(4) {
            Spacer(Modifier.width(72.dp).height(32.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.large))
        }
    }
}

@Composable
private fun PaneError(failure: ContentFailure, onRetry: () -> Unit) {
    val body = when (failure) {
        ContentFailure.Network -> stringResource(R.string.feature_discover_impl_discover_error_network)
        ContentFailure.AuthenticationRequired -> stringResource(R.string.feature_discover_impl_discover_error_auth)
        else -> stringResource(R.string.feature_discover_impl_discover_error_generic)
    }
    ResonoteErrorState(
        onRetry = onRetry,
        message = body,
        title = stringResource(R.string.feature_discover_impl_discover_error_title),
        retryLabel = stringResource(R.string.feature_discover_impl_discover_retry),
        modifier = Modifier.fillMaxWidth().height(300.dp),
    )
}

@Composable
private fun PlaylistSkeleton(shimmer: ResonoteShimmer) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(
                            Modifier.fillMaxWidth().aspectRatio(1f)
                                .resonoteShimmer(shimmer, MaterialTheme.shapes.large),
                        )
                        SkeletonLine(shimmer, 116.dp, 15.dp)
                        SkeletonLine(shimmer, 72.dp, 12.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingSkeleton(shimmer: ResonoteShimmer) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.size(60.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.medium))
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonLine(shimmer, 164.dp, 16.dp)
            SkeletonLine(shimmer, 96.dp, 11.dp)
        }
    }
}

@Composable
private fun AlbumGridSkeleton(shimmer: ResonoteShimmer) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(
                            Modifier.fillMaxWidth().height(164.dp)
                                .resonoteShimmer(shimmer, MaterialTheme.shapes.large),
                        )
                        SkeletonLine(shimmer, 120.dp, 15.dp)
                        SkeletonLine(shimmer, 88.dp, 12.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SongListSkeleton(shimmer: ResonoteShimmer) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonLine(shimmer, 116.dp, 18.dp)
                SkeletonLine(shimmer, 152.dp, 11.dp)
            }
            SkeletonLine(shimmer, 64.dp, 14.dp)
        }
        repeat(7) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.size(56.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.medium))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonLine(shimmer, 176.dp, 15.dp)
                    SkeletonLine(shimmer, 108.dp, 12.dp)
                }
            }
        }
    }
}

@Composable
private fun SkeletonLine(shimmer: ResonoteShimmer, width: Dp, height: Dp) {
    Spacer(Modifier.width(width).height(height).resonoteShimmer(shimmer, MaterialTheme.shapes.small))
}

private fun DiscoverSection.hasContent(state: DiscoverUiState): Boolean = when (this) {
    DiscoverSection.PLAYLISTS -> state.playlists is DiscoverPageState.Content
    DiscoverSection.RANKINGS -> state.rankings is DiscoverLoadState.Content
    DiscoverSection.ALBUMS -> state.albums is DiscoverLoadState.Content
    DiscoverSection.SONGS -> state.songs is DiscoverPageState.Content
}

@Composable
private fun DiscoverSection.label(): String = when (this) {
    DiscoverSection.PLAYLISTS -> stringResource(R.string.feature_discover_impl_discover_playlists)
    DiscoverSection.RANKINGS -> stringResource(R.string.feature_discover_impl_discover_rankings)
    DiscoverSection.ALBUMS -> stringResource(R.string.feature_discover_impl_discover_albums)
    DiscoverSection.SONGS -> stringResource(R.string.feature_discover_impl_discover_songs)
}

@Composable
private fun AlbumRegion.label(): String = when (this) {
    AlbumRegion.Chinese -> stringResource(R.string.feature_discover_impl_discover_chinese)
    AlbumRegion.Western -> stringResource(R.string.feature_discover_impl_discover_western)
    AlbumRegion.Japanese -> stringResource(R.string.feature_discover_impl_discover_japanese)
    AlbumRegion.Korean -> stringResource(R.string.feature_discover_impl_discover_korean)
}

private fun PaddingValues.plusBottom(extra: Dp) = PaddingValues(
    start = calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
    top = calculateTopPadding(),
    end = calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
    bottom = calculateBottomPadding() + extra,
)

private fun Long.compactCount(): String = when {
    this >= 100_000_000 -> "%.1f亿".format(this / 100_000_000.0)
    this >= 10_000 -> "%.1f万".format(this / 10_000.0)
    else -> toString()
}

private fun Long.durationLabel(): String {
    val totalSeconds = coerceAtLeast(0) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun AudioQuality.label(): String? = when (this) {
    AudioQuality.Standard -> null
    AudioQuality.HighQuality -> "HQ"
    AudioQuality.HighResolution -> "HI-RES"
    AudioQuality.Lossless -> "LOSSLESS"
}
