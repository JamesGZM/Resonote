package com.resonote.core.playback.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.DesktopLyricsDisplayMode
import com.resonote.core.model.LyricLine
import com.resonote.core.model.LyricSyllable
import com.resonote.core.model.LyricsDocument
import com.resonote.core.model.LyricsPreferences
import com.resonote.core.model.PlaybackMode
import org.junit.Test

class DesktopLyricsRendererTest {
    @Test
    fun renderSelectsActiveLineHighlightsElapsedWordsAndShowsNextLine() {
        val document = LyricsDocument(
            listOf(
                LyricLine(
                    syllables = listOf(
                        LyricSyllable("Hello ", 1_000, 1_500),
                        LyricSyllable("world", 1_500, 2_000),
                    ),
                    translation = "你好，世界",
                ),
                LyricLine(2_000, "Next line"),
            ),
        )

        val content = DesktopLyricsRenderer.render(document, 1_600, LyricsPreferences())

        assertThat(content).isEqualTo(
            DesktopLyricsContent(
                primary = "Hello world",
                primaryHighlightTextOffset = 7f,
                supplemental = "你好，世界",
                next = "Next line",
                layoutReference = "Hello world",
            ),
        )
    }

    @Test
    fun renderHonorsSingleLineAndSupplementalPreferences() {
        val document = LyricsDocument(
            listOf(
                LyricLine(
                    syllables = listOf(LyricSyllable("Line", 1_000, 2_000, phonetic = "Lain")),
                    translation = "翻译",
                ),
                LyricLine(2_000, "Next"),
            ),
        )

        val content = DesktopLyricsRenderer.render(
            document,
            500,
            LyricsPreferences(
                translationEnabled = false,
                transliterationEnabled = false,
                desktopLyricsDisplayMode = DesktopLyricsDisplayMode.SingleLine,
            ),
        )

        assertThat(content?.primary).isEqualTo("Line")
        assertThat(content?.primaryHighlightTextOffset).isEqualTo(0f)
        assertThat(content?.supplemental).isNull()
        assertThat(content?.next).isNull()
    }

    @Test
    fun desktopControllerCyclesThroughEveryPlaybackMode() {
        assertThat(PlaybackMode.ListLoop.nextDesktopLyricsMode()).isEqualTo(PlaybackMode.Shuffle)
        assertThat(PlaybackMode.Shuffle.nextDesktopLyricsMode()).isEqualTo(PlaybackMode.SingleLoop)
        assertThat(PlaybackMode.SingleLoop.nextDesktopLyricsMode()).isEqualTo(PlaybackMode.Sequential)
        assertThat(PlaybackMode.Sequential.nextDesktopLyricsMode()).isEqualTo(PlaybackMode.ListLoop)
    }

    @Test
    fun desktopLyricsPositionInterpolatesBetweenPlaybackUpdatesAndStopsAtDuration() {
        assertThat(
            interpolatedDesktopLyricsPosition(
                positionAnchorMillis = 1_000,
                elapsedRealtimeMillis = 240,
                durationMillis = 3_000,
            ),
        ).isEqualTo(1_240)
        assertThat(
            interpolatedDesktopLyricsPosition(
                positionAnchorMillis = 2_900,
                elapsedRealtimeMillis = 240,
                durationMillis = 3_000,
            ),
        ).isEqualTo(3_000)
    }

    @Test
    fun fixedControllerWindowKeepsTheLyricsAnchorAtTheSameScreenPosition() {
        val anchorY = 420
        val controlsInset = 132

        val expandedWindowY = desktopLyricsWindowY(anchorY, controlsInset)

        assertThat(expandedWindowY).isEqualTo(288)
        assertThat(desktopLyricsAnchorY(expandedWindowY, controlsInset)).isEqualTo(anchorY)
    }
}
