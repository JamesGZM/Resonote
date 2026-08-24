package com.resonote.feature.ranking.impl

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
class RankingScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun ranking_compactScrollStates() {
        setRankingContent(ResonoteThemeMode.LIGHT)

        composeRule.onAllNodesWithText("潮汐热歌榜 · 本周上升最快").assertCountEquals(1)
        composeRule.onNodeWithContentDescription("潮汐热歌榜 · 本周上升最快的榜单封面").assertExists()
        composeRule.onNodeWithContentDescription("第 1 名").assertIsDisplayed()
        capture("top")

        composeRule.onNodeWithTag("ranking-list").performScrollToIndex(6)
        composeRule.waitForIdle()
        capture("songs")
    }

    @Test
    fun ranking_compactTopDark() {
        setRankingContent(ResonoteThemeMode.DARK)
        composeRule.onNodeWithContentDescription("第 1 名").assertIsDisplayed()
        capture("top_dark")
    }

    @Test
    fun ranking_compactTopAmoled() {
        setRankingContent(ResonoteThemeMode.AMOLED)
        composeRule.onNodeWithContentDescription("第 1 名").assertIsDisplayed()
        capture("top_amoled")
    }

    @Test
    fun ranking_compactLoadingSkeleton() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme {
                    RankingScreen(
                        state = RankingUiState.Loading(
                            RankingMetadata("tide-chart", "潮汐热歌榜 · 本周上升最快", null),
                        ),
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
        composeRule.onNodeWithTag("ranking-skeleton").assertIsDisplayed()
        capture("loading")
    }

    @Test
    fun ranking_loadingToContent_keepsRouteHeroNode() {
        val metadata = RankingMetadata("stable-ranking", "稳定榜单", null)
        var state by mutableStateOf<RankingUiState>(RankingUiState.Loading(RankingMetadata("", null, null)))
        composeRule.setContent {
            ResonoteTheme {
                RankingScreen(
                    state = state,
                    initialMetadata = metadata,
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
        val loadingHeroId = composeRule.onNodeWithTag("ranking-hero").fetchSemanticsNode().id

        composeRule.runOnIdle {
            state = RankingUiState.Content(metadata, listOf(songs.first()), 1, 1, false)
        }

        val contentHeroId = composeRule.onNodeWithTag("ranking-hero").fetchSemanticsNode().id
        assertEquals(loadingHeroId, contentHeroId)
    }

    private fun setRankingContent(themeMode: ResonoteThemeMode) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = themeMode) {
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
            filePath = "src/test/screenshots/Ranking/RankingCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    private companion object {
        val songs = listOf(
            song("tide", "潮汐信号", "林澈 · 潮汐记忆", AudioQuality.Lossless, true),
            song("coast", "海岸线以北", "林澈", AudioQuality.HighResolution, true),
            song("letter", "无人岛来信", "林澈", AudioQuality.Lossless, false),
            song("route", "夜航路线", "林澈", AudioQuality.HighResolution, false),
            song("light", "舷窗微光", "林澈", AudioQuality.Standard, false),
            song("radio", "凌晨电台", "林澈", AudioQuality.HighQuality, false),
            song("home", "回到海岸", "林澈", AudioQuality.Standard, false),
            song("rain", "雨夜唱片", "林澈", AudioQuality.Lossless, false),
            song("station", "末班车站", "林澈", AudioQuality.Standard, false),
            song("echo", "潮汐回声", "林澈", AudioQuality.HighQuality, false),
            song("island", "远岛灯塔", "林澈", AudioQuality.Standard, false),
            song("morning", "清晨航线", "林澈", AudioQuality.HighResolution, false),
        )

        fun song(id: String, title: String, artist: String, quality: AudioQuality, vip: Boolean) = OnlineSong(
            hash = id,
            title = title,
            artist = artist,
            coverUrl = "https://example.invalid/$id.jpg",
            albumId = "night-flight",
            albumAudioId = "audio-$id",
            durationMillis = 248_000,
            quality = quality,
            vip = vip,
        )
    }
}
