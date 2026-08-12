package com.resonote.feature.ranking.impl

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
class RankingScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun ranking_compactScrollStates() {
        val songs = listOf(
            song("tide", "潮汐信号", "林澈 · 潮汐记忆", AudioQuality.Lossless, true),
            song("coast", "海岸线以北", "林澈", AudioQuality.HighResolution, true),
            song("letter", "无人岛来信", "林澈", AudioQuality.Lossless, false),
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
                    RankingScreen(
                        state = RankingUiState.Content(
                            metadata = RankingMetadata("tide-chart", "潮汐热歌榜 · 本周上升最快", null),
                            songs = songs,
                            page = 1,
                            total = 50,
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

        composeRule.onAllNodesWithText("潮汐热歌榜 · 本周上升最快").assertCountEquals(2)
        composeRule.onNodeWithContentDescription("潮汐热歌榜 · 本周上升最快的榜单封面").assertExists()
        composeRule.onNodeWithText("01").assertExists()
        capture("top")

        composeRule.onNodeWithTag("ranking-list").performScrollToIndex(6)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("加载更多").assertExists()
        capture("songs")
    }

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/Ranking/RankingCompact_$name.png",
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
        albumId = "night-flight",
        albumAudioId = "audio-$id",
        durationMillis = 248_000,
        quality = quality,
        vip = vip,
    )
}
