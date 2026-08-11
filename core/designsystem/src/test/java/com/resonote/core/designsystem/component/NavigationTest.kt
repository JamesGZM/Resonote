package com.resonote.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
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
@Config(sdk = [35], qualifiers = "w1200dp-h1200dp-420dpi")
@OptIn(ExperimentalMaterial3Api::class)
class NavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun itemClick_updatesHoistedSelection_andSwitchesIcon() {
        var selectedIndex by mutableIntStateOf(0)
        composeRule.setContent {
            ResonoteTheme {
                NavigationExample(
                    selectedIndex = selectedIndex,
                    onSelected = { selectedIndex = it },
                    adaptiveInfo = adaptiveInfo(400.dp, 800.dp),
                )
            }
        }

        composeRule.onNodeWithText("Foundation").assertIsSelected()
        composeRule.onNodeWithText("Components").assertIsNotSelected().performClick()
        composeRule.onNodeWithText("Components").assertIsSelected()
        composeRule.onNodeWithTag("selected-icon-1", useUnmergedTree = true).assertExists()
        assertEquals(1, selectedIndex)
    }

    @Test
    fun windowChange_preservesDestinationsAndHoistedSelection() {
        var selectedIndex by mutableIntStateOf(0)
        var windowAdaptiveInfo by mutableStateOf(adaptiveInfo(400.dp, 800.dp))
        composeRule.setContent {
            ResonoteTheme {
                NavigationExample(
                    selectedIndex = selectedIndex,
                    onSelected = { selectedIndex = it },
                    adaptiveInfo = windowAdaptiveInfo,
                )
            }
        }

        composeRule.onNodeWithText("Components").performClick().assertIsSelected()
        composeRule.runOnIdle {
            windowAdaptiveInfo = adaptiveInfo(700.dp, 800.dp)
        }

        composeRule.onNodeWithText("Foundation").assertExists().assertIsNotSelected()
        composeRule.onNodeWithText("Components").assertExists().assertIsSelected()
        composeRule.onNodeWithText("Music").assertExists().assertIsNotSelected()
        assertEquals(1, selectedIndex)
    }

    @Test
    fun compactWindow_placesNavigationBelowContent() {
        setSizedNavigation(width = 400.dp, height = 800.dp)

        val navigationBounds = composeRule.onNodeWithTag("navigation-item-0").fetchSemanticsNode().boundsInRoot
        val contentBounds = composeRule.onNodeWithTag("navigation-content").fetchSemanticsNode().boundsInRoot

        assertTrue(navigationBounds.top >= contentBounds.bottom)
    }

    @Test
    fun mediumWindow_placesNavigationBeforeContent() {
        setSizedNavigation(width = 700.dp, height = 800.dp)

        val navigationBounds = composeRule.onNodeWithTag("navigation-item-0").fetchSemanticsNode().boundsInRoot
        val contentBounds = composeRule.onNodeWithTag("navigation-content").fetchSemanticsNode().boundsInRoot

        assertTrue(navigationBounds.right <= contentBounds.left)
    }

    @Test
    fun tabletopPosture_usesBottomNavigation() {
        setSizedNavigation(width = 1000.dp, height = 800.dp, posture = Posture(true, emptyList()))

        val navigationBounds = composeRule.onNodeWithTag("navigation-item-0").fetchSemanticsNode().boundsInRoot
        val contentBounds = composeRule.onNodeWithTag("navigation-content").fetchSemanticsNode().boundsInRoot

        assertTrue(navigationBounds.top >= contentBounds.bottom)
    }

    @Test
    fun topAppBar_actionsAreAccessibleAndMeetTouchTarget() {
        var clicks = 0
        composeRule.setContent {
            ResonoteTheme {
                ResonoteTopAppBar(
                    title = { Text("Library") },
                    navigationIcon = {
                        ResonoteIconButton(
                            label = "Back",
                            onClick = { clicks++ },
                            icon = { Box(Modifier.fillMaxSize()) },
                        )
                    },
                    actions = {
                        ResonoteIconButton(
                            label = "Settings",
                            onClick = { clicks++ },
                            icon = { Box(Modifier.fillMaxSize()) },
                        )
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back")
            .assertTouchWidthIsEqualTo(48.dp)
            .assertTouchHeightIsEqualTo(48.dp)
            .performClick()
        composeRule.onNodeWithContentDescription("Settings")
            .assertTouchWidthIsEqualTo(48.dp)
            .assertTouchHeightIsEqualTo(48.dp)
            .performClick()

        assertEquals(2, clicks)
    }

    private fun setSizedNavigation(width: Dp, height: Dp, posture: Posture = Posture()) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(width, height)),
            ) {
                ResonoteTheme {
                    NavigationExample(
                        selectedIndex = 0,
                        onSelected = {},
                        adaptiveInfo = adaptiveInfo(width, height, posture),
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun NavigationExample(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    adaptiveInfo: WindowAdaptiveInfo,
) {
    ResonoteNavigationSuiteScaffold(
        navigationSuiteItems = {
            listOf("Foundation", "Components", "Music").forEachIndexed { index, label ->
                item(
                    selected = selectedIndex == index,
                    onClick = { onSelected(index) },
                    modifier = Modifier.testTag("navigation-item-$index"),
                    icon = { TestNavigationIcon("icon-$index") },
                    selectedIcon = { TestNavigationIcon("selected-icon-$index") },
                    label = { Text(label) },
                )
            }
        },
        windowAdaptiveInfo = adaptiveInfo,
    ) {
        Box(Modifier.fillMaxSize().testTag("navigation-content"))
    }
}

@androidx.compose.runtime.Composable
private fun TestNavigationIcon(tag: String) {
    Icon(
        imageVector = ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).build(),
        contentDescription = null,
        modifier = Modifier.testTag(tag),
    )
}

@Suppress("DEPRECATION")
private fun adaptiveInfo(width: Dp, height: Dp, posture: Posture = Posture()) = WindowAdaptiveInfo(
    windowSizeClass = WindowSizeClass.compute(width.value, height.value),
    windowPosture = posture,
)
