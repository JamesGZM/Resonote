package com.resonote.core.playback.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.LyricLine
import com.resonote.core.model.LyricSyllable
import com.resonote.core.model.LyricsDocument
import com.resonote.core.model.PlaybackMode
import org.junit.Test

class DesktopLyricsRendererTest {
    @Test
    fun renderSelectsActiveLineAndHighlightsElapsedWords() {
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

        val content = DesktopLyricsRenderer.render(document, 1_600)

        assertThat(content).isEqualTo(
            DesktopLyricsContent(
                primary = "Hello world",
                primaryHighlightTextOffset = 7f,
            ),
        )
    }

    @Test
    fun lineTimedLyricsAdvanceAcrossTheCurrentLineDuration() {
        val document = LyricsDocument(
            listOf(
                LyricLine(
                    syllables = listOf(LyricSyllable("Line", 1_000, 2_000, phonetic = "Lain")),
                    translation = "翻译",
                ),
                LyricLine(2_000, "Next"),
            ),
        )

        val content = DesktopLyricsRenderer.render(document, 1_500)

        assertThat(content?.primary).isEqualTo("Line")
        assertThat(content?.primaryHighlightTextOffset).isEqualTo(2f)
    }

    @Test
    fun singleLineRemovesEmbeddedLineBreaksAndSupplementalRows() {
        val document = LyricsDocument(
            listOf(
                LyricLine(
                    syllables = listOf(LyricSyllable("女：\n下一句", 1_000, 2_000)),
                    translation = "Translation",
                ),
            ),
        )

        val content = DesktopLyricsRenderer.render(document, 1_500)

        assertThat(content?.primary).isEqualTo("女： 下一句")
        assertThat(content?.primary).doesNotContain("\n")
    }

    @Test
    fun longLineSegmentsAdvanceOneVisibleRowAtATime() {
        assertThat(desktopLyricsSegmentIndex(listOf(0, 8, 16), 0f)).isEqualTo(0)
        assertThat(desktopLyricsSegmentIndex(listOf(0, 8, 16), 7.9f)).isEqualTo(0)
        assertThat(desktopLyricsSegmentIndex(listOf(0, 8, 16), 8f)).isEqualTo(1)
        assertThat(desktopLyricsSegmentIndex(listOf(0, 8, 16), 20f)).isEqualTo(2)
    }

    @Test
    fun desktopControllerCyclesThroughEveryPlaybackMode() {
        assertThat(PlaybackMode.ListLoop.nextDesktopLyricsMode()).isEqualTo(PlaybackMode.Shuffle)
        assertThat(PlaybackMode.Shuffle.nextDesktopLyricsMode()).isEqualTo(PlaybackMode.SingleLoop)
        assertThat(PlaybackMode.SingleLoop.nextDesktopLyricsMode()).isEqualTo(PlaybackMode.Sequential)
        assertThat(PlaybackMode.Sequential.nextDesktopLyricsMode()).isEqualTo(PlaybackMode.ListLoop)
    }

    @Test
    fun blankTapTogglesControlsWithoutInvokingHiddenButtons() {
        assertThat(
            desktopLyricsTapOutcome(
                controlsWereVisible = false,
                pressedControl = false,
                releasedOnPressedControl = false,
                isLocked = false,
            ),
        ).isEqualTo(DesktopLyricsTapOutcome.ShowControls)
        assertThat(
            desktopLyricsTapOutcome(
                controlsWereVisible = true,
                pressedControl = false,
                releasedOnPressedControl = false,
                isLocked = false,
            ),
        ).isEqualTo(DesktopLyricsTapOutcome.HideControls)
    }

    @Test
    fun visibleControlRequiresPressAndReleaseOnTheSameTarget() {
        assertThat(
            desktopLyricsTapOutcome(
                controlsWereVisible = true,
                pressedControl = true,
                releasedOnPressedControl = true,
                isLocked = false,
            ),
        ).isEqualTo(DesktopLyricsTapOutcome.InvokeControl)
        assertThat(
            desktopLyricsTapOutcome(
                controlsWereVisible = true,
                pressedControl = true,
                releasedOnPressedControl = false,
                isLocked = false,
            ),
        ).isEqualTo(DesktopLyricsTapOutcome.KeepControls)
    }

    @Test
    fun lockedDesktopLyricsWindowPassesTouchesThrough() {
        assertThat(
            desktopLyricsWindowFlags(locked = true) and
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        ).isNotEqualTo(0)
        assertThat(
            desktopLyricsWindowFlags(locked = false) and
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        ).isEqualTo(0)
        assertThat(desktopLyricsWindowAlpha(locked = true, maximumObscuringOpacity = 0.8f)).isEqualTo(0.8f)
        assertThat(desktopLyricsWindowAlpha(locked = false, maximumObscuringOpacity = 0.8f)).isEqualTo(1f)
    }

    @Test
    fun desktopLyricsWindowStaysVisibleInsideTheAppWhenEnabledAndPermitted() {
        assertThat(desktopLyricsWindowShouldBeVisible(true, overlayPermissionGranted = true))
            .isTrue()
        assertThat(desktopLyricsWindowShouldBeVisible(false, overlayPermissionGranted = true))
            .isFalse()
        assertThat(desktopLyricsWindowShouldBeVisible(true, overlayPermissionGranted = false))
            .isFalse()
    }

    @Test
    fun lockedDesktopLyricsOnlyAllowsUnlockControl() {
        assertThat(
            DesktopLyricsControl.entries.filter {
                isDesktopLyricsControlAvailable(it, isLocked = true)
            },
        ).containsExactly(DesktopLyricsControl.Lock)
        assertThat(
            DesktopLyricsControl.entries.filter {
                isDesktopLyricsControlAvailable(it, isLocked = false)
            },
        ).containsExactlyElementsIn(DesktopLyricsControl.entries)
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
        assertThat(
            interpolatedDesktopLyricsPosition(
                positionAnchorMillis = 1_000,
                elapsedRealtimeMillis = 240,
                durationMillis = 3_000,
                visualLeadMillis = 260,
            ),
        ).isEqualTo(1_500)
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
