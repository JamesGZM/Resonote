package com.resonote.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
class PlainActionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun enabledAction_performsClick_andMeetsMinimumSize() {
        var clicks = 0
        composeRule.setContent {
            ResonoteTheme {
                ResonotePlainAction(onClick = { clicks++ }) { Text("Play all") }
            }
        }

        composeRule.onNodeWithText("Play all")
            .assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun disabledAction_doesNotPerformClick() {
        var clicks = 0
        composeRule.setContent {
            ResonoteTheme {
                Row {
                    ResonotePlainAction(
                        onClick = { clicks++ },
                        enabled = false,
                    ) {
                        Text("Unavailable")
                    }
                }
            }
        }

        composeRule.onNodeWithText("Unavailable")
            .assertIsNotEnabled()
            .performClick()

        assertEquals(0, clicks)
    }

    @Test
    fun action_exposesFocusRequest() {
        composeRule.setContent {
            ResonoteTheme {
                ResonotePlainAction(onClick = {}) {
                    Text("Focusable")
                }
            }
        }

        composeRule.onNodeWithText("Focusable")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.RequestFocus))
    }
}
