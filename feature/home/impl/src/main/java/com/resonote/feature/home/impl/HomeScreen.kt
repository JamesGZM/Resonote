package com.resonote.feature.home.impl

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteArtwork
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteIconButton
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonotePlaylistItem
import com.resonote.core.designsystem.component.ResonotePlaylistMetadata
import com.resonote.core.designsystem.component.ResonoteRemoteArtwork
import com.resonote.core.designsystem.component.ResonoteTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeContentUiState,
    playingMediaId: String?,
    bottomContentPadding: Dp,
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = {
                    Image(
                        painter = painterResource(R.drawable.feature_home_impl_resonote_wordmark),
                        contentDescription = stringResource(R.string.feature_home_impl_brand),
                        modifier = Modifier
                            .width(124.dp)
                            .height(40.dp),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    )
                },
                actions = {
                    ResonoteIconButton(
                        label = stringResource(R.string.feature_home_impl_search),
                        onClick = onSearchClick,
                        icon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    )
                    ResonoteIconButton(
                        label = stringResource(R.string.feature_home_impl_recognize),
                        onClick = onRecognitionClick,
                        icon = { Icon(Icons.Rounded.Mic, contentDescription = null) },
                    )
                },
            )
        },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "recommendations") {
                RecommendationArea(
                    radio = state.radio,
                    onPlayRadio = onPlayRadio,
                    onOpenRankings = onOpenRankings,
                    onOpenFeaturedPlaylists = onOpenFeaturedPlaylists,
                )
            }
            item(key = "daily-header") {
                HomeSectionHeader(
                    title = stringResource(R.string.feature_home_impl_daily),
                    actionLabel = stringResource(R.string.feature_home_impl_play_all),
                    onAction = { onPlayAll(HomeSongCollection.DAILY_RECOMMENDATIONS) },
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
                HomeSectionHeader(
                    title = stringResource(R.string.feature_home_impl_playlists),
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
                HomeSectionHeader(
                    title = stringResource(R.string.feature_home_impl_new_releases),
                    actionLabel = stringResource(R.string.feature_home_impl_play_all),
                    onAction = { onPlayAll(HomeSongCollection.NEW_SONGS) },
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

@Composable
private fun RecommendationArea(
    radio: HomeSongUiModel?,
    onPlayRadio: () -> Unit,
    onOpenRankings: () -> Unit,
    onOpenFeaturedPlaylists: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (radio != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ResonoteArtwork(
                        state = if (radio.artworkUrl.isNullOrBlank()) {
                            ResonoteArtworkState.MISSING
                        } else {
                            ResonoteArtworkState.LOADED
                        },
                        contentDescription = radio.title,
                        modifier = Modifier.size(112.dp),
                    ) {
                        ResonoteRemoteArtwork(
                            model = radio.artworkUrl,
                            contentDescription = null,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            radio.title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            radio.artist,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Button(onClick = onPlayRadio) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.feature_home_impl_play_radio))
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RecommendationShortcut(
                title = stringResource(R.string.feature_home_impl_rankings),
                supporting = stringResource(R.string.feature_home_impl_popular_rankings),
                icon = Icons.Rounded.BarChart,
                onClick = onOpenRankings,
                modifier = Modifier.weight(1f),
            )
            RecommendationShortcut(
                title = stringResource(R.string.feature_home_impl_featured_playlists),
                supporting = stringResource(R.string.feature_home_impl_selected_for_you),
                icon = Icons.Rounded.LibraryMusic,
                onClick = onOpenFeaturedPlaylists,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RecommendationShortcut(
    title: String,
    supporting: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium,
            ) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        if (actionLabel != null) {
            Button(onClick = onAction, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun SongCollection(
    songs: List<HomeSongUiModel>,
    playingMediaId: String?,
    onSongClick: (HomeSongUiModel) -> Unit,
    onSongMoreClick: (HomeSongUiModel) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            songs.forEach { song ->
                ResonoteMusicItem(
                    title = song.title,
                    supportingText = song.artist,
                    duration = song.duration,
                    qualityLabel = song.qualityLabel,
                    isVip = song.isVip,
                    isPlaying = song.id == playingMediaId,
                    onClick = { onSongClick(song) },
                    onMoreClick = { onSongMoreClick(song) },
                    artworkState = if (song.artworkUrl.isNullOrBlank()) {
                        ResonoteArtworkState.MISSING
                    } else {
                        ResonoteArtworkState.LOADED
                    },
                    artworkUrl = song.artworkUrl,
                )
            }
        }
    }
}
