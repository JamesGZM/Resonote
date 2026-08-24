package com.resonote.feature.video.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.media3.test.utils.FakePlayer
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.screenshottesting.DefaultRoborazziOptions
import com.resonote.feature.video.api.VideoNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-420dpi")
class VideoScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun video_compactLoading() {
        setScreen(state = VideoUiState.Loading)

        composeRule.onNodeWithText("正在连接视频源").assertIsDisplayed()
        composeRule.onAllNodesWithText("潮汐信号：海岸线现场").assertCountEquals(1)
        capture("loading")
    }

    @Test
    fun video_compactUnavailable() {
        setScreen(state = VideoUiState.Unavailable)

        composeRule.onNodeWithText("这个 MV 暂时无法播放").assertIsDisplayed()
        composeRule.onNodeWithText("重新加载").assertIsDisplayed()
        capture("unavailable")
    }

    @Test
    fun video_fullscreenLoading() {
        setScreen(
            state = VideoUiState.Loading,
            fullscreen = true,
            size = DpSize(844.dp, 390.dp),
        )

        composeRule.onNodeWithTag("video-fullscreen").assertIsDisplayed()
        composeRule.onNodeWithText("正在连接视频源").assertIsDisplayed()
        capture("fullscreen_loading")
    }

    @Test
    fun video_compactControls() {
        val player = FakePlayer()
        var fullscreenClicks = 0
        composeRule.setContent {
            DeviceConfigurationOverride(override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp))) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        VideoPlayerControls(
                            key = videoKey,
                            player = player,
                            fullscreen = false,
                            onBack = {},
                            onToggleFullscreen = { fullscreenClicks += 1 },
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("video-play-pause").assertIsDisplayed()
        composeRule.onNodeWithTag("video-progress").assertIsDisplayed()
        composeRule.onNodeWithTag("video-fullscreen-action").assertIsDisplayed()
        capture("controls")

        composeRule.onNodeWithTag("video-fullscreen-action").performClick()
        assertThat(fullscreenClicks).isEqualTo(1)
    }

    @Test
    fun video_seekBarLayers() {
        composeRule.setContent {
            DeviceConfigurationOverride(override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 96.dp))) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        VideoSeekBar(
                            positionMillis = 88_000,
                            bufferedPositionMillis = 156_000,
                            durationMillis = 265_000,
                            onSeek = {},
                            onSeekFinished = {},
                            modifier = Modifier.fillMaxWidth().padding(
                                horizontal = 16.dp,
                            ).testTag("video-progress-layers"),
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("video-progress-layers").assertIsDisplayed()
        capture("seek_bar_layers")
    }

    private fun setScreen(state: VideoUiState, fullscreen: Boolean = false, size: DpSize = DpSize(390.dp, 844.dp)) {
        composeRule.setContent {
            DeviceConfigurationOverride(override = DeviceConfigurationOverride.ForcedSize(size)) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    VideoScreen(
                        key = videoKey,
                        state = state,
                        player = null,
                        playbackFailed = false,
                        fullscreen = fullscreen,
                        onBack = {},
                        onToggleFullscreen = {},
                        onRetry = {},
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/Video/VideoCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    private companion object {
        val videoKey = VideoNavKey(
            hash = "tide-signal",
            title = "潮汐信号：海岸线现场",
            singer = "林澈 · Winter Archive",
            coverUrl = null,
            durationMillis = 265_000,
        )
    }
}
