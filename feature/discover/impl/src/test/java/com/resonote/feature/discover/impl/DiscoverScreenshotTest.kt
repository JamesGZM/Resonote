package com.resonote.feature.discover.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.Album
import com.resonote.core.model.AlbumRegion
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistCategory
import com.resonote.core.model.PlaylistSummary
import com.resonote.core.model.Ranking
import com.resonote.core.screenshottesting.DefaultRoborazziOptions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-420dpi")
class DiscoverScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun discover_compactPlaylists() {
        render(
            baseState.copy(
                categories = DiscoverLoadState.Content(categories),
                selectedParentCategoryId = 10,
                selectedPlaylistCategoryId = 11,
                playlists = DiscoverPageState.Content(playlists, 1, true),
            ),
            "playlists",
        )
        composeRule.onNodeWithText("流行").assertExists()
        composeRule.onNodeWithText("深夜航线").assertExists()
    }

    @Test
    fun discover_compactRankings() {
        render(
            baseState.copy(
                selectedSection = DiscoverSection.RANKINGS,
                rankings = DiscoverLoadState.Content(rankings),
            ),
            "rankings",
        )
        composeRule.onNodeWithText("潮汐热歌榜").assertExists()
    }

    @Test
    fun discover_compactRankingsDark() {
        render(
            rankingState,
            "rankings_dark",
            ResonoteThemeMode.DARK,
        )
    }

    @Test
    fun discover_compactRankingsAmoled() {
        render(
            rankingState,
            "rankings_amoled",
            ResonoteThemeMode.AMOLED,
        )
    }

    @Test
    fun discover_compactAlbums() {
        render(
            baseState.copy(
                selectedSection = DiscoverSection.ALBUMS,
                albums = DiscoverLoadState.Content(albums),
            ),
            "albums",
        )
        composeRule.onNodeWithText("夜航日志").assertExists()
    }

    @Test
    fun discover_compactSongs() {
        render(
            baseState.copy(
                selectedSection = DiscoverSection.SONGS,
                songs = DiscoverPageState.Content(songs, 1, true),
            ),
            "songs",
        )
        composeRule.onNodeWithText("播放全部").assertExists()
        composeRule.onNodeWithText("潮汐信号").assertExists()
    }

    @Test
    fun discover_compactPlaylistLoading() {
        render(baseState, "playlists_loading")
    }

    @Test
    fun discover_compactRankingLoading() {
        render(baseState.copy(selectedSection = DiscoverSection.RANKINGS), "rankings_loading")
    }

    @Test
    fun discover_compactAlbumLoading() {
        render(baseState.copy(selectedSection = DiscoverSection.ALBUMS), "albums_loading")
    }

    @Test
    fun discover_compactSongLoading() {
        render(baseState.copy(selectedSection = DiscoverSection.SONGS), "songs_loading")
    }

    @Test
    fun discover_compactEmpty() {
        render(
            baseState.copy(
                categories = DiscoverLoadState.Content(categories),
                playlists = DiscoverPageState.Empty,
            ),
            "empty",
        )
    }

    @Test
    fun discover_compactError() {
        render(
            baseState.copy(
                categories = DiscoverLoadState.Content(categories),
                playlists = DiscoverPageState.Error(ContentFailure.Network),
            ),
            "error",
        )
    }

    @Test
    fun discover_compactPaginationLoading() {
        render(
            baseState.copy(
                categories = DiscoverLoadState.Content(categories),
                playlists = DiscoverPageState.Content(playlists, 1, true, isLoadingMore = true),
            ),
            "pagination_loading",
        )
        composeRule.onNodeWithTag("resonote-load-more-footer").performScrollTo()
        capture("pagination_loading")
    }

    @Test
    fun discover_compactPaginationError() {
        render(
            baseState.copy(
                selectedSection = DiscoverSection.SONGS,
                songs = DiscoverPageState.Content(
                    songs,
                    1,
                    true,
                    loadMoreFailure = ContentFailure.Network,
                ),
            ),
            "pagination_error",
        )
        composeRule.onNodeWithTag("resonote-load-more-footer").performScrollTo()
        capture("pagination_error")
    }

    private fun render(state: DiscoverUiState, name: String, themeMode: ResonoteThemeMode = ResonoteThemeMode.LIGHT) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = themeMode) {
                    DiscoverScreen(
                        state = state,
                        bottomContentPadding = 24.dp,
                        playingMediaId = "tide",
                        onSelectSection = {},
                        onSelectPlaylistParent = {},
                        onSelectPlaylistCategory = {},
                        onSelectAlbumRegion = {},
                        onRetryCategories = {},
                        onRetry = {},
                        onRefresh = {},
                        onLoadMore = {},
                        onPlaylistClick = {},
                        onRankingClick = {},
                        onAlbumClick = {},
                        onPlaySongs = {},
                        onSongClick = {},
                        onSongMoreClick = null,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(
            when (state.selectedSection) {
                DiscoverSection.PLAYLISTS -> "歌单"
                DiscoverSection.RANKINGS -> "榜单"
                DiscoverSection.ALBUMS -> "新碟"
                DiscoverSection.SONGS -> "新歌"
            },
        ).assertIsDisplayed()
        capture(name)
    }

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/Discover/DiscoverCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    private companion object {
        val baseState = DiscoverUiState()
        val rankingState: DiscoverUiState
            get() = baseState.copy(
                selectedSection = DiscoverSection.RANKINGS,
                rankings = DiscoverLoadState.Content(rankings),
            )
        val categories = listOf(
            PlaylistCategory(10, "风格", listOf(PlaylistCategory(11, "流行", emptyList()))),
            PlaylistCategory(20, "场景", listOf(PlaylistCategory(21, "通勤", emptyList()))),
            PlaylistCategory(30, "情绪", emptyList()),
        )
        val playlists = listOf(
            PlaylistSummary("night", "深夜航线", null, 128_000),
            PlaylistSummary("coast", "沿着海岸慢慢醒来", null, 86_000),
            PlaylistSummary("city", "城市低速漫游", null, 42_000),
            PlaylistSummary("forest", "写给森林的信", null, 35_000),
            PlaylistSummary("room", "蓝色房间", null, 24_000),
            PlaylistSummary("radio", "凌晨电台", null, 18_000),
        )
        val rankings = listOf(
            Ranking("tide", "潮汐热歌榜", null),
            Ranking("rising", "本周上升最快", null),
            Ranking("indie", "独立音乐新声", null),
            Ranking("night", "深夜循环榜", null),
            Ranking("classic", "经典回响榜", null),
        )
        val albums = listOf(
            Album("flight", "夜航日志", "林澈", null, "2026-08-13", 14, AlbumRegion.Chinese),
            Album("archive", "Winter Archive", "潮汐记忆", null, "2026-08-10", 10, AlbumRegion.Western),
            Album("island", "无人岛来信", "林澈", null, "2026-08-08", 8, AlbumRegion.Chinese),
            Album("signal", "Signal / 04:17", "Northern Room", null, "2026-08-06", 12, AlbumRegion.Japanese),
            Album("forest", "森林电台", "北岸合唱团", null, "2026-08-04", 9, AlbumRegion.Korean),
            Album("blue", "蓝色房间", "Winter Archive", null, "2026-08-01", 11, AlbumRegion.Western),
        )
        val songs = listOf(
            song("tide", "潮汐信号", AudioQuality.Lossless, true),
            song("coast", "海岸线以北", AudioQuality.HighResolution, true),
            song("letter", "无人岛来信", AudioQuality.Lossless, false),
            song("route", "夜航路线", AudioQuality.HighResolution, false),
            song("light", "舷窗微光", AudioQuality.Standard, false),
            song("radio", "凌晨电台", AudioQuality.HighQuality, false),
            song("home", "回到海岸", AudioQuality.Standard, false),
        )

        fun song(id: String, title: String, quality: AudioQuality, vip: Boolean) = OnlineSong(
            hash = id,
            title = title,
            artist = "林澈 · 潮汐记忆",
            coverUrl = null,
            albumId = "night-flight",
            albumAudioId = "audio-$id",
            durationMillis = 248_000,
            quality = quality,
            vip = vip,
        )
    }
}
