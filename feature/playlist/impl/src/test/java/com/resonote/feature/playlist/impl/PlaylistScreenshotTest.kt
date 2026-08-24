package com.resonote.feature.playlist.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistDetails
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
class PlaylistScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playlist_compactScrollStates() {
        setPlaylistContent(ResonoteThemeMode.LIGHT)

        composeRule.onAllNodesWithText("深夜独白：安静的陪伴").assertCountEquals(1)
        composeRule.onNodeWithText("播放全部").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("歌单“深夜独白：安静的陪伴”的封面").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("静默轨道 的更多操作").assertExists()
        capture("top")

        composeRule.onNodeWithTag("playlist-list").performScrollToIndex(7)
        composeRule.waitForIdle()
        capture("songs")

        composeRule.onNodeWithContentDescription("城市醒来之前 的更多操作").performClick()
        composeRule.onNodeWithText("从歌单中移除？").assertExists()
        composeRule.onNodeWithText("“城市醒来之前”将从这个歌单中移除，不会删除歌曲本身。").assertExists()
    }

    @Test
    fun playlist_compactTopDark() {
        setPlaylistContent(ResonoteThemeMode.DARK)
        composeRule.onNodeWithText("播放全部").assertIsDisplayed()
        capture("top_dark")
    }

    @Test
    fun playlist_compactTopAmoled() {
        setPlaylistContent(ResonoteThemeMode.AMOLED)
        composeRule.onNodeWithText("播放全部").assertIsDisplayed()
        capture("top_amoled")
    }

    @Test
    fun playlist_compactFavoriteAction() {
        setPlaylistContent(
            themeMode = ResonoteThemeMode.LIGHT,
            favorite = PlaylistFavoriteUiState.Available(isFavorited = false),
            writable = false,
        )
        composeRule.onNodeWithText("收藏").assertIsDisplayed()
        capture("favorite")
    }

    @Test
    fun playlist_compactLoadingSkeleton() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme {
                    PlaylistScreen(
                        state = PlaylistUiState.Loading,
                        playingMediaId = null,
                        onBack = {},
                        onRetry = {},
                        onRefresh = {},
                        onLoadMore = {},
                        onPlayAll = {},
                        onSongClick = {},
                        onSongMoreClick = null,
                    )
                }
            }
        }
        composeRule.onNodeWithTag("playlist-skeleton").assertIsDisplayed()
        capture("loading")
    }

    @Test
    fun playlist_loadingToContent_keepsHeroNode() {
        val initialDetails = PlaylistDetails(
            id = "stable-hero",
            title = "稳定封面",
            description = "",
            coverUrl = null,
            songCount = 1,
        )
        var state by mutableStateOf<PlaylistUiState>(PlaylistUiState.Loading)
        composeRule.setContent {
            ResonoteTheme {
                PlaylistScreen(
                    state = state,
                    initialDetails = initialDetails,
                    heroPlaylistId = initialDetails.id,
                    playingMediaId = null,
                    onBack = {},
                    onRetry = {},
                    onRefresh = {},
                    onLoadMore = {},
                    onPlayAll = {},
                    onSongClick = {},
                    onSongMoreClick = null,
                )
            }
        }
        val loadingHeroId = composeRule.onNodeWithTag("playlist-hero").fetchSemanticsNode().id

        composeRule.runOnIdle {
            state = PlaylistUiState.Content(
                details = initialDetails,
                songs = listOf(songs.first()),
                page = 1,
                hasMore = false,
            )
        }

        val contentHeroId = composeRule.onNodeWithTag("playlist-hero").fetchSemanticsNode().id
        assertEquals(loadingHeroId, contentHeroId)
    }

    private fun setPlaylistContent(
        themeMode: ResonoteThemeMode,
        favorite: PlaylistFavoriteUiState = PlaylistFavoriteUiState.Hidden,
        writable: Boolean = true,
    ) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = themeMode) {
                    PlaylistScreen(
                        state = PlaylistUiState.Content(
                            details = PlaylistDetails(
                                id = "midnight",
                                title = "深夜独白：安静的陪伴",
                                description = "在城市安静下来之后，留一点空间给夜色、呼吸和缓慢展开的旋律。",
                                coverUrl = null,
                                songCount = 42,
                            ),
                            songs = songs,
                            page = 1,
                            hasMore = true,
                            writableListId = "list-midnight".takeIf { writable },
                            favorite = favorite,
                        ),
                        playingMediaId = "room",
                        onBack = {},
                        onRetry = {},
                        onRefresh = {},
                        onLoadMore = {},
                        onPlayAll = {},
                        onSongClick = {},
                        onSongMoreClick = null,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/Playlist/PlaylistCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    private companion object {
        val songs = listOf(
            song("quiet", "静默轨道", "Resonote Ensemble", AudioQuality.HighResolution, true),
            song("room", "蓝色房间", "Lin & The Archive", AudioQuality.Lossless, true),
            song("years", "那些年我们一起听过的歌", "陈粒", AudioQuality.Lossless, false),
            song("signal", "晚风信号", "林澈 · 潮汐记忆", AudioQuality.HighResolution, false),
            song("forest", "写给森林的信", "北岸合唱团", AudioQuality.Standard, false),
            song("snow", "雪线以北", "远山计划", AudioQuality.HighQuality, false),
            song("city", "城市醒来之前", "晨雾唱片", AudioQuality.Standard, false),
            song("harbor", "港口来信", "北岸计划", AudioQuality.Lossless, false),
            song("rain", "窗外的雨", "林澈", AudioQuality.Standard, false),
            song("station", "末班站台", "远山计划", AudioQuality.HighResolution, false),
            song("echo", "夜色回声", "潮汐记忆", AudioQuality.HighQuality, false),
            song("morning", "清晨之前", "晨雾唱片", AudioQuality.Standard, false),
        )

        fun song(id: String, title: String, artist: String, quality: AudioQuality, vip: Boolean) = OnlineSong(
            hash = id,
            fileId = "$id-file",
            title = title,
            artist = artist,
            coverUrl = null,
            albumId = null,
            albumAudioId = null,
            durationMillis = 248_000,
            quality = quality,
            vip = vip,
        )
    }
}
