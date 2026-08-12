package com.resonote.core.designsystem.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
class MusicComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playingItem_replacesDuration_andKeepsMoreActionIndependent() {
        var rowClicks = 0
        var moreClicks = 0
        composeRule.setContent {
            ResonoteTheme {
                ResonoteMusicItem(
                    title = "静默轨道",
                    supportingText = "Resonote Ensemble",
                    duration = "4:12",
                    isPlaying = true,
                    onClick = { rowClicks++ },
                    onMoreClick = { moreClicks++ },
                )
            }
        }

        composeRule.onNodeWithText("4:12").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Playing").assertExists()
        composeRule.onNodeWithContentDescription("More actions for 静默轨道").performClick()

        assertEquals(0, rowClicks)
        assertEquals(1, moreClicks)
    }

    @Test
    fun missingArtwork_keepsSongContentAndDuration() {
        composeRule.setContent {
            ResonoteTheme {
                ResonoteMusicItem(
                    title = "未收录封面",
                    supportingText = "Resonote",
                    duration = "3:36",
                    artworkState = ResonoteArtworkState.MISSING,
                    onClick = {},
                    onMoreClick = {},
                )
            }
        }

        composeRule.onNodeWithText("未收录封面").assertExists()
        composeRule.onNodeWithText("3:36").assertExists()
    }

    @Test
    fun absentMoreCallback_removesUnavailableAction() {
        composeRule.setContent {
            ResonoteTheme {
                ResonoteMusicItem(
                    title = "无更多操作",
                    supportingText = "Resonote",
                    duration = "3:00",
                    onClick = {},
                    onMoreClick = null,
                )
            }
        }

        composeRule.onNodeWithContentDescription("More actions for 无更多操作").assertDoesNotExist()
    }
}
