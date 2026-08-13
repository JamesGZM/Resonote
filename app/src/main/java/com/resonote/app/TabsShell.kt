package com.resonote.app

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.resonote.core.designsystem.component.ResonoteNavigationSuiteScaffold
import com.resonote.core.model.Album
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.Ranking
import com.resonote.core.model.UserPlaylist
import com.resonote.core.playback.PlaybackMode
import com.resonote.core.playback.PlaybackState
import com.resonote.feature.discover.impl.DiscoverRoute
import com.resonote.feature.discover.impl.DiscoverSection
import com.resonote.feature.discover.impl.DiscoverViewModel
import com.resonote.feature.home.impl.HomeRoute
import com.resonote.feature.home.impl.HomeViewModel
import com.resonote.feature.library.impl.MyRoute
import com.resonote.feature.library.impl.MyViewModel
import com.resonote.feature.player.impl.MiniPlayerUiState
import com.resonote.feature.player.impl.PlaybackQueueSheet
import com.resonote.feature.player.impl.ResonoteMiniPlayer
import com.resonote.feature.player.impl.badgeLabel

internal enum class ResonoteTab(
    val labelRes: Int,
    @field:DrawableRes val iconRes: Int,
) {
    HOME(R.string.tab_home, R.drawable.ic_tab_home),
    DISCOVER(R.string.tab_discover, R.drawable.ic_tab_discover),
    MY(R.string.tab_my, R.drawable.ic_tab_my),
}

