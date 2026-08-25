package com.resonote.feature.artist.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.ArtistAlbum
import com.resonote.core.model.ArtistVideo
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.screenshottesting.DefaultRoborazziOptions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-420dpi")
class ArtistScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun artist_firstFrameUsesRouteHeroAndKeepsNode() {
        val profile = ArtistProfile("stable-artist", "稳定歌手", null, null, 1, 1, null, null)
        var state by mutableStateOf(ArtistUiState())
        composeRule.setContent {
            ResonoteTheme {
                ArtistScreen(
                    state = state,
                    initialProfile = profile,
                    playingMediaId = null,
                    onBack = {},
                    onFollowClick = {},
                    onSelectSection = {},
                    onSelectSort = {},
                    onRetry = {},
                    onRefresh = {},
                    onLoadMore = {},
                    onPlayAll = {},
                    onSongClick = {},
                    onSongMoreClick = null,
                    onAlbumClick = {},
                    onVideoClick = {},
                )
            }
        }
        val initialHeroId = composeRule.onNodeWithTag("artist-hero").fetchSemanticsNode().id
        composeRule.onNodeWithTag("artist-skeleton").assertExists()

        composeRule.runOnIdle { state = ArtistUiState(profile = profile) }

        val loadedHeroId = composeRule.onNodeWithTag("artist-hero").fetchSemanticsNode().id
        assertEquals(initialHeroId, loadedHeroId)
    }

    @Test
    fun artist_compactScrollStates() {
        val songs = listOf(
            song("tide", "潮汐信号", AudioQuality.Lossless, true),
            song("coast", "海岸线以北", AudioQuality.HighResolution, true),
            song("letter", "无人岛来信", AudioQuality.Lossless, false),
            song("route", "夜航路线", AudioQuality.HighResolution, false),
            song("light", "舷窗微光", AudioQuality.Standard, false),
            song("radio", "凌晨电台", AudioQuality.HighQuality, false),
            song("home", "回到海岸", AudioQuality.Standard, false),
        )
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    ArtistScreen(
                        state = ArtistUiState(
                            profile = ArtistProfile(
                                id = "lin-che",
                                name = "林澈与潮汐记忆",
                                avatarUrl = null,
                                intro = "独立唱作人与声音采集者，在潮汐、城市边缘和凌晨电台之间记录缓慢展开的旋律。" +
                                    "她习惯把旅途中收集的环境声写进作品，让海风、旧车站和深夜广播成为歌曲的一部分。" +
                                    "最近的创作继续探索人与城市之间若即若离的关系。",
                                songCount = 36,
                                albumCount = 4,
                                mvCount = 7,
                                fansCount = 128_000,
                            ),
                            follow = ArtistFollowUiState.Available(isFollowed = false),
                            popularSongs = ArtistPageUiState.Content(
                                items = songs.map(ArtistItem::Song),
                                page = 1,
                                total = 36,
                                hasMore = true,
                            ),
                        ),
                        playingMediaId = "tide",
                        onBack = {},
                        onFollowClick = {},
                        onSelectSection = {},
                        onSelectSort = {},
                        onRetry = {},
                        onRefresh = {},
                        onLoadMore = {},
                        onPlayAll = {},
                        onSongClick = {},
                        onSongMoreClick = null,
                        onAlbumClick = {},
                        onVideoClick = {},
                    )
                }
            }
        }

        composeRule.onAllNodesWithText("林澈与潮汐记忆").assertCountEquals(1)
        composeRule.onNodeWithText("热门").assertExists()
        composeRule.onNodeWithTag("artist-sort-toggle").assertExists()
        composeRule.onNodeWithText("最新").assertDoesNotExist()
        composeRule.onNodeWithText("关注").assertExists()
        composeRule.onAllNodesWithText("歌曲").assertCountEquals(2)
        composeRule.onNodeWithText("专辑").assertExists()
        composeRule.onNodeWithText("MV").assertExists()
        composeRule.onNodeWithContentDescription("林澈与潮汐记忆的歌手头像").assertExists()
        composeRule.onNodeWithText("展开").assertDoesNotExist()
        composeRule.onNodeWithText("收起").assertDoesNotExist()
        composeRule.onNodeWithTag("artist-description-toggle").performClick()
        capture("expanded")
        composeRule.onNodeWithTag("artist-description-toggle").performClick()
        capture("top")

        composeRule.onNodeWithTag("artist-list").performScrollToIndex(8)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("回到海岸").assertExists()
        capture("songs")
    }

    @Test
    fun artist_primaryTabsShowAlbumAndMvCards() {
        var state by mutableStateOf(
            ArtistUiState(
                profile = ArtistProfile(id = "lin-che", name = "林澈"),
                follow = ArtistFollowUiState.Available(isFollowed = true),
                popularSongs = ArtistPageUiState.Content(
                    items = List(10) { index ->
                        ArtistItem.Song(
                            song(
                                id = "song-$index",
                                title = if (index == 0) "潮汐" else "歌曲 $index",
                                quality = AudioQuality.Standard,
                                vip = false,
                            ),
                        )
                    },
                    page = 1,
                    total = 10,
                    hasMore = false,
                ),
                popularAlbums = ArtistPageUiState.Content(
                    items = List(10) { index ->
                        ArtistItem.Album(
                            ArtistAlbum(
                                id = "album-$index",
                                name = if (index == 0) "潮汐专辑" else "专辑 $index",
                                artist = "林澈",
                                coverUrl = null,
                                publishDate = "2026-08-23",
                                songCount = 10,
                            ),
                        )
                    },
                    page = 1,
                    total = 10,
                    hasMore = false,
                ),
                videos = ArtistPageUiState.Content(
                    items = listOf(
                        ArtistItem.Video(ArtistVideo("mv", "潮汐 MV", "林澈", null, 180_000)),
                    ),
                    page = 1,
                    total = 1,
                    hasMore = false,
                ),
            ),
        )
        composeRule.setContent {
            ResonoteTheme {
                ArtistScreen(
                    state = state,
                    playingMediaId = null,
                    onBack = {},
                    onFollowClick = {
                        val follow = state.follow as ArtistFollowUiState.Available
                        state = state.copy(follow = follow.copy(isFollowed = !follow.isFollowed))
                    },
                    onSelectSection = { state = state.copy(selectedSection = it) },
                    onSelectSort = { state = state.copy(selectedSort = it) },
                    onRetry = {},
                    onRefresh = {},
                    onLoadMore = {},
                    onPlayAll = {},
                    onSongClick = {},
                    onSongMoreClick = null,
                    onAlbumClick = {},
                    onVideoClick = {},
                )
            }
        }

        val heroLeftBeforeSwipe = composeRule.onNodeWithTag("artist-hero")
            .fetchSemanticsNode().boundsInRoot.left
        val tabsLeftBeforeSwipe = composeRule.onNodeWithTag("artist-section-tabs")
            .fetchSemanticsNode().boundsInRoot.left
        composeRule.onNodeWithTag("artist-list").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("潮汐专辑").assertExists()
        val heroLeftAfterSwipe = composeRule.onNodeWithTag("artist-hero")
            .fetchSemanticsNode().boundsInRoot.left
        val tabsLeftAfterSwipe = composeRule.onNodeWithTag("artist-section-tabs")
            .fetchSemanticsNode().boundsInRoot.left
        assertEquals(heroLeftBeforeSwipe, heroLeftAfterSwipe, 1f)
        assertEquals(tabsLeftBeforeSwipe, tabsLeftAfterSwipe, 1f)
        composeRule.onNodeWithText("歌曲").performClick()

        composeRule.onNodeWithText("已关注").performClick()
        composeRule.onNodeWithText("关注").assertExists()
        composeRule.onNodeWithTag("artist-sort-toggle").performClick()
        composeRule.onNodeWithText("最新").assertExists()
        composeRule.onNodeWithTag("artist-sort-toggle").performClick()
        composeRule.onNodeWithText("热门").assertExists()
        composeRule.onNodeWithTag("artist-list").performTouchInput {
            swipeUp(startY = 700f, endY = 520f, durationMillis = 200)
        }
        composeRule.waitForIdle()
        val tabTopBeforeSwitch = composeRule.onNodeWithTag("artist-section-tabs")
            .fetchSemanticsNode().boundsInRoot.top
        composeRule.onNodeWithText("专辑").performClick()
        composeRule.waitForIdle()
        val tabTopAfterSwitch = composeRule.onNodeWithTag("artist-section-tabs")
            .fetchSemanticsNode().boundsInRoot.top
        assertEquals(tabTopBeforeSwitch, tabTopAfterSwitch, 1f)
        composeRule.onNodeWithText("潮汐专辑").assertExists()
        composeRule.onNodeWithText("热门").assertExists()
        composeRule.onNodeWithText("MV").performClick()
        composeRule.onNodeWithText("潮汐 MV").assertExists()
        composeRule.onNodeWithText("共 1 个 MV").assertExists()
        composeRule.onNodeWithText("热门").assertDoesNotExist()
        composeRule.onNodeWithText("最新").assertDoesNotExist()
    }

    @Test
    fun artist_usesCommonEmptyAndErrorStates() {
        var state by mutableStateOf(
            ArtistUiState(
                profile = ArtistProfile(id = "lin-che", name = "林澈"),
                follow = ArtistFollowUiState.AuthenticationRequired,
                popularSongs = ArtistPageUiState.Empty,
            ),
        )
        composeRule.setContent {
            ResonoteTheme {
                ArtistScreen(
                    state = state,
                    playingMediaId = null,
                    onBack = {},
                    onFollowClick = {},
                    onSelectSection = {},
                    onSelectSort = {},
                    onRetry = {},
                    onRefresh = {},
                    onLoadMore = {},
                    onPlayAll = {},
                    onSongClick = {},
                    onSongMoreClick = null,
                    onAlbumClick = {},
                    onVideoClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("resonote-empty-state").assertExists()
        capture("empty")
        composeRule.runOnIdle {
            state = state.copy(popularSongs = ArtistPageUiState.Error(ContentFailure.Network))
        }
        composeRule.onNodeWithTag("resonote-error-state").assertExists()
        composeRule.onNodeWithTag("resonote-empty-state").assertDoesNotExist()
        capture("error")
    }

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/Artist/ArtistCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    private fun song(id: String, title: String, quality: AudioQuality, vip: Boolean) = OnlineSong(
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
