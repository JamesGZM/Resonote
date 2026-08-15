package com.resonote.feature.home.impl

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteCompactFilledIconButton
import com.resonote.core.designsystem.component.ResonoteIconButton
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonotePlainAction
import com.resonote.core.designsystem.component.ResonotePlaylistItem
import com.resonote.core.designsystem.component.ResonotePlaylistMetadata
import com.resonote.core.designsystem.component.ResonotePullToRefreshIndicator
import com.resonote.core.designsystem.component.ResonoteSectionHeader
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.designsystem.tokens.ResonoteTokens

@OptIn(ExperimentalMaterial3Api::class)
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
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .testTag("home-pull-to-refresh"),
        state = pullToRefreshState,
        indicator = {
            ResonotePullToRefreshIndicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
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

@Composable
private fun RecommendationArea(
    onPlayRadio: () -> Unit,
    onOpenRankings: () -> Unit,
    onOpenFeaturedPlaylists: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
    ) {
        RecommendationShortcut(
            title = stringResource(R.string.feature_home_impl_radio),
            supporting = stringResource(R.string.feature_home_impl_radio_supporting),
            iconRes = R.drawable.home_radio_waveform,
            iconWidth = 54.dp,
            iconHeight = 48.dp,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            actionLabel = stringResource(R.string.feature_home_impl_play_radio),
            onAction = onPlayRadio,
            modifier = Modifier.weight(1f),
        )
        RecommendationShortcut(
            title = stringResource(R.string.feature_home_impl_rankings),
            supporting = stringResource(R.string.feature_home_impl_popular_rankings),
            iconRes = R.drawable.home_ranking_bars,
            iconWidth = 64.dp,
            iconHeight = 43.dp,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            onClick = onOpenRankings,
            modifier = Modifier
                .weight(1f)
                .testTag("home-shortcut-rankings"),
        )
        RecommendationShortcut(
            title = stringResource(R.string.feature_home_impl_featured_playlists),
            supporting = stringResource(R.string.feature_home_impl_selected_for_you),
            iconRes = R.drawable.home_playlist_disc,
            iconWidth = 47.dp,
            iconHeight = 50.dp,
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            onClick = onOpenFeaturedPlaylists,
            modifier = Modifier
                .weight(1f)
                .testTag("home-shortcut-featured-playlists"),
        )
    }
}

@Composable
private fun RecommendationShortcut(
    title: String,
    supporting: String,
    iconRes: Int,
    iconWidth: Dp,
    iconHeight: Dp,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val cardContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            lerp(containerColor, Color.White, 0.08f),
                            containerColor,
                            lerp(containerColor, Color.Black, 0.06f),
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(
                        start = ResonoteTokens.spacing.space3,
                        top = ResonoteTokens.spacing.space3,
                        end = ResonoteTokens.spacing.space3,
                        bottom = ResonoteTokens.spacing.space2,
                    ),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space1)) {
                    Text(
                        title,
                        color = contentColor,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        supporting,
                        color = contentColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .align(if (actionLabel == null) Alignment.BottomCenter else Alignment.BottomStart)
                            .width(iconWidth)
                            .height(iconHeight),
                        colorFilter = ColorFilter.tint(contentColor),
                    )
                }
            }
            if (actionLabel != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(ResonoteTokens.touchTargets.minimum),
                ) {
                    ResonoteCompactFilledIconButton(
                        label = actionLabel,
                        onClick = onAction,
                        modifier = Modifier.fillMaxSize(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = contentColor,
                            contentColor = containerColor,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }

    val cardModifier = modifier.aspectRatio(1f)
    if (onClick == null) {
        Surface(
            modifier = cardModifier,
            color = Color.Transparent,
            contentColor = contentColor,
            shape = MaterialTheme.shapes.large,
            content = cardContent,
        )
    } else {
        Surface(
            onClick = onClick,
            modifier = cardModifier,
            color = Color.Transparent,
            contentColor = contentColor,
            shape = MaterialTheme.shapes.large,
            content = cardContent,
        )
    }
}

@Composable
private fun HomePlayAllButton(onClick: () -> Unit) {
    ResonotePlainAction(onClick = onClick) {
        Text(
            text = stringResource(R.string.feature_home_impl_play_all),
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
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
        shape = MaterialTheme.shapes.large,
    ) {
        Column {
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