@Composable
internal fun TabsShell(
    homeViewModel: HomeViewModel? = null,
    playbackState: PlaybackState = PlaybackState(),
    onPlaySong: (OnlineSong) -> Unit = {},
    onPlaySongs: (List<OnlineSong>, Int) -> Unit = { _, _ -> },
    onTogglePlay: () -> Unit = {},
    onNext: () -> Unit = {},
    onOpenPlayer: () -> Unit = {},
    onSelectQueueItem: (Int) -> Unit = {},
    onRemoveQueueItem: (Int) -> Unit = {},
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onClearQueue: () -> Unit = {},
    onModeChange: (PlaybackMode) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onRecognitionClick: () -> Unit = {},
    onSongMoreClick: (OnlineSong) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    onUserPlaylistClick: (UserPlaylist) -> Unit = {},
    onAlbumClick: (Album) -> Unit = {},
    onRankingClick: (Ranking) -> Unit = {},
    discoverViewModel: DiscoverViewModel? = null,
    myViewModel: MyViewModel? = null,
    onLoginRequest: () -> Unit = {},
    onDailyVipClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onCloudClick: () -> Unit = {},
    onLocalMusicClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSnackbarBottomInsetChanged: (Dp) -> Unit = {},
) {
    val tabsShellState = rememberTabsShellState()
    val selectedTab = tabsShellState.selectedTab
    val rootStateHolder = rememberSaveableStateHolder()
    var requestedDiscoverSection by remember { mutableStateOf<DiscoverSection?>(null) }
    var queueOpen by remember { mutableStateOf(false) }
    var shellBottomPx by remember { mutableStateOf(0f) }
    var contentBottomPx by remember { mutableStateOf(0f) }
    var miniPlayerTopPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    fun openDiscover(section: DiscoverSection) {
        requestedDiscoverSection = section
        tabsShellState.selectTab(ResonoteTab.DISCOVER)
    }

    BackHandler(enabled = selectedTab != ResonoteTab.HOME) { tabsShellState.handleBack() }

    val currentMedia = playbackState.currentMetadata
    val snackbarAnchorPx = if (currentMedia == null) contentBottomPx else miniPlayerTopPx
    val snackbarBottomInset = if (shellBottomPx == 0f || snackbarAnchorPx == 0f) {
        0.dp
    } else {
        with(density) {
            val safeDrawingBottomPx = WindowInsets.safeDrawing.getBottom(this)
            (shellBottomPx - snackbarAnchorPx - safeDrawingBottomPx)
                .coerceAtLeast(0f)
                .toDp()
        }
    }
    LaunchedEffect(snackbarBottomInset) {
        onSnackbarBottomInsetChanged(snackbarBottomInset)
    }
    ResonoteNavigationSuiteScaffold(
        navigationSuiteItems = {
            ResonoteTab.entries.forEach { tab ->
                item(
                    selected = selectedTab == tab,
                    onClick = { tabsShellState.selectTab(tab) },
                    modifier = Modifier.testTag("resonote-tab-${tab.name.lowercase()}"),
                    icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                    label = { Text(stringResource(tab.labelRes)) },
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { shellBottomPx = it.boundsInRoot().bottom },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { outerPadding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(outerPadding)
                    .onGloballyPositioned { contentBottomPx = it.boundsInRoot().bottom },
            ) {
                rootStateHolder.SaveableStateProvider(selectedTab.name) {
                    when (selectedTab) {
                        ResonoteTab.HOME -> {
                            val bottomContentPadding = if (currentMedia == null) 24.dp else 120.dp
                            val common: @Composable (HomeViewModel?) -> Unit = { suppliedViewModel ->
                                if (suppliedViewModel == null) {
                                    HomeRoute(
                                        playingMediaId = playbackState.currentMetadata?.mediaId, bottomContentPadding = bottomContentPadding,
                                        onSearchClick = onSearchClick, onRecognitionClick = onRecognitionClick,
                                        onPlay = { onPlaySongs(it.songs, it.startIndex) },
                                        onOpenRankings = { openDiscover(DiscoverSection.RANKINGS) },
                                        onOpenFeaturedPlaylists = { openDiscover(DiscoverSection.PLAYLISTS) },
                                        onSongMoreClick = onSongMoreClick,
                                        onPlaylistClick = { onPlaylistClick(it.id) },
                                    )
                                } else {
                                    HomeRoute(
                                        playingMediaId = playbackState.currentMetadata?.mediaId, bottomContentPadding = bottomContentPadding,
                                        onSearchClick = onSearchClick, onRecognitionClick = onRecognitionClick,
                                        onPlay = { onPlaySongs(it.songs, it.startIndex) },
                                        onOpenRankings = { openDiscover(DiscoverSection.RANKINGS) },
                                        onOpenFeaturedPlaylists = { openDiscover(DiscoverSection.PLAYLISTS) },
                                        onSongMoreClick = onSongMoreClick,
                                        onPlaylistClick = { onPlaylistClick(it.id) },
                                        viewModel = suppliedViewModel,
                                    )
                                }
                            }
                            common(homeViewModel)
                        }
                        ResonoteTab.DISCOVER -> {
                            val bottomContentPadding = if (currentMedia == null) 24.dp else 120.dp
                            val actualViewModel = discoverViewModel ?: hiltViewModel()
                            DiscoverRoute(
                                bottomContentPadding = bottomContentPadding,
                                playingMediaId = playbackState.currentMetadata?.mediaId,
                                requestedSection = requestedDiscoverSection,
                                onRequestedSectionConsumed = { requestedDiscoverSection = null },
                                onPlaylistClick = { onPlaylistClick(it.id) },
                                onRankingClick = onRankingClick,
                                onAlbumClick = onAlbumClick,
                                onPlaySongs = { onPlaySongs(it, 0) },
                                onSongClick = onPlaySong,
                                onSongMoreClick = onSongMoreClick,
                                viewModel = actualViewModel,
                            )
                        }
                        ResonoteTab.MY -> {
                            val bottomContentPadding = if (currentMedia == null) 24.dp else 120.dp
                            val actualViewModel = myViewModel ?: hiltViewModel()
                            MyRoute(
                                bottomContentPadding = bottomContentPadding,
                                onLoginClick = onLoginRequest,
                                onDailyVipClick = onDailyVipClick,
                                onHistoryClick = onHistoryClick,
                                onCloudClick = onCloudClick,
                                onLocalMusicClick = onLocalMusicClick,
                                onSettingsClick = onSettingsClick,
                                onPlaylistClick = onUserPlaylistClick,
                                viewModel = actualViewModel,
                            )
                        }
                    }
                }

                currentMedia?.let { song ->
                    ResonoteMiniPlayer(
                        state = MiniPlayerUiState(
                            song.mediaId, song.title, song.artist.orEmpty(), song.format.badgeLabel(), song.isVip,
                            playbackState.isPlaying, playbackState.progress, PrototypeArtworkColors, song.artworkUri,
                        ),
                        onOpenPlayer = onOpenPlayer,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext,
                        onOpenQueue = { queueOpen = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .testTag("resonote-mini-player")
                            .onGloballyPositioned { miniPlayerTopPx = it.boundsInRoot().top }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                }
            }
        }
    }

    if (queueOpen) {
        PlaybackQueueSheet(
            playback = playbackState,
            onDismiss = { queueOpen = false },
            onSelect = onSelectQueueItem,
            onRemove = onRemoveQueueItem,
            onMove = onMoveQueueItem,
            onClear = onClearQueue,
            onModeChange = onModeChange,
        )
    }
}

private val PrototypeArtworkColors = listOf(
    androidx.compose.ui.graphics.Color(0xFF5A061B),
    androidx.compose.ui.graphics.Color(0xFFE31353),
    androidx.compose.ui.graphics.Color(0xFFFF8DA9),
)
