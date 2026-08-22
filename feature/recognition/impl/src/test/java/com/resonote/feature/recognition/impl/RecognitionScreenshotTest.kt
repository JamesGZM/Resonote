package com.resonote.feature.recognition.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.RecognitionMatch
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
class RecognitionScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun recognition_compactIdle() {
        var started = false
        var backed = false
        setScreen(
            state = RecognitionUiState.Idle,
            onBack = { backed = true },
            onStart = { started = true },
        )

        composeRule.onNodeWithText("听歌识曲").assertIsDisplayed()
        composeRule.onNodeWithText("让旋律靠近一点").assertIsDisplayed()
        composeRule.onNodeWithText("开始识别").assertIsDisplayed()
        composeRule.onNodeWithTag("recognition-action").performClick()
        composeRule.onNodeWithContentDescription("返回").performClick()

        assertThat(started).isTrue()
        assertThat(backed).isTrue()
        capture("idle")
    }

    @Test
    fun recognition_compactRecording() {
        setScreen(
            RecognitionUiState.Recording(
                elapsedMillis = 4_000,
                amplitude = 0.62f,
            ),
        )

        composeRule.onNodeWithText("正在聆听").assertIsDisplayed()
        composeRule.onNodeWithText("4 / 10 秒").assertIsDisplayed()
        capture("recording")
    }

    @Test
    fun recognition_compactSilentRecordingHasNoWaveform() {
        setScreen(
            RecognitionUiState.Recording(
                elapsedMillis = 1_000,
                amplitude = 0.02f,
            ),
        )

        composeRule.onNodeWithText("正在聆听").assertIsDisplayed()
        capture("recording_silent")
    }

    @Test
    fun recognition_compactModerateRecordingEnergy() {
        setScreen(
            RecognitionUiState.Recording(
                elapsedMillis = 2_000,
                amplitude = 0.35f,
            ),
        )

        composeRule.onNodeWithText("正在聆听").assertIsDisplayed()
        capture("recording_clearance")
    }

    @Test
    fun recognition_compactPermanentPermissionDenial() {
        setScreen(RecognitionUiState.PermissionDenied(permanently = true))

        composeRule.onNodeWithText("打开应用设置").assertIsDisplayed()
        capture("permission_denied")
    }

    @Test
    fun recognition_compactNoMatch() {
        var retried = false
        setScreen(
            state = RecognitionUiState.NoMatch,
            onRetry = { retried = true },
        )

        composeRule.onNodeWithText("没有找到匹配歌曲").assertIsDisplayed()
        composeRule.onNodeWithText("重新听一次").assertIsDisplayed()
        composeRule.onNodeWithTag("recognition-action").performClick()

        assertThat(retried).isTrue()
        capture("no_match")
    }

    @Test
    fun recognition_compactMatchesAndPreservesActions() {
        val match = match("signal", 0.91)
        var played: OnlineSong? = null
        var searched: RecognitionMatch? = null
        var added: OnlineSong? = null
        var reset = false
        setScreen(
            state = RecognitionUiState.Matches(
                listOf(
                    match,
                    match("coast", 0.84, title = "沿海公路", artist = "清醒电台"),
                    match("night", 0.76, title = "夜航星", artist = "折光乐队"),
                ),
            ),
            onPlay = { played = it },
            onSearch = { searched = it },
            onAddToPlaylist = { added = it },
            onReset = { reset = true },
        )

        composeRule.onAllNodesWithText("SQ")[0].assertIsDisplayed()
        composeRule.onAllNodesWithText("播放")[0].performClick()
        composeRule.onAllNodesWithContentDescription("添加到歌单")[0].performClick()
        composeRule.onAllNodesWithContentDescription("搜索这首歌")[0].performClick()
        composeRule.onNodeWithText("重新识别").performClick()

        assertThat(played).isEqualTo(match.song)
        assertThat(searched).isEqualTo(match)
        assertThat(added).isEqualTo(match.song)
        assertThat(reset).isTrue()
        capture("matches")
    }

    @Test
    fun recognition_matchCarouselSwipesBetweenResults() {
        setScreen(
            state = RecognitionUiState.Matches(
                listOf(
                    match("signal", 0.91),
                    match("coast", 0.84, title = "沿海公路", artist = "清醒电台"),
                ),
            ),
        )

        composeRule.onNodeWithTag("recognition-match-pager").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("沿海公路").assertIsDisplayed()
    }

    private fun setScreen(
        state: RecognitionUiState,
        onBack: () -> Unit = {},
        onStart: () -> Unit = {},
        onRetry: () -> Unit = {},
        onPlay: (OnlineSong) -> Unit = {},
        onSearch: (RecognitionMatch) -> Unit = {},
        onAddToPlaylist: (OnlineSong) -> Unit = {},
        onReset: () -> Unit = {},
    ) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    RecognitionScreen(
                        state = state,
                        onBack = onBack,
                        onStart = onStart,
                        onStop = {},
                        onRetry = onRetry,
                        onOpenSettings = {},
                        onPlay = onPlay,
                        onSearch = onSearch,
                        onAddToPlaylist = onAddToPlaylist,
                        onReset = onReset,
                        animateListeningField = false,
                    )
                }
            }
        }
        composeRule.onNodeWithTag("recognition-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("recognition-toolbar").assertIsDisplayed()
    }

    private fun capture(name: String) {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/Recognition/RecognitionCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    private companion object {
        fun match(
            id: String,
            confidence: Double,
            title: String = "潮汐信号：海岸线现场",
            artist: String = "林澈 · Winter Archive",
        ) = RecognitionMatch(
            confidence = confidence,
            song = OnlineSong(
                hash = id,
                title = title,
                artist = artist,
                coverUrl = null,
                albumId = null,
                albumAudioId = null,
                durationMillis = 265_000,
                quality = AudioQuality.Lossless,
                vip = false,
            ),
        )
    }
}
