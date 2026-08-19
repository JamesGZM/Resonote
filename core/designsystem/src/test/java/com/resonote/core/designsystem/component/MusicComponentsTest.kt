package com.resonote.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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

    @Test
    fun leadingContent_isRenderedInsideClickableItem() {
        var rowClicks = 0
        composeRule.setContent {
            ResonoteTheme {
                ResonoteMusicItem(
                    title = "榜单歌曲",
                    supportingText = "Resonote",
                    duration = "3:00",
                    onClick = { rowClicks++ },
                    onMoreClick = null,
                    leadingContent = { androidx.compose.material3.Text("1") },
                )
            }
        }

        composeRule.onNodeWithText("1").assertExists().performClick()

        assertEquals(1, rowClicks)
    }

    @Test
    fun customTrailingAction_replacesMoreAction_andRemainsIndependent() {
        var rowClicks = 0
        var moreClicks = 0
        var removeClicks = 0
        composeRule.setContent {
            ResonoteTheme {
                ResonoteMusicItem(
                    title = "待移除歌曲",
                    supportingText = "Resonote",
                    duration = "3:00",
                    onClick = { rowClicks++ },
                    onMoreClick = { moreClicks++ },
                    trailingAction = {
                        ResonoteIconButton(
                            label = "移除待移除歌曲",
                            onClick = { removeClicks++ },
                            icon = { Icon(Icons.Rounded.Close, contentDescription = null) },
                        )
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("More actions for 待移除歌曲").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("移除待移除歌曲").performClick()

        assertEquals(0, rowClicks)
        assertEquals(0, moreClicks)
        assertEquals(1, removeClicks)
    }

    @Test
    fun qualityAndVipLabels_areCombinedIntoCompactArtworkBadge() {
        composeRule.setContent {
            ResonoteTheme {
                ResonoteMusicItem(
                    title = "无损歌曲",
                    supportingText = "歌手",
                    duration = "4:26",
                    qualityLabel = "LOSSLESS",
                    isVip = true,
                    onClick = {},
                    onMoreClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("resonote-artwork-badge", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("SQ · VIP", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("LOSSLESS").assertDoesNotExist()
    }
}
