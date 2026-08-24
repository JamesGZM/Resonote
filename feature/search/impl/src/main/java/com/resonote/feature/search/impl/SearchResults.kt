package com.resonote.feature.search.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonote.core.model.ComplexSearchResult
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.SearchAlbum
import com.resonote.core.model.SearchArtist
import com.resonote.core.model.SearchMv
import com.resonote.core.model.SearchPlaylist

@Composable
internal fun SearchResults(
    result: SearchContentUiState,
    category: SearchCategory,
    playingMediaId: String?,
    onSelectCategory: (SearchCategory) -> Unit,
    onLoadMore: () -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    onPlaylistClick: ((SearchPlaylist) -> Unit)?,
    onAlbumClick: ((SearchAlbum) -> Unit)?,
    onArtistClick: ((SearchArtist) -> Unit)?,
    onMvClick: ((SearchMv) -> Unit)?,
    bottomContentPadding: Dp,
) {
    when (result) {
        is SearchContentUiState.Aggregate -> AggregateSearchResults(
            result = result.value,
            playingMediaId = playingMediaId,
            onSelectCategory = onSelectCategory,
            onSongClick = onSongClick,
            onSongMoreClick = onSongMoreClick,
            onPlaylistClick = onPlaylistClick,
            onAlbumClick = onAlbumClick,
            onArtistClick = onArtistClick,
            onMvClick = onMvClick,
            bottomContentPadding = bottomContentPadding,
        )
        is SearchContentUiState.Page -> PagedSearchResults(
            page = result,
            category = category,
            playingMediaId = playingMediaId,
            onLoadMore = onLoadMore,
            onSongClick = onSongClick,
            onSongMoreClick = onSongMoreClick,
            onPlaylistClick = onPlaylistClick,
            onAlbumClick = onAlbumClick,
            onArtistClick = onArtistClick,
            onMvClick = onMvClick,
            bottomContentPadding = bottomContentPadding,
        )
    }
}

