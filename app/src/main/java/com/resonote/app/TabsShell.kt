package com.resonote.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.resonote.core.designsystem.component.ResonoteNavigationSuiteScaffold
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.Album
import com.resonote.core.model.Ranking
import com.resonote.feature.discover.impl.DiscoverRoute
import com.resonote.feature.discover.impl.DiscoverSection
import com.resonote.feature.discover.impl.DiscoverViewModel
import com.resonote.feature.home.impl.HomeRoute
import com.resonote.feature.home.impl.HomeViewModel
import com.resonote.feature.library.impl.MyRoute
import com.resonote.feature.library.impl.MyViewModel
import com.resonote.feature.player.impl.MiniPlayerUiState
import com.resonote.feature.player.impl.ResonoteMiniPlayer
import kotlinx.coroutines.launch

internal enum class ResonoteTab(
    val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME(R.string.tab_home, Icons.Outlined.Home, Icons.Rounded.Home),
    DISCOVER(R.string.tab_discover, Icons.Outlined.Explore, Icons.Rounded.Explore),
    MY(R.string.tab_my, Icons.Outlined.AccountCircle, Icons.Rounded.AccountCircle),
}

@Composable
internal fun TabsShell(
    homeViewModel: HomeViewModel? = null,
    playbackState: PrototypePlaybackState = rememberPrototypePlaybackState(),
    onSearchClick: () -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    onAlbumClick: (Album) -> Unit = {},
    onRankingClick: (Ranking) -> Unit = {},
    discoverViewModel: DiscoverViewModel? = null,
    myViewModel: MyViewModel? = null,
    onLoginRequest: () -> Unit = {},
    onDailyVipClick: () -> Unit = {},
    onCloudClick: () -> Unit = {},
) {
    val tabsShellState = rememberTabsShellState()
    val selectedTab = tabsShellState.selectedTab
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val rootStateHolder = rememberSaveableStateHolder()
    var requestedDiscoverSection by remember { mutableStateOf<DiscoverSection?>(null) }
    val comingSoonMessage = stringResource(R.string.feature_coming_soon)

    fun showComingSoon() {
        scope.launch { snackbarHostState.showSnackbar(comingSoonMessage) }
    }

    fun openDiscover(section: DiscoverSection) {
        requestedDiscoverSection = section
        tabsShellState.selectTab(ResonoteTab.DISCOVER)
    }

    BackHandler(enabled = selectedTab != ResonoteTab.HOME) { tabsShellState.handleBack() }

    val currentSong = playbackState.currentSong
    ResonoteNavigationSuiteScaffold(
        navigationSuiteItems = {
            ResonoteTab.entries.forEach { tab ->
                item(
                    selected = selectedTab == tab,
                    onClick = { tabsShellState.selectTab(tab) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    selectedIcon = { Icon(tab.selectedIcon, contentDescription = null) },
                    label = { Text(stringResource(tab.labelRes)) },
                )
            }
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { outerPadding ->
            Box(Modifier.fillMaxSize().padding(outerPadding)) {
                rootStateHolder.SaveableStateProvider(selectedTab.name) {
                    when (selectedTab) {
                        ResonoteTab.HOME -> {
                            val bottomContentPadding = if (currentSong == null) 24.dp else 120.dp
                            val common: @Composable (HomeViewModel?) -> Unit = { suppliedViewModel ->
                                if (suppliedViewModel == null) {
                                    HomeRoute(
                                        playingMediaId = playbackState.currentSongId, bottomContentPadding = bottomContentPadding,
                                        onSearchClick = onSearchClick, onRecognitionClick = ::showComingSoon,
                                        onPlay = playbackState::play, onOpenRankings = { openDiscover(DiscoverSection.RANKINGS) },
                                        onOpenFeaturedPlaylists = ::showComingSoon,
                                        onSongMoreClick = { showComingSoon() },
                                        onPlaylistClick = { onPlaylistClick(it.id) },
                                    )
                                } else {
                                    HomeRoute(
                                        playingMediaId = playbackState.currentSongId, bottomContentPadding = bottomContentPadding,
                                        onSearchClick = onSearchClick, onRecognitionClick = ::showComingSoon,
                                        onPlay = playbackState::play, onOpenRankings = { openDiscover(DiscoverSection.RANKINGS) },
                                        onOpenFeaturedPlaylists = ::showComingSoon,
                                        onSongMoreClick = { showComingSoon() },
                                        onPlaylistClick = { onPlaylistClick(it.id) },
                                        viewModel = suppliedViewModel,
                                    )
                                }
                            }
                            common(homeViewModel)
                        }
                        ResonoteTab.DISCOVER -> {
                            val bottomContentPadding = if (currentSong == null) 24.dp else 120.dp
                            val actualViewModel = discoverViewModel ?: hiltViewModel()
                            DiscoverRoute(
                                bottomContentPadding = bottomContentPadding,
                                playingMediaId = playbackState.currentSongId,
                                requestedSection = requestedDiscoverSection,
                                onRequestedSectionConsumed = { requestedDiscoverSection = null },
                                onPlaylistClick = { onPlaylistClick(it.id) },
                                onRankingClick = onRankingClick,
                                onAlbumClick = onAlbumClick,
                                onPlaySongs = playbackState::playAll,
                                onSongClick = playbackState::play,
                                onSongMoreClick = { showComingSoon() },
                                viewModel = actualViewModel,
                            )
                        }
                        ResonoteTab.MY -> {
                            val bottomContentPadding = if (currentSong == null) 24.dp else 120.dp
                            val actualViewModel = myViewModel ?: hiltViewModel()
                            MyRoute(
                                bottomContentPadding = bottomContentPadding,
                                onLoginClick = onLoginRequest,
                                onDailyVipClick = onDailyVipClick,
                                onCloudClick = onCloudClick,
                                onPlaylistClick = onPlaylistClick,
                                viewModel = actualViewModel,
                            )
                        }
                    }
                }

                currentSong?.let { song ->
                    ResonoteMiniPlayer(
                        state = MiniPlayerUiState(
                            song.hash, song.title, song.artist.orEmpty(), song.quality.toLabel(), song.vip,
                            playbackState.isPlaying, playbackState.progress, PrototypeArtworkColors,
                        ),
                        onOpenPlayer = ::showComingSoon,
                        onTogglePlay = playbackState::togglePlay,
                        onNext = playbackState::playNext,
                        onOpenQueue = ::showComingSoon,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                }
            }
        }
    }
}

private val PrototypeArtworkColors = listOf(
    androidx.compose.ui.graphics.Color(0xFF5A061B),
    androidx.compose.ui.graphics.Color(0xFFE31353),
    androidx.compose.ui.graphics.Color(0xFFFF8DA9),
)

private fun AudioQuality.toLabel(): String? = when (this) {
    AudioQuality.Standard -> null
    AudioQuality.HighQuality -> "HQ"
    AudioQuality.HighResolution -> "HI-RES"
    AudioQuality.Lossless -> "LOSSLESS"
}
