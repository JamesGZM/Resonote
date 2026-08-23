package com.resonote.feature.artist.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteEmptyState
import com.resonote.core.designsystem.component.ResonoteErrorState
import com.resonote.core.designsystem.component.ResonoteLoadMoreEffect
import com.resonote.core.designsystem.component.ResonoteLoadMoreFooter
import com.resonote.core.designsystem.component.ResonoteLoadMoreState
import com.resonote.core.designsystem.component.ResonoteMediaCardItem
import com.resonote.core.designsystem.component.ResonoteMediaCardMetadata
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonotePlainAction
import com.resonote.core.designsystem.component.ResonotePullToRefreshBox
import com.resonote.core.designsystem.component.ResonoteSectionHeader
import com.resonote.core.designsystem.component.ResonoteTabbedToolbar
import com.resonote.core.designsystem.component.ResonoteVideoItem
import com.resonote.core.designsystem.component.ResonoteVideoMetadata
import com.resonote.core.model.ArtistAlbum
import com.resonote.core.model.ArtistVideo
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong

@Composable
internal fun ArtistContent(
    state: ArtistUiState,
    profile: ArtistProfile? = state.profile,
    playingMediaId: String?,
    onBack: () -> Unit,
    onFollowClick: () -> Unit,
    onSelectSection: (ArtistSection) -> Unit,
    onSelectSort: (ArtistSort) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    onAlbumClick: (ArtistAlbum) -> Unit,
    onVideoClick: (ArtistVideo) -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val listState = remember(profile?.id) { LazyListState() }
    val page = state.selectedPage()
    val content = page as? ArtistPageUiState.Content
    ResonoteLoadMoreEffect(
        listState = listState,
        itemCount = content?.items?.size ?: 0,
        enabled = content?.let {
            it.hasMore && !it.isLoadingMore && !it.isRefreshing && it.loadMoreFailure == null
        } == true,
        onLoadMore = onLoadMore,
    )
    ResonotePullToRefreshBox(
        isRefreshing = content?.isRefreshing == true,
        onRefresh = onRefresh,
        enabled = content != null,
        modifier = modifier.fillMaxSize().testTag("artist-pull-to-refresh"),
    ) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag(
                    if (page is ArtistPageUiState.Loading || page is ArtistPageUiState.Idle) {
                        "artist-skeleton"
                    } else {
                        "artist-list"
                    },
                ),
                contentPadding = PaddingValues(bottom = bottomContentPadding),
            ) {
                item(key = "profile") {
                    ArtistHeader(
                        profile = profile,
                        follow = state.follow,
                        onFollowClick = onFollowClick,
                    )
                }
                item(key = "sections") {
                    ResonoteTabbedToolbar(
                        labels = ArtistSection.entries.map { it.label() },
                        selectedIndex = state.selectedSection.ordinal,
                        onSelected = { onSelectSection(ArtistSection.entries[it]) },
                        modifier = Modifier.testTag("artist-section-tabs"),
                        windowInsets = WindowInsets(0, 0, 0, 0),
                    )
                }
                if (state.selectedSection != ArtistSection.MVS) {
                    item(key = "section-actions") {
                        ArtistSectionActions(
                            section = state.selectedSection,
                            sort = state.selectedSort,
                            content = content,
                            onSelectSort = onSelectSort,
                            onPlayAll = onPlayAll,
                        )
                    }
                }
                when (page) {
                    ArtistPageUiState.Idle,
                    ArtistPageUiState.Loading,
                    -> artistSkeleton(state.selectedSection)
                    ArtistPageUiState.Empty -> item(key = "empty") {
                        ResonoteEmptyState(
                            title = state.selectedSection.emptyTitle(),
                            message = state.selectedSection.emptyBody(),
                            modifier = Modifier.fillParentMaxHeight(0.55f),
                        )
                    }
                    is ArtistPageUiState.Error -> item(key = "error") {
                        ResonoteErrorState(
                            onRetry = onRetry,
                            title = stringResource(R.string.feature_artist_impl_artist_error_title),
                            message = page.failure.errorMessage(),
                            retryLabel = stringResource(R.string.feature_artist_impl_artist_retry),
                            modifier = Modifier.fillParentMaxHeight(0.55f),
                        )
                    }
                    is ArtistPageUiState.Content -> {
                        when (state.selectedSection) {
                            ArtistSection.SONGS -> songs(
                                page = page,
                                playingMediaId = playingMediaId,
                                onSongClick = onSongClick,
                                onSongMoreClick = onSongMoreClick,
                            )
                            ArtistSection.ALBUMS -> albums(page.items, onAlbumClick)
                            ArtistSection.MVS -> videos(page.items, onVideoClick)
                        }
                        loadMoreFooter(page, onLoadMore)
                    }
                }
                item(key = "end-spacing") { Spacer(Modifier.height(8.dp)) }
            }
            ArtistImmersiveToolbar(
                title = profile?.name,
                onBack = onBack,
                listState = listState,
            )
        }
    }
}

