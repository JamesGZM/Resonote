@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.discover.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteEmptyState
import com.resonote.core.designsystem.component.ResonoteFilterPill
import com.resonote.core.designsystem.component.ResonoteLoadMoreEffect
import com.resonote.core.designsystem.component.ResonoteLoadMoreFooter
import com.resonote.core.designsystem.component.ResonoteLoadMoreState
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonotePlainAction
import com.resonote.core.designsystem.component.ResonoteSectionHeader
import com.resonote.core.designsystem.component.rememberResonoteShimmer
import com.resonote.core.model.Album
import com.resonote.core.model.AlbumRegion
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.Ranking

@Composable
internal fun RankingPane(
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
internal fun AlbumPane(
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
internal fun SongPane(
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
