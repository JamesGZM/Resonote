package com.resonote.app

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.resonote.core.playback.PlaybackState
import com.resonote.feature.discover.impl.DiscoverRoute
import com.resonote.feature.discover.impl.DiscoverSection
import com.resonote.feature.discover.impl.DiscoverViewModel
import com.resonote.feature.home.impl.HomeRoute
import com.resonote.feature.home.impl.HomeViewModel
import com.resonote.feature.library.impl.MyRoute
import com.resonote.feature.library.impl.MyViewModel

internal enum class ResonoteTab(val labelRes: Int, @field:DrawableRes val iconRes: Int) {
    HOME(R.string.tab_home, R.drawable.ic_tab_home),
    DISCOVER(R.string.tab_discover, R.drawable.ic_tab_discover),
    MY(R.string.tab_my, R.drawable.ic_tab_my),
}

internal data class PlaylistDestinationSeed(val id: String, val title: String, val coverUrl: String?)

@Composable
internal fun TabsShell(
    tabsShellState: TabsShellState,
    isActiveDestination: Boolean = true,
    homeViewModel: HomeViewModel? = null,
    playbackState: PlaybackState = PlaybackState(),
    onPlaySong: (OnlineSong) -> Unit = {},
    onPlaySongs: (List<OnlineSong>, Int) -> Unit = { _, _ -> },
    onSearchClick: () -> Unit = {},
    onRecognitionClick: () -> Unit = {},
    onSongMoreClick: (OnlineSong) -> Unit = {},
    onPlaylistClick: (PlaylistDestinationSeed) -> Unit = {},
    onUserPlaylistClick: (UserPlaylist) -> Unit = {},
    onAlbumClick: (Album) -> Unit = {},
    onRankingClick: (Ranking) -> Unit = {},
    discoverViewModel: DiscoverViewModel? = null,
    myViewModel: MyViewModel? = null,
    onLoginRequest: () -> Unit = {},
    onDailyVipClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onCloudClick: () -> Unit = {},
    onLocalMusicClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onBottomBarInsetChanged: (Dp) -> Unit = {},
) {
    val selectedTab = tabsShellState.selectedTab
    val rootStateHolder = rememberSaveableStateHolder()
    var requestedDiscoverSection by remember { mutableStateOf<DiscoverSection?>(null) }

    fun openDiscover(section: DiscoverSection) {
        requestedDiscoverSection = section
        tabsShellState.selectTab(ResonoteTab.DISCOVER)
    }

    BackHandler(enabled = tabsShellState.canHandleBack(isActiveDestination)) {
        tabsShellState.handleBack()
    }

    val currentMedia = playbackState.currentMetadata
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
        modifier = Modifier.fillMaxSize(),
        onBottomBarInsetChanged = onBottomBarInsetChanged,
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { outerPadding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(outerPadding),
            ) {
                rootStateHolder.SaveableStateProvider(selectedTab.name) {
                    when (selectedTab) {
                        ResonoteTab.HOME -> {
                            val bottomContentPadding = if (currentMedia == null) 24.dp else 120.dp
                            val common: @Composable (HomeViewModel?) -> Unit = { suppliedViewModel ->
                                if (suppliedViewModel == null) {
                                    HomeRoute(
                                        playingMediaId = playbackState.currentMetadata?.mediaId,
                                        bottomContentPadding = bottomContentPadding,
                                        onSearchClick = onSearchClick,
                                        onRecognitionClick = onRecognitionClick,
                                        onPlay = { onPlaySongs(it.songs, it.startIndex) },
                                        onOpenRankings = { openDiscover(DiscoverSection.RANKINGS) },
                                        onOpenFeaturedPlaylists = { openDiscover(DiscoverSection.PLAYLISTS) },
                                        onSongMoreClick = onSongMoreClick,
                                        onPlaylistClick = {
                                            onPlaylistClick(PlaylistDestinationSeed(it.id, it.title, it.artworkUrl))
                                        },
                                    )
                                } else {
                                    HomeRoute(
                                        playingMediaId = playbackState.currentMetadata?.mediaId,
                                        bottomContentPadding = bottomContentPadding,
                                        onSearchClick = onSearchClick,
                                        onRecognitionClick = onRecognitionClick,
                                        onPlay = { onPlaySongs(it.songs, it.startIndex) },
                                        onOpenRankings = { openDiscover(DiscoverSection.RANKINGS) },
                                        onOpenFeaturedPlaylists = { openDiscover(DiscoverSection.PLAYLISTS) },
                                        onSongMoreClick = onSongMoreClick,
                                        onPlaylistClick = {
                                            onPlaylistClick(PlaylistDestinationSeed(it.id, it.title, it.artworkUrl))
                                        },
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
                                onPlaylistClick = {
                                    onPlaylistClick(PlaylistDestinationSeed(it.id, it.title, it.coverUrl))
                                },
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
                                onFollowingClick = onFollowingClick,
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
            }
        }
    }
}
