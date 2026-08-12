package com.resonote.feature.player.impl

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.resonote.core.designsystem.theme.ResonoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MiniPlayerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun controls_dispatchIndependentActions() {
        var toggles = 0
        var next = 0
        var queue = 0
        composeRule.setContent {
            ResonoteTheme {
                ResonoteMiniPlayer(
                    state = MiniPlayerUiState(
                        mediaId = "quiet-track",
                        title = "静默轨道",
                        artist = "Resonote Ensemble",
                        isPlaying = true,
                        progress = 0.4f,
                        artworkColors = listOf(Color.Red, Color.Magenta),
                    ),
                    onOpenPlayer = {},
                    onTogglePlay = { toggles++ },
                    onNext = { next++ },
                    onOpenQueue = { queue++ },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Pause").performClick()
        composeRule.onNodeWithContentDescription("Next").performClick()
        composeRule.onNodeWithContentDescription("Queue").performClick()

        assertEquals(1, toggles)
        assertEquals(1, next)
        assertEquals(1, queue)
    }
}
