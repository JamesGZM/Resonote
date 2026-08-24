package com.resonote.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
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
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SelectionPillTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tabbedToolbar_updatesHoistedSelection_andUsesEqualWidths() {
        var selectedIndex by mutableIntStateOf(0)
        composeRule.setContent {
            ResonoteTheme {
                ResonoteTabbedToolbar(
                    labels = listOf("Playlists", "Rankings", "Albums", "Songs"),
                    selectedIndex = selectedIndex,
                    onSelected = { selectedIndex = it },
                    windowInsets = WindowInsets(0),
                )
            }
        }

        val firstWidth = composeRule.onNodeWithText("Playlists").fetchSemanticsNode().boundsInRoot.width
        val lastWidth = composeRule.onNodeWithText("Songs").fetchSemanticsNode().boundsInRoot.width
        assertEquals(firstWidth, lastWidth, 0.5f)

        composeRule.onNodeWithText("Playlists").assertIsSelected()
        composeRule.onNodeWithText("Albums").assertIsNotSelected().performClick().assertIsSelected()
        assertEquals(2, selectedIndex)
    }

    @Test
    fun filterPill_exposesSelectionAndMinimumTouchTarget() {
        var selectedIndex by mutableIntStateOf(0)
        composeRule.setContent {
            ResonoteTheme {
                Row(Modifier.selectableGroup()) {
                    ResonoteFilterPill(
                        label = "Recommended",
                        selected = selectedIndex == 0,
                        onClick = { selectedIndex = 0 },
                    )
                    ResonoteFilterPill(
                        label = "Pop",
                        selected = selectedIndex == 1,
                        onClick = { selectedIndex = 1 },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Recommended").assertIsSelected()
        composeRule.onNodeWithText("Pop")
            .assertIsNotSelected()
            .assertTouchHeightIsEqualTo(48.dp)
            .performClick()
            .assertIsSelected()
        assertEquals(1, selectedIndex)
    }
}
