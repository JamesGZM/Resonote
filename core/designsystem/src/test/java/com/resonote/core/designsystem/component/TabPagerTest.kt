package com.resonote.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.resonote.core.designsystem.theme.ResonoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TabPagerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun swipeAndExternalSelection_keepPageStateInSync() {
        var selectedPage by mutableIntStateOf(0)
        composeRule.setContent {
            ResonoteTheme {
                ResonoteTabPager(
                    selectedPage = selectedPage,
                    pageCount = 4,
                    onPageSelected = { selectedPage = it },
                    modifier = Modifier.testTag("tab-pager"),
                ) { page ->
                    Text("Page $page")
                }
            }
        }

        composeRule.onNodeWithTag("tab-pager").performTouchInput { swipeLeft() }
        composeRule.waitUntil { selectedPage == 1 }
        composeRule.onNodeWithText("Page 1").assertExists()

        composeRule.runOnIdle { selectedPage = 3 }
        composeRule.waitUntil { composeRule.onAllNodesWithText("Page 3").fetchSemanticsNodes().isNotEmpty() }
        composeRule.runOnIdle { assertEquals(3, selectedPage) }
    }

    @Test
    fun externalSelectionImmediatelyAfterComposition_isNotOverwrittenByInitialPage() {
        var selectedPage by mutableIntStateOf(0)
        composeRule.setContent {
            ResonoteTheme {
                ResonoteTabPager(
                    selectedPage = selectedPage,
                    pageCount = 4,
                    onPageSelected = { selectedPage = it },
                ) { page ->
                    Text("Page $page")
                }
            }
        }

        composeRule.runOnIdle { selectedPage = 1 }

        composeRule.waitUntil { composeRule.onAllNodesWithText("Page 1").fetchSemanticsNodes().isNotEmpty() }
        composeRule.runOnIdle { assertEquals(1, selectedPage) }
    }
}
