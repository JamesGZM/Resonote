@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.album.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteLoadMoreEffect
import com.resonote.core.designsystem.component.ResonoteLoadMoreFooter
import com.resonote.core.designsystem.component.ResonoteLoadMoreState
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonotePullToRefreshBox
import com.resonote.core.model.OnlineSong

@Composable
internal fun AlbumContentLayout(
    state: AlbumUiState.Content?,
    metadata: AlbumMetadata,
    playingMediaId: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    bottomContentPadding: Dp,
) {
    val listState = remember(metadata.id) { LazyListState() }
    val collapseProgress = rememberCollapseProgress(listState)
    ResonoteLoadMoreEffect(
        listState = listState,
        itemCount = state?.songs?.size ?: 0,
        enabled = state != null &&
            state.hasMore &&
            !state.isLoadingMore &&
            !state.isRefreshing &&
            state.loadMoreFailure == null,
        onLoadMore = onLoadMore,
    )
    ResonotePullToRefreshBox(
        isRefreshing = state?.isRefreshing == true,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize().testTag("album-pull-to-refresh"),
    ) {
        Box(Modifier.fillMaxSize()) {
            AlbumContent(
                state = state,
                metadata = metadata,
                listState = listState,
                playingMediaId = playingMediaId,
                onLoadMore = onLoadMore,
                onPlayAll = onPlayAll,
                onSongClick = onSongClick,
                onSongMoreClick = onSongMoreClick,
                bottomContentPadding = bottomContentPadding,
            )
            ImmersiveToolbar(
                title = metadata.title,
                onBack = onBack,
                collapseProgress = collapseProgress,
            )
        }
    }
}

@Composable
private fun AlbumContent(
    state: AlbumUiState.Content?,
    metadata: AlbumMetadata,
    listState: LazyListState,
    playingMediaId: String?,
    onLoadMore: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    bottomContentPadding: Dp,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag(if (state == null) "album-skeleton" else "album-list"),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
    ) {
        item(key = "header") {
            if (metadata.id.isBlank()) {
                AlbumLoadingHeader()
            } else {
                AlbumHeader(
                    metadata = metadata,
                    loadedSongCount = state?.songs?.size ?: 0,
                    onPlayAll = { state?.songs?.let(onPlayAll) },
                    canPlay = state?.songs?.isNotEmpty() == true,
                )
            }
        }
        item(key = "list-top-spacing") { Spacer(Modifier.height(12.dp)) }
        if (state == null) {
            repeat(6) { index ->
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
        } else {
            itemsIndexed(state.songs, key = { index, song -> "song-${song.hash}-$index" }) { _, song ->
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
        if (state?.isLoadingMore == true || state?.loadMoreFailure != null) {
            item(key = "load-more") {
                ResonoteLoadMoreFooter(
                    state = if (state.isLoadingMore) {
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
