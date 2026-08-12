package com.resonote.feature.playlist.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-420dpi")
class PlaylistScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playlist_compactScrollStates() {
        val songs = listOf(
            song("quiet", "静默轨道", "Resonote Ensemble", AudioQuality.HighResolution, true),
            song("room", "蓝色房间", "Lin & The Archive", AudioQuality.Lossless, true),
            song("years", "那些年我们一起听过的歌", "陈粒", AudioQuality.Lossless, false),
            song("signal", "晚风信号", "林澈 · 潮汐记忆", AudioQuality.HighResolution, false),
            song("forest", "写给森林的信", "北岸合唱团", AudioQuality.Standard, false),
            song("snow", "雪线以北", "远山计划", AudioQuality.HighQuality, false),
            song("city", "城市醒来之前", "晨雾唱片", AudioQuality.Standard, false),
        )
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
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
                        ),
                        playingMediaId = "room",
                        onBack = {},
                        onRetry = {},
                        onLoadMore = {},
                        onPlayAll = {},
                        onSongClick = {},
                        onSongMoreClick = null,
                    )
                }
            }
        }

        composeRule.onAllNodesWithText("深夜独白：安静的陪伴").assertCountEquals(2)
        composeRule.onNodeWithText("播放全部").assertExists()
        composeRule.onNodeWithContentDescription("More actions for 静默轨道").assertDoesNotExist()
        capture("top")

        composeRule.onNodeWithTag("playlist-list").performScrollToIndex(6)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("加载更多").assertExists()
        capture("songs")
    }

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/Playlist/PlaylistCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    private fun song(
        id: String,
        title: String,
        artist: String,
        quality: AudioQuality,
        vip: Boolean,
    ) = OnlineSong(
        hash = id,
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