private fun LazyListScope.songs(
    page: ArtistPageUiState.Content,
    playingMediaId: String?,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
) {
    val songs = page.items.mapNotNull { (it as? ArtistItem.Song)?.value }
    itemsIndexed(songs, key = { index, song -> "song-${song.hash}-$index" }) { _, song ->
        ResonoteMusicItem(
            title = song.title,
            supportingText = song.artist.orEmpty(),
            duration = song.durationMillis.durationLabel(),
            modifier = Modifier.padding(horizontal = 8.dp),
            qualityLabel = song.quality.label(),
            isVip = song.vip,
            isPlaying = song.hash == playingMediaId,
            artworkUrl = song.coverUrl,
            onClick = { onSongClick(song) },
            onMoreClick = onSongMoreClick?.let { callback -> { callback(song) } },
        )
    }
}

@Composable
private fun ArtistSectionActions(
    section: ArtistSection,
    sort: ArtistSort,
    content: ArtistPageUiState.Content?,
    onSelectSort: (ArtistSort) -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
) {
    val songs = content?.items.orEmpty().mapNotNull { (it as? ArtistItem.Song)?.value }
    ResonoteSectionHeader(
        title = section.label(),
        supportingText = content?.let {
            val total = it.total ?: it.items.size
            when (section) {
                ArtistSection.SONGS -> stringResource(R.string.feature_artist_impl_artist_song_count, total)
                ArtistSection.ALBUMS -> stringResource(R.string.feature_artist_impl_artist_album_count, total)
                ArtistSection.MVS -> ""
            }
        }.orEmpty(),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        trailingContent = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                ArtistSortAction(sort, onSelectSort)
                if (section == ArtistSection.SONGS && songs.isNotEmpty()) {
                    ResonotePlainAction(onClick = { onPlayAll(songs) }) {
                        Text(
                            text = stringResource(R.string.feature_artist_impl_artist_play_all),
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun ArtistSortAction(sort: ArtistSort, onSelectSort: (ArtistSort) -> Unit) {
    ResonotePlainAction(
        onClick = {
            onSelectSort(if (sort == ArtistSort.POPULAR) ArtistSort.LATEST else ArtistSort.POPULAR)
        },
        modifier = Modifier.testTag("artist-sort-toggle"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.SwapVert,
                contentDescription = stringResource(R.string.feature_artist_impl_artist_sort),
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = sort.label(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun LazyListScope.albums(items: List<ArtistItem>, onAlbumClick: (ArtistAlbum) -> Unit) {
    val albums = items.mapNotNull { (it as? ArtistItem.Album)?.value }
    itemsIndexed(albums.chunked(2), key = { index, _ -> "album-row-$index" }) { _, row ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            row.forEach { album ->
                ResonoteMediaCardItem(
                    metadata = ResonoteMediaCardMetadata(
                        title = album.name,
                        supportingText = album.publishDate.takeIf(String::isNotBlank) ?: album.artist,
                    ),
                    artworkContentDescription = stringResource(
                        R.string.feature_artist_impl_artist_album_artwork,
                        album.name,
                    ),
                    artworkUrl = album.coverUrl,
                    onClick = { onAlbumClick(album) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

private fun LazyListScope.videos(items: List<ArtistItem>, onVideoClick: (ArtistVideo) -> Unit) {
    val videos = items.mapNotNull { (it as? ArtistItem.Video)?.value }
    itemsIndexed(videos.chunked(2), key = { index, _ -> "video-row-$index" }) { _, row ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            row.forEach { video ->
                ResonoteVideoItem(
                    metadata = ResonoteVideoMetadata(
                        title = video.name,
                        supportingText = video.singer,
                        duration = video.durationMillis.durationLabel(),
                    ),
                    artworkUrl = video.coverUrl,
                    onClick = { onVideoClick(video) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

private fun LazyListScope.loadMoreFooter(page: ArtistPageUiState.Content, onLoadMore: () -> Unit) {
    if (page.isLoadingMore || page.loadMoreFailure != null) {
        item(key = "load-more") {
            ResonoteLoadMoreFooter(
                state = if (page.isLoadingMore) ResonoteLoadMoreState.LOADING else ResonoteLoadMoreState.ERROR,
                onRetry = onLoadMore,
            )
        }
    }
}

private fun LazyListScope.artistSkeleton(section: ArtistSection) {
    when (section) {
        ArtistSection.SONGS -> repeat(6) { index ->
            item(key = "loading-song-$index") {
                ResonoteMusicItem(
                    title = "",
                    supportingText = "",
                    duration = "",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    artworkState = ResonoteArtworkState.LOADING,
                    enabled = false,
                    onClick = {},
                    onMoreClick = null,
                )
            }
        }
        ArtistSection.ALBUMS,
        ArtistSection.MVS,
        -> repeat(3) { rowIndex ->
            item(key = "loading-card-$rowIndex") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(2) { cardIndex ->
                        if (section == ArtistSection.ALBUMS) {
                            ResonoteMediaCardItem(
                                metadata = ResonoteMediaCardMetadata(title = ""),
                                artworkContentDescription = "",
                                onClick = {},
                                modifier = Modifier.weight(1f),
                                artworkState = ResonoteArtworkState.LOADING,
                                enabled = false,
                            )
                        } else {
                            ResonoteVideoItem(
                                metadata = ResonoteVideoMetadata(title = ""),
                                onClick = {},
                                modifier = Modifier.weight(1f).testTag("loading-video-$rowIndex-$cardIndex"),
                                artworkState = ResonoteArtworkState.LOADING,
                                enabled = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistSection.label(): String = when (this) {
    ArtistSection.SONGS -> stringResource(R.string.feature_artist_impl_artist_songs)
    ArtistSection.ALBUMS -> stringResource(R.string.feature_artist_impl_artist_albums)
    ArtistSection.MVS -> stringResource(R.string.feature_artist_impl_artist_mvs)
}

@Composable
private fun ArtistSort.label(): String = when (this) {
    ArtistSort.POPULAR -> stringResource(R.string.feature_artist_impl_artist_popular)
    ArtistSort.LATEST -> stringResource(R.string.feature_artist_impl_artist_latest)
}

@Composable
private fun ArtistSection.emptyTitle(): String = when (this) {
    ArtistSection.SONGS -> stringResource(R.string.feature_artist_impl_artist_empty_songs)
    ArtistSection.ALBUMS -> stringResource(R.string.feature_artist_impl_artist_empty_albums)
    ArtistSection.MVS -> stringResource(R.string.feature_artist_impl_artist_empty_mvs)
}

@Composable
private fun ArtistSection.emptyBody(): String = when (this) {
    ArtistSection.SONGS -> stringResource(R.string.feature_artist_impl_artist_empty_songs_body)
    ArtistSection.ALBUMS -> stringResource(R.string.feature_artist_impl_artist_empty_albums_body)
    ArtistSection.MVS -> stringResource(R.string.feature_artist_impl_artist_empty_mvs_body)
}

@Composable
private fun ContentFailure.errorMessage(): String = when (this) {
    ContentFailure.Network -> stringResource(R.string.feature_artist_impl_artist_error_network)
    ContentFailure.AuthenticationRequired -> stringResource(R.string.feature_artist_impl_artist_error_auth)
    else -> stringResource(R.string.feature_artist_impl_artist_error_generic)
}
