package com.resonote.feature.album.impl

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
class AlbumScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun album_compactScrollStates() {
        val songs = listOf(
            song("harbor", "离港之前", "林澈", AudioQuality.HighResolution, true),
            song("tide", "潮汐信号", "林澈 · Winter Archive", AudioQuality.Lossless, true),
            song("island", "无人岛来信", "林澈", AudioQuality.Lossless, false),
            song("route", "夜航路线", "林澈", AudioQuality.HighResolution, false),
            song("light", "舷窗微光", "林澈", AudioQuality.Standard, false),
            song("radio", "凌晨电台", "林澈", AudioQuality.HighQuality, false),
            song("home", "回到海岸", "林澈", AudioQuality.Standard, false),
        )
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    AlbumScreen(
                        state = AlbumUiState.Content(
                            metadata = AlbumMetadata(
                                id = "night-flight",
                                title = "夜航日志：写给海岸线的七封信",
                                artist = "林澈 / Winter Archive",
                                coverUrl = null,
                                publishDate = "2026-08-13 00:00:00",
                                songCount = 14,
                            ),
                            songs = songs,
                            page = 1,
                            hasMore = true,
                        ),
                        playingMediaId = "tide",
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

        composeRule.onAllNodesWithText("夜航日志：写给海岸线的七封信").assertCountEquals(2)
        composeRule.onNodeWithText("播放全部").assertExists()
        composeRule.onNodeWithContentDescription("夜航日志：写给海岸线的七封信的专辑封面").assertExists()
        capture("top")

        composeRule.onNodeWithTag("album-list").performScrollToIndex(6)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("加载更多").assertExists()
        capture("songs")
    }

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/Album/AlbumCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    private fun song(id: String, title: String, artist: String, quality: AudioQuality, vip: Boolean) = OnlineSong(
        hash = id,
        title = title,
        artist = artist,
        coverUrl = null,
        albumId = "night-flight",
        albumAudioId = "audio-$id",
        durationMillis = 248_000,
        quality = quality,
        vip = vip,
        albumTitle = "夜航日志",
    )
}
