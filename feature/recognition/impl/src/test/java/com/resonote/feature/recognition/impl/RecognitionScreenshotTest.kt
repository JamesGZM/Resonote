package com.resonote.feature.recognition.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
        setScreen(RecognitionUiState.Idle)

        composeRule.onNodeWithText("让声音留下线索").assertIsDisplayed()
        composeRule.onNodeWithText("开始聆听").assertIsDisplayed()
        capture("idle")
    }

    @Test
    fun recognition_compactRecording() {
        setScreen(RecognitionUiState.Recording(elapsedMillis = 4_000))

        composeRule.onNodeWithText("正在聆听").assertIsDisplayed()
        composeRule.onNodeWithText("4 / 10 秒").assertIsDisplayed()
        capture("recording")
    }

    @Test
    fun recognition_compactPermanentPermissionDenial() {
        setScreen(RecognitionUiState.PermissionDenied(permanently = true))

        composeRule.onNodeWithText("打开应用设置").assertIsDisplayed()
        capture("permission_denied")
    }

    @Test
    fun recognition_compactNoMatch() {
        setScreen(RecognitionUiState.NoMatch)

        composeRule.onNodeWithText("没有找到匹配歌曲").assertIsDisplayed()
        capture("no_match")
    }

    @Test
    fun recognition_compactMatchesAndPreservesActions() {
        val match = match("signal", 0.91)
        var played: OnlineSong? = null
        var searched: RecognitionMatch? = null
        setScreen(
            state = RecognitionUiState.Matches(listOf(match)),
            onPlay = { played = it },
            onSearch = { searched = it },
        )

        composeRule.onNodeWithText("播放").performClick()
        composeRule.onNodeWithText("继续搜索").performClick()

        assertThat(played).isEqualTo(match.song)
        assertThat(searched).isEqualTo(match)
        capture("matches")
    }

    private fun setScreen(
        state: RecognitionUiState,
        onPlay: (OnlineSong) -> Unit = {},
        onSearch: (RecognitionMatch) -> Unit = {},
    ) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    RecognitionScreen(
                        state = state,
                        onBack = {},
                        onStart = {},
                        onStop = {},
                        onRetry = {},
                        onOpenSettings = {},
                        onPlay = onPlay,
                        onSearch = onSearch,
                    )
                }
            }
        }
        composeRule.onNodeWithTag("recognition-screen").assertIsDisplayed()
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
        fun match(id: String, confidence: Double) = RecognitionMatch(
            confidence = confidence,
            song = OnlineSong(
                hash = id,
                title = "潮汐信号：海岸线现场",
                artist = "林澈 · Winter Archive",
                coverUrl = null,
                albumId = null,
                albumAudioId = null,
                durationMillis = 265_000,
                quality = AudioQuality.HighQuality,
                vip = false,
            ),
        )
    }
}
