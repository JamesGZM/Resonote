package com.resonote.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.resonote.core.designsystem.component.ResonoteNavigationSuiteScaffold
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.navigation.TabsShellNavKey
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlineSong
import com.resonote.feature.home.impl.HomePlaybackRequest
import com.resonote.feature.home.impl.HomeRoute
import com.resonote.feature.home.impl.HomeViewModel
import com.resonote.feature.player.impl.MiniPlayerUiState
import com.resonote.feature.player.impl.ResonoteMiniPlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ResonoteTheme(themeMode = ResonoteThemeMode.SYSTEM) {
                SyncSystemBars()
                ResonoteApp()
            }
        }
    }
}

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
private fun ResonoteApp() {
    val backStack = rememberNavBackStack(TabsShellNavKey)
    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<TabsShellNavKey> { TabsShell() }
        },
    )
}

@Composable
internal fun TabsShell(homeViewModel: HomeViewModel? = null) {
    val tabsShellState = rememberTabsShellState()
    val selectedTab = tabsShellState.selectedTab
    var prototypeQueue by remember { mutableStateOf<List<OnlineSong>>(emptyList()) }
    var currentSongId by rememberSaveable { mutableStateOf<String?>(null) }
    var isPlaying by rememberSaveable { mutableStateOf(true) }
    var progress by rememberSaveable { mutableStateOf(0.42f) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showComingSoon() {
        scope.launch { snackbarHostState.showSnackbar("功能开发中") }
    }

    fun play(request: HomePlaybackRequest) {
        prototypeQueue = request.songs
        currentSongId = request.songs[request.startIndex].hash
        isPlaying = true
        progress = 0f
    }

    BackHandler(enabled = selectedTab != ResonoteTab.HOME) {
        tabsShellState.handleBack()
    }

    val currentSong = prototypeQueue.firstOrNull { it.hash == currentSongId }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(outerPadding),
            ) {
                when (selectedTab) {
                    ResonoteTab.HOME -> {
                        val bottomContentPadding = if (currentSong == null) 24.dp else 120.dp
                        if (homeViewModel == null) {
                            HomeRoute(
                                playingMediaId = currentSongId,
                                bottomContentPadding = bottomContentPadding,
                                onSearchClick = ::showComingSoon,
                                onRecognitionClick = ::showComingSoon,
                                onPlay = ::play,
                                onOpenRankings = ::showComingSoon,
                                onOpenFeaturedPlaylists = ::showComingSoon,
                                onSongMoreClick = { showComingSoon() },
                                onPlaylistClick = { showComingSoon() },
                            )
                        } else {
                            HomeRoute(
                                playingMediaId = currentSongId,
                                bottomContentPadding = bottomContentPadding,
                                onSearchClick = ::showComingSoon,
                                onRecognitionClick = ::showComingSoon,
                                onPlay = ::play,
                                onOpenRankings = ::showComingSoon,
                                onOpenFeaturedPlaylists = ::showComingSoon,
                                onSongMoreClick = { showComingSoon() },
                                onPlaylistClick = { showComingSoon() },
                                viewModel = homeViewModel,
                            )
                        }
                    }

                    ResonoteTab.DISCOVER -> PlaceholderPage(
                        title = stringResource(R.string.tab_discover),
                    )

                    ResonoteTab.MY -> PlaceholderPage(
                        title = stringResource(R.string.tab_my),
                    )
                }

                if (currentSong != null) {
                    ResonoteMiniPlayer(
                        state = MiniPlayerUiState(
                            mediaId = currentSong.hash,
                            title = currentSong.title,
                            artist = currentSong.artist.orEmpty(),
                            qualityLabel = currentSong.quality.toLabel(),
                            isVip = currentSong.vip,
                            isPlaying = isPlaying,
                            progress = progress,
                            artworkColors = PrototypeArtworkColors,
                        ),
                        onOpenPlayer = ::showComingSoon,
                        onTogglePlay = { isPlaying = !isPlaying },
                        onNext = {
                            val currentIndex = prototypeQueue.indexOfFirst { it.hash == currentSongId }
                            if (prototypeQueue.isNotEmpty()) {
                                currentSongId = prototypeQueue[(currentIndex + 1).mod(prototypeQueue.size)].hash
                                isPlaying = true
                                progress = 0f
                            }
                        },
                        onOpenQueue = ::showComingSoon,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
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

private fun AudioQuality.toLabel(): String? =
    when (this) {
        AudioQuality.Standard -> null
        AudioQuality.HighQuality -> "HQ"
        AudioQuality.HighResolution -> "HI-RES"
        AudioQuality.Lossless -> "LOSSLESS"
    }

@Composable
private fun PlaceholderPage(title: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(top = 72.dp),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.page_in_design),
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
