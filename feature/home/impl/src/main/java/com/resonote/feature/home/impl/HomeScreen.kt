package com.resonote.feature.home.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonotePlaylistItem
import com.resonote.core.designsystem.component.ResonotePlaylistMetadata
import com.resonote.core.designsystem.component.ResonotePullToRefreshBox
import com.resonote.core.designsystem.component.ResonoteSectionHeader

@Composable
fun HomeScreen(
    state: HomeContentUiState,
    isRefreshing: Boolean,
    playingMediaId: String?,
    bottomContentPadding: Dp,
    onRefresh: () -> Unit,
    onSearchClick: () -> Unit,
    onRecognitionClick: () -> Unit,
    onPlayRadio: () -> Unit,
    onOpenRankings: () -> Unit,
    onOpenFeaturedPlaylists: () -> Unit,
    onSongClick: (HomeSongCollection, HomeSongUiModel) -> Unit,
    onSongMoreClick: (HomeSongUiModel) -> Unit,
    onPlayAll: (HomeSongCollection) -> Unit,
    onPlaylistClick: (HomePlaylistUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    ResonotePullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .testTag("home-pull-to-refresh"),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { HomeTopBar(onSearchClick, onRecognitionClick) },
        ) { scaffoldPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home-list")
                    .padding(top = scaffoldPadding.calculateTopPadding()),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = bottomContentPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "recommendations") {
                    RecommendationArea(
                        onPlayRadio = onPlayRadio,
                        onOpenRankings = onOpenRankings,
                        onOpenFeaturedPlaylists = onOpenFeaturedPlaylists,
                    )
                }
                item(key = "daily-header") {
                    ResonoteSectionHeader(
                        title = stringResource(R.string.feature_home_impl_daily),
                        supportingText = stringResource(R.string.feature_home_impl_daily_subtitle),
                        trailingContent = {
                            HomePlayAllButton(
                                onClick = { onPlayAll(HomeSongCollection.DAILY_RECOMMENDATIONS) },
                            )
                        },
                    )
                }
                item(key = "daily-songs") {
                    SongCollection(
                        songs = state.dailySongs,
                        playingMediaId = playingMediaId,
                        onSongClick = { onSongClick(HomeSongCollection.DAILY_RECOMMENDATIONS, it) },
                        onSongMoreClick = onSongMoreClick,
                    )
                }
                item(key = "playlist-header") {
                    ResonoteSectionHeader(
                        title = stringResource(R.string.feature_home_impl_playlists),
                        supportingText = stringResource(R.string.feature_home_impl_playlists_subtitle),
                        modifier = Modifier.testTag("home-playlists-header"),
                    )
                }
                itemsIndexed(
                    items = state.recommendedPlaylists.chunked(2),
                    key = { index, pair -> "${pair.joinToString { it.id }}-$index" },
                ) { _, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        pair.forEach { playlist ->
                            ResonotePlaylistItem(
                                metadata = ResonotePlaylistMetadata(playlist.title, playlist.playCount),
                                onClick = { onPlaylistClick(playlist) },
                                modifier = Modifier.weight(1f),
                                artworkState = if (playlist.artworkUrl.isNullOrBlank()) {
                                    ResonoteArtworkState.MISSING
                                } else {
                                    ResonoteArtworkState.LOADED
                                },
                                artworkUrl = playlist.artworkUrl,
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                item(key = "new-header") {
                    ResonoteSectionHeader(
                        title = stringResource(R.string.feature_home_impl_new_releases),
                        supportingText = stringResource(R.string.feature_home_impl_new_releases_subtitle),
                        trailingContent = {
                            HomePlayAllButton(
                                onClick = { onPlayAll(HomeSongCollection.NEW_SONGS) },
                            )
                        },
                        modifier = Modifier.testTag("home-new-releases-header"),
                    )
                }
                item(key = "new-songs") {
                    SongCollection(
                        songs = state.newSongs,
                        playingMediaId = playingMediaId,
                        onSongClick = { onSongClick(HomeSongCollection.NEW_SONGS, it) },
                        onSongMoreClick = onSongMoreClick,
                    )
                }
            }
        }
    }
}
