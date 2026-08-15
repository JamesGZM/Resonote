package com.resonote.feature.player.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.LyricLine
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackMode
import com.resonote.core.playback.PlaybackState
import com.resonote.core.playback.PlaybackStatus
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
@Config(sdk = [35], qualifiers = "w390dp-h844dp-420dpi")
@OptIn(ExperimentalRoborazziApi::class)
class PlayerScreenScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun player_compactCover() {
        setPlayerContent()

        captureScreenRoboImage(
            filePath = "src/test/screenshots/Player/PlayerCompact_cover.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun player_compactLyrics() {
        setPlayerContent(initialPage = 1)

        captureScreenRoboImage(
            filePath = "src/test/screenshots/Player/PlayerCompact_lyrics.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun player_compactSpeedDialog() {
        setPlayerContent()
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Playback speed").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("1.5×").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()

        captureScreenRoboImage(
            filePath = "src/test/screenshots/Player/PlayerCompact_speed.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun primaryControlsDispatchIndependentActions() {
        var toggleCount = 0
        var nextCount = 0
        setPlayerContent(onTogglePlay = { toggleCount++ }, onNext = { nextCount++ })

        composeRule.onNodeWithContentDescription("Pause").performClick()
        composeRule.onNodeWithContentDescription("Next").performClick()

        assertEquals(1, toggleCount)
        assertEquals(1, nextCount)
    }

    @Test
    fun playbackSpeedSelectionDispatchesFromOverflow() {
        var selected: PlaybackSpeed? = null
        setPlayerContent(onPlaybackSpeedChange = { selected = it })

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Playback speed").performClick()
        composeRule.onNodeWithText("1.5×").performClick()

        assertEquals(PlaybackSpeed.OneAndHalf, selected)
    }

    @Test
    fun onlineSongActionsRemainReachableFromOverflow() {
        var songActionCount = 0
        setPlayerContent(onSongMoreClick = { songActionCount++ })

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Song actions").performClick()

        assertEquals(1, songActionCount)
    }

    private fun setPlayerContent(
        onTogglePlay: () -> Unit = {},
        onNext: () -> Unit = {},
        onPlaybackSpeedChange: (PlaybackSpeed) -> Unit = {},
        onSongMoreClick: (() -> Unit)? = null,
        initialPage: Int = 0,
    ) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    PlayerScreen(
                        state = screenshotState(),
                        onBack = {},
                        onTogglePlay = onTogglePlay,
                        onPrevious = {},
                        onNext = onNext,
                        onSeek = {},
                        onModeChange = {},
                        onPlaybackSpeedChange = onPlaybackSpeedChange,
                        onRetryLyrics = {},
                        onSelectQueueItem = {},
                        onRemoveQueueItem = {},
                        onClearQueue = {},
                        initialPage = initialPage,
                        onSongMoreClick = onSongMoreClick,
                    )
                }
            }
        }
    }

    private companion object {
        fun screenshotState(): PlayerUiState {
            val current = song("current", "潮汐记忆", "林澈")
            val queue = listOf(current, song("next", "失重来信", "沉岛乐队"), song("third", "凌晨四点", "周遥"))
            return PlayerUiState(
                playback = PlaybackState(
                    queue = queue.map(::PlaybackItem),
                    currentIndex = 0,
                    status = PlaybackStatus.Playing,
                    positionMillis = 102_000,
                    durationMillis = 248_000,
                    mode = PlaybackMode.ListLoop,
                ),
                lyrics = LyricsUiState.Content(
                    listOf(
                        LyricLine(0, "夜色落进无声的海"),
                        LyricLine(38_000, "微光沿着波纹醒来"),
                        LyricLine(92_000, "潮汐把记忆推回岸边"),
                    ),
                ),
            )
        }

        fun song(hash: String, title: String, artist: String) = OnlineSong(
            hash = hash,
            title = title,
            artist = artist,
            coverUrl = null,
            albumId = "night-signal",
            albumAudioId = "audio-$hash",
            durationMillis = 248_000,
            quality = AudioQuality.Lossless,
            vip = false,
        )
    }
}
