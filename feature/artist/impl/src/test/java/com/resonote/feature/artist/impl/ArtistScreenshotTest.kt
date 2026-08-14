package com.resonote.feature.artist.impl

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
class ArtistScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

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
                                intro = "独立唱作人与声音采集者，在潮汐、城市边缘和凌晨电台之间记录缓慢展开的旋律。",
                                songCount = 36,
                                albumCount = 4,
                                mvCount = 7,
                                fansCount = 128_000,
                            ),
                            popular = ArtistPageUiState.Content(
                                songs = songs,
                                page = 1,
                                total = 36,
                                hasMore = true,
                            ),
                        ),
                        playingMediaId = "tide",
                        onBack = {},
                        onSelectSection = {},
                        onRetry = {},
                        onLoadMore = {},
                        onPlayAll = {},
                        onSongClick = {},
                        onSongMoreClick = null,
                    )
                }
            }
        }

        composeRule.onAllNodesWithText("林澈与潮汐记忆").assertCountEquals(2)
        composeRule.onNodeWithText("热门").assertExists()
        composeRule.onNodeWithText("最新").assertExists()
        composeRule.onNodeWithContentDescription("林澈与潮汐记忆的歌手头像").assertExists()
        capture("top")

        composeRule.onNodeWithTag("artist-list").performScrollToIndex(8)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("加载更多").assertExists()
        capture("songs")
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
