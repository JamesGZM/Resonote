package com.resonote.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.theme.ResonoteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun enabledButton_performsClick_andMeetsMinimumSize() {
        var clicks = 0
        composeRule.setContent {
            ResonoteTheme {
                ResonoteButton(label = "Play", onClick = { clicks++ })
            }
        }

        val button = composeRule.onNodeWithText("Play")
            .assertIsEnabled()
            .assertWidthIsAtLeast(58.dp)
            .assertHeightIsAtLeast(40.dp)
            .performClick()

        assertEquals(1, clicks)
        assertTouchTarget(button.fetchSemanticsNode().touchBoundsInRoot.width, 48f)
        assertTouchTarget(button.fetchSemanticsNode().touchBoundsInRoot.height, 48f)
    }

    @Test
    fun disabledAndLoadingButtons_doNotPerformClick() {
        var clicks = 0
        composeRule.setContent {
            ResonoteTheme {
                Box {
                    ResonoteButton(
                        label = "Disabled",
                        onClick = { clicks++ },
                        enabled = false,
                    )
                    ResonoteTonalButton(
                        label = "Save",
                        loadingLabel = "Saving…",
                        onClick = { clicks++ },
                        loading = true,
                    )
                }
            }
        }

        composeRule.onNodeWithText("Disabled").assertIsNotEnabled().performClick()
        composeRule.onNode(hasStateDescription("Saving…")).assertIsNotEnabled().performClick()

        assertEquals(0, clicks)
    }

    @Test
    fun leadingIcon_isConstrainedTo18Dp() {
        composeRule.setContent {
            ResonoteTheme {
                ResonoteButton(
                    label = "Add",
                    onClick = {},
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("leading-icon"),
                        )
                    },
                )
            }
        }

        composeRule.onNodeWithTag("leading-icon", useUnmergedTree = true)
            .assertWidthIsAtLeast(18.dp)
            .assertHeightIsAtLeast(18.dp)
    }

    @Test
    fun fontScale200_preservesMinimumTouchTarget() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.FontScale(2f),
            ) {
                ResonoteTheme {
                    ResonoteButton(
                        label = "Play selected album",
                        onClick = {},
                    )
                }
            }
        }

        val button = composeRule.onNodeWithText("Play selected album")
        assertTouchTarget(button.fetchSemanticsNode().touchBoundsInRoot.width, 48f)
        assertTouchTarget(button.fetchSemanticsNode().touchBoundsInRoot.height, 48f)
    }

    @Test
    fun iconButton_hasAccessibleNameAndMinimumTouchTarget() {
        composeRule.setContent {
            ResonoteTheme {
                ResonoteIconButton(
                    label = "Add",
                    onClick = {},
                    icon = { Box(Modifier.fillMaxSize()) },
                )
            }
        }

        val iconButton = composeRule.onNodeWithContentDescription("Add")
            .assertIsEnabled()
            .assertWidthIsAtLeast(40.dp)
            .assertHeightIsAtLeast(40.dp)
        assertTouchTarget(iconButton.fetchSemanticsNode().touchBoundsInRoot.width, 48f)
        assertTouchTarget(iconButton.fetchSemanticsNode().touchBoundsInRoot.height, 48f)
    }

    @Test
    fun uncheckedToggleIconButton_exposesUncheckedState() {
        composeRule.setContent {
            ResonoteTheme {
                ResonoteIconToggleButton(
                    checked = false,
                    label = "Favorite",
                    onCheckedChange = {},
                    icon = { Box(Modifier.fillMaxSize()) },
                    checkedIcon = { Box(Modifier.fillMaxSize()) },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Favorite").assertIsOff()
    }

    @Test
    fun checkedToggleIconButton_exposesCheckedState() {
        composeRule.setContent {
            ResonoteTheme {
                ResonoteIconToggleButton(
                    checked = true,
                    label = "Favorite",
                    onCheckedChange = {},
                    icon = { Box(Modifier.fillMaxSize()) },
                    checkedIcon = { Box(Modifier.fillMaxSize()) },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Favorite").assertIsOn()
    }

    private fun assertTouchTarget(sizePx: Float, minimumDp: Float) {
        assertTrue(
            "Touch target was ${sizePx / composeRule.density.density}dp, expected at least ${minimumDp}dp",
            sizePx / composeRule.density.density >= minimumDp,
        )
    }
}
