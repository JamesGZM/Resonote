package com.resonote.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.ContentCatalogRepository
import com.resonote.core.data.HomeRepository
import com.resonote.core.data.RankingRepository
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.designsystem.component.ResonoteSnackbarHost
import com.resonote.core.designsystem.component.rememberResonoteSnackbarController
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.Album
import com.resonote.core.model.ArtistInfo
import com.resonote.core.model.ArtistSongsPage
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.Banner
import com.resonote.core.model.CatalogSongPage
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.HomeRefreshResult
import com.resonote.core.model.HomeContent
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistCategory
import com.resonote.core.model.PlaylistSummary
import com.resonote.core.model.Ranking
import com.resonote.core.model.RadioRecommendationResult
import com.resonote.core.model.RecommendationMode
import com.resonote.core.model.SongPage
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackState
import com.resonote.core.screenshottesting.DefaultRoborazziOptions
import com.resonote.feature.discover.impl.DiscoverViewModel
import com.resonote.feature.home.impl.HomeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-420dpi")
class TabsShellScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tabsShell_compactHome() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    val homeViewModel = remember { HomeViewModel(ScreenshotHomeRepository()) }
                    TabsShell(homeViewModel = homeViewModel)
                }
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Rankings").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("My music").assertExists()
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/TabsShell/TabsShellCompact_home.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun tabsShell_bottomChromeThemeMatrix() {
        var themeMode by mutableStateOf(ResonoteThemeMode.LIGHT)
        var dynamicColorEnabled by mutableStateOf(false)
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(
                    themeMode = themeMode,
                    dynamicColorEnabled = dynamicColorEnabled,
                ) {
                    val homeViewModel = remember { HomeViewModel(ScreenshotHomeRepository()) }
                    TabsShell(
                        homeViewModel = homeViewModel,
                        playbackState = PlaybackState(
                            queue = listOf(PlaybackItem(song("theme-preview"))),
                            currentIndex = 0,
                        ),
                    )
                }
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Rankings").fetchSemanticsNodes().isNotEmpty()
        }

        listOf(
            Triple("Light", ResonoteThemeMode.LIGHT, false),
            Triple("Dark", ResonoteThemeMode.DARK, false),
            Triple("Amoled", ResonoteThemeMode.AMOLED, false),
            Triple("Dynamic", ResonoteThemeMode.LIGHT, true),
        ).forEach { (name, mode, dynamic) ->
            composeRule.runOnIdle {
                themeMode = mode
                dynamicColorEnabled = dynamic
            }
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage(
                filePath = "src/test/screenshots/TabsShell/BottomShell_$name.png",
                roborazziOptions = DefaultRoborazziOptions,
            )
        }
    }

    @Test
    fun compactTabItemAndClickTargetAre64DpHigh() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    val homeViewModel = remember { HomeViewModel(ScreenshotHomeRepository()) }
                    TabsShell(homeViewModel = homeViewModel)
                }
            }
        }

        composeRule.onNodeWithTag("resonote-tab-home")
            .assertHeightIsEqualTo(64.dp)
    }

    @Test
    fun tabsShell_compactDiscoverRankingFromHomeShortcut() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    val homeViewModel = remember { HomeViewModel(ScreenshotHomeRepository()) }
                    val discoverViewModel = remember {
                        DiscoverViewModel(ScreenshotCatalogRepository(), ScreenshotRankingRepository())
                    }
                    TabsShell(homeViewModel = homeViewModel, discoverViewModel = discoverViewModel)
                }
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Rankings").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Rankings").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("潮汐热歌榜").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("潮汐热歌榜").assertExists()
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/TabsShell/TabsShellCompact_discover-ranking.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun featuredPlaylistsShortcutOpensRealDiscoverContent() {
        composeRule.setContent {
            ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                val homeViewModel = remember { HomeViewModel(ScreenshotHomeRepository()) }
                val discoverViewModel = remember {
                    DiscoverViewModel(ScreenshotCatalogRepository(), ScreenshotRankingRepository())
                }
                TabsShell(homeViewModel = homeViewModel, discoverViewModel = discoverViewModel)
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Featured playlists").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Featured playlists").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("深夜航线").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("深夜航线").assertExists()
    }

    @Test
    fun snackbarIsPlacedAboveTabBar() {
        val snackbarHostState = SnackbarHostState()
        setSnackbarContent(snackbarHostState)

        val snackbarBottom = composeRule.onNodeWithTag("resonote-snackbar-host")
            .fetchSemanticsNode().boundsInRoot.bottom
        val tabBarTop = composeRule.onNodeWithText("Home")
            .fetchSemanticsNode().boundsInRoot.top

        assertThat(composeRule.onAllNodesWithTag("resonote-snackbar-host").fetchSemanticsNodes()).hasSize(1)
        assertThat(snackbarBottom).isAtMost(tabBarTop)
    }

    @Test
    fun snackbarIsPlacedAboveMiniPlayer() {
        val snackbarHostState = SnackbarHostState()
        setSnackbarContent(
            snackbarHostState = snackbarHostState,
            playbackState = PlaybackState(queue = listOf(PlaybackItem(song("playing"))), currentIndex = 0),
        )

        val snackbarBottom = composeRule.onNodeWithTag("resonote-snackbar-host")
            .fetchSemanticsNode().boundsInRoot.bottom
        val miniPlayerTop = composeRule.onNodeWithTag("resonote-mini-player")
            .fetchSemanticsNode().boundsInRoot.top
        val expectedGapPx = with(composeRule.density) { 8.dp.toPx() }

        assertThat(miniPlayerTop - snackbarBottom).isWithin(1f).of(expectedGapPx)
    }

    @Test
    fun snackbarOutlivesTheEffectThatDispatchesIt() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                val snackbarHostState = remember { SnackbarHostState() }
                val snackbarController = rememberResonoteSnackbarController(snackbarHostState)
                var producerExists by remember { mutableStateOf(true) }
                Box(Modifier.fillMaxSize()) {
                    if (producerExists) {
                        LaunchedEffect(Unit) {
                            snackbarController.show(
                                message = "Playback failed",
                                duration = SnackbarDuration.Indefinite,
                            )
                            producerExists = false
                        }
                    }
                    ResonoteSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }

        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Playback failed").assertExists()

        composeRule.mainClock.advanceTimeBy(5_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Playback failed").assertExists()
    }

    private fun setSnackbarContent(
        snackbarHostState: SnackbarHostState,
        playbackState: PlaybackState = PlaybackState(),
    ) {
        composeRule.setContent {
            ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                val homeViewModel = remember { HomeViewModel(ScreenshotHomeRepository()) }
                var snackbarBottomInset by remember { mutableStateOf(0.dp) }
                val snackbarSpacing = ResonoteTokens.spacing.space2
                LaunchedEffect(snackbarHostState) {
                    snackbarHostState.showSnackbar(
                        message = "Saved to your library",
                        duration = SnackbarDuration.Indefinite,
                    )
                }
                Box(Modifier.fillMaxSize()) {
                    TabsShell(
                        homeViewModel = homeViewModel,
                        playbackState = playbackState,
                        onSnackbarBottomInsetChanged = { snackbarBottomInset = it },
                    )
                    ResonoteSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                start = snackbarSpacing,
                                end = snackbarSpacing,
                                bottom = snackbarSpacing + snackbarBottomInset,
                            ),
                    )
                }
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Saved to your library").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private class ScreenshotHomeRepository : HomeRepository {
        private val homeContent =
            HomeContent(
                dailyRecommendations = List(6) { song("daily-$it") },
                recommendedPlaylists = List(6) {
                    PlaylistSummary("playlist-$it", "推荐歌单 ${it + 1}", null, 12_000L * (it + 1))
                },
                newSongs = List(6) { song("new-$it") },
            )
        private val mutableContent = MutableStateFlow<HomeContent?>(homeContent)
        override val content: StateFlow<HomeContent?> = mutableContent

        override suspend fun refresh(): HomeRefreshResult = HomeRefreshResult.Updated(homeContent, emptyList())

        override suspend fun loadRadio(mode: RecommendationMode): RadioRecommendationResult =
            RadioRecommendationResult.Available(listOf(song("radio")))
    }

    private class ScreenshotCatalogRepository : ContentCatalogRepository {
        override suspend fun loadPlaylistCategories() = CollectionLoadResult.Available(
            listOf(PlaylistCategory(10, "风格", listOf(PlaylistCategory(11, "流行", emptyList())))),
        )

        override suspend fun loadCategoryPlaylists(categoryId: Int, page: Int, pageSize: Int) =
            CollectionLoadResult.Available(listOf(PlaylistSummary("playlist", "深夜航线", null, 128_000)))

        override suspend fun loadBanners(): CollectionLoadResult<List<Banner>> = unused()
        override suspend fun loadNewAlbums(page: Int, pageSize: Int): CollectionLoadResult<List<Album>> = unused()
        override suspend fun loadNewSongs(page: Int, pageSize: Int): CollectionLoadResult<SongPage> = unused()
        override suspend fun loadAlbumSongs(albumId: String, page: Int, pageSize: Int): CollectionLoadResult<CatalogSongPage> = unused()
        override suspend fun loadArtistDetail(artistId: String): CollectionLoadResult<ArtistInfo?> = unused()
        override suspend fun loadArtistSongs(artistId: String, page: Int, pageSize: Int, newestFirst: Boolean): CollectionLoadResult<ArtistSongsPage> = unused()
    }

    private class ScreenshotRankingRepository : RankingRepository {
        override suspend fun loadRankings() = CollectionLoadResult.Available(
            listOf(
                Ranking("tide", "潮汐热歌榜", null),
                Ranking("rising", "本周上升最快", null),
                Ranking("indie", "独立音乐新声", null),
            ),
        )

        override suspend fun loadSongs(rankId: String, page: Int, pageSize: Int): CollectionLoadResult<SongPage> = unused()
    }

    private companion object {
        fun <T> unused(): T = error("unused")

        fun song(id: String) =
            OnlineSong(
                hash = id,
                title = "歌曲 $id",
                artist = "Resonote Artist",
                coverUrl = null,
                albumId = "1",
                albumAudioId = "2",
                durationMillis = 180_000,
                quality = AudioQuality.Lossless,
                vip = false,
            )
    }
}
