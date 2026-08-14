package com.resonote.feature.player.impl

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
    fun controls_dispatchIndependentActions() {
        var opens = 0
        var toggles = 0
        var next = 0
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
                    ),
                    onOpenPlayer = { opens++ },
                    onTogglePlay = { toggles++ },
                    onNext = { next++ },
                    onOpenQueue = { queue++ },
                    modifier = Modifier.onSizeChanged { miniPlayerHeightPx = it.height },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Pause").performClick()
        composeRule.onNodeWithContentDescription("Next").performClick()
        composeRule.onNodeWithContentDescription("Queue").performClick()

        assertEquals(0, opens)
        composeRule.onNodeWithText("静默轨道").performClick()

        composeRule.onNodeWithTag("resonote-mini-player-artwork", useUnmergedTree = true)
            .assertWidthIsEqualTo(56.dp)
            .assertHeightIsEqualTo(56.dp)
        composeRule.onNodeWithText("LOSSLESS").assertExists()
        composeRule.onNodeWithText("VIP").assertExists()
        assertEquals(with(composeRule.density) { 72.dp.roundToPx() }, miniPlayerHeightPx)
        assertEquals(1, toggles)
        assertEquals(1, next)
        assertEquals(1, queue)
        assertEquals(1, opens)
    }
}
