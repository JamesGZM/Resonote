package com.resonote.feature.player.impl

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
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
    fun controls_dispatchIndependentActions_withoutNextAction() {
        var opens = 0
        var toggles = 0
        var queue = 0
        var miniPlayerHeightPx = 0
        composeRule.setContent {
            ResonoteTheme {
                ResonoteMiniPlayer(
                    state = MiniPlayerUiState(
                        mediaId = "quiet-track",
                        title = "静默轨道",
                        artist = "Resonote Ensemble",
                        qualityLabel = "LOSSLESS",
                        isVip = true,
                        isPlaying = true,
                        progress = 0.4f,
                        artworkColors = listOf(Color.Red, Color.Magenta),
                    ),
                    onOpenPlayer = { opens++ },
                    onTogglePlay = { toggles++ },
                    onOpenQueue = { queue++ },
                    modifier = Modifier.onSizeChanged { miniPlayerHeightPx = it.height },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Pause").performClick()
        composeRule.onNodeWithContentDescription("Queue").performClick()

        assertEquals(0, opens)
        composeRule.onNodeWithText("静默轨道").performClick()

        composeRule.onNodeWithContentDescription("Next").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Artwork for 静默轨道", useUnmergedTree = true)
            .assertWidthIsEqualTo(56.dp)
            .assertHeightIsEqualTo(56.dp)
        composeRule.onNodeWithText("SQ · VIP").assertExists()
        composeRule.onNodeWithText("LOSSLESS").assertDoesNotExist()
        assertEquals(with(composeRule.density) { 72.dp.roundToPx() }, miniPlayerHeightPx)
        assertEquals(1, toggles)
        assertEquals(1, queue)
        assertEquals(1, opens)
    }
}