@Composable
private fun AggregateSearchResults(
    result: ComplexSearchResult,
    playingMediaId: String?,
    onSelectCategory: (SearchCategory) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    onPlaylistClick: ((SearchPlaylist) -> Unit)?,
    onAlbumClick: ((SearchAlbum) -> Unit)?,
    onArtistClick: ((SearchArtist) -> Unit)?,
    onMvClick: ((SearchMv) -> Unit)?,
    bottomContentPadding: Dp,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("search-aggregate"),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (result.artists.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchResultSectionHeader(
                        title = stringResource(R.string.feature_search_impl_search_artists),
                        onClick = { onSelectCategory(SearchCategory.ARTISTS) },
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(
                            result.artists,
                            key = { index, artist -> "artist-${artist.id}-$index" },
                        ) { _, artist ->
                            SearchArtistItem(
                                artist = artist,
                                modifier = Modifier.width(104.dp),
                                onClick = onArtistClick?.let { callback -> { callback(artist) } },
                            )
                        }
                    }
                }
            }
        }
        if (result.songs.isNotEmpty()) {
            item {
                SearchResultSectionHeader(
                    title = stringResource(R.string.feature_search_impl_search_songs),
                    total = result.songsTotal,
                    onClick = { onSelectCategory(SearchCategory.SONGS) },
                )
            }
            itemsIndexed(result.songs, key = { index, song -> "song-${song.hash}-$index" }) { _, song ->
                SearchSongItem(
                    song = song,
                    playingMediaId = playingMediaId,
                    onClick = { onSongClick(song) },
                    onMoreClick = onSongMoreClick?.let { callback -> { callback(song) } },
                )
            }
        }
        if (result.playlists.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SearchResultSectionHeader(
                        title = stringResource(R.string.feature_search_impl_search_playlists),
                        total = result.playlistsTotal,
                        onClick = { onSelectCategory(SearchCategory.PLAYLISTS) },
                    )
                    result.playlists.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { playlist ->
                                SearchPlaylistItem(
                                    playlist = playlist,
                                    modifier = Modifier.weight(1f),
                                    onClick = onPlaylistClick?.let { callback -> { callback(playlist) } },
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        if (result.albums.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SearchResultSectionHeader(
                        title = stringResource(R.string.feature_search_impl_search_albums),
                        total = result.albumsTotal,
                        onClick = { onSelectCategory(SearchCategory.ALBUMS) },
                    )
                    result.albums.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { album ->
                                SearchAlbumItem(
                                    album = album,
                                    modifier = Modifier.weight(1f),
                                    onClick = onAlbumClick?.let { callback -> { callback(album) } },
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        if (result.mvs.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SearchResultSectionHeader(
                        title = stringResource(R.string.feature_search_impl_search_mvs),
                        total = result.mvsTotal,
                        onClick = { onSelectCategory(SearchCategory.MVS) },
                    )
                    result.mvs.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { mv ->
                                SearchMvItem(
                                    mv = mv,
                                    modifier = Modifier.weight(1f),
                                    onClick = onMvClick?.let { callback -> { callback(mv) } },
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PagedSearchResults(
    page: SearchContentUiState.Page,
    category: SearchCategory,
    playingMediaId: String?,
    onLoadMore: () -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    onPlaylistClick: ((SearchPlaylist) -> Unit)?,
    onAlbumClick: ((SearchAlbum) -> Unit)?,
    onArtistClick: ((SearchArtist) -> Unit)?,
    onMvClick: ((SearchMv) -> Unit)?,
    bottomContentPadding: Dp,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomContentPadding),
    ) {
        when (category) {
            SearchCategory.ALL -> Unit
            SearchCategory.SONGS -> {
                val songs = page.items.filterIsInstance<SearchResultItem.Song>()
                itemsIndexed(songs, key = { index, item -> "song-${item.stableId}-$index" }) { _, item ->
                    SearchSongItem(
                        song = item.value,
                        playingMediaId = playingMediaId,
                        onClick = { onSongClick(item.value) },
                        onMoreClick = onSongMoreClick?.let { callback -> { callback(item.value) } },
                    )
                }
            }
            SearchCategory.PLAYLISTS -> {
                val rows = page.items.filterIsInstance<SearchResultItem.Playlist>().chunked(2)
                itemsIndexed(rows, key = { index, row -> "playlist-${row.first().stableId}-$index" }) { _, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { item ->
                            SearchPlaylistItem(
                                playlist = item.value,
                                modifier = Modifier.weight(1f),
                                onClick = onPlaylistClick?.let { callback -> { callback(item.value) } },
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            SearchCategory.ALBUMS -> {
                val rows = page.items.filterIsInstance<SearchResultItem.Album>().chunked(2)
                itemsIndexed(rows, key = { index, row -> "album-${row.first().stableId}-$index" }) { _, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { item ->
                            SearchAlbumItem(
                                album = item.value,
                                modifier = Modifier.weight(1f),
                                onClick = onAlbumClick?.let { callback -> { callback(item.value) } },
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            SearchCategory.MVS -> {
                val rows = page.items.filterIsInstance<SearchResultItem.Mv>().chunked(2)
                itemsIndexed(rows, key = { index, row -> "mv-${row.first().stableId}-$index" }) { _, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { item ->
                            SearchMvItem(
                                mv = item.value,
                                modifier = Modifier.weight(1f),
                                onClick = onMvClick?.let { callback -> { callback(item.value) } },
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            SearchCategory.ARTISTS -> {
                val rows = page.items.filterIsInstance<SearchResultItem.Artist>().chunked(3)
                itemsIndexed(rows, key = { index, row -> "artist-${row.first().stableId}-$index" }) { _, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { item ->
                            SearchArtistItem(
                                artist = item.value,
                                modifier = Modifier.weight(1f),
                                onClick = onArtistClick?.let { callback -> { callback(item.value) } },
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        if (page.hasMore || page.isLoadingMore || page.loadMoreFailure != null) {
            item(key = "load-more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        page.isLoadingMore -> CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        page.loadMoreFailure != null -> TextButton(onClick = onLoadMore) {
                            Text(stringResource(R.string.feature_search_impl_search_load_more_retry))
                        }
                        page.hasMore -> TextButton(onClick = onLoadMore) {
                            Text(stringResource(R.string.feature_search_impl_search_load_more))
                        }
                    }
                }
            }
        }
    }
}
