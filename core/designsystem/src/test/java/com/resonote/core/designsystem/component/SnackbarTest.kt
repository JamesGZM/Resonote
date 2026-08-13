package com.resonote.core.designsystem.component

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SnackbarTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var hostState: SnackbarHostState
    private lateinit var controller: ResonoteSnackbarController

    @Before
    fun setUp() {
        composeRule.setContent {
            hostState = remember { SnackbarHostState() }
            controller = rememberResonoteSnackbarController(hostState)
            ResonoteSnackbarHost(hostState)
        }
    }

    @Test
    fun repeatedMessageRestartsCurrentDisplayInsteadOfQueuing() {
        var firstResultCount = 0
        var latestResultCount = 0
        composeRule.runOnIdle {
            controller.show(
                message = "Unavailable",
                duration = SnackbarDuration.Indefinite,
                onResult = { firstResultCount++ },
            )
        }
        composeRule.onNodeWithText("Unavailable").assertIsDisplayed()

        composeRule.runOnIdle {
            repeat(9) {
                controller.show(
                    message = "Unavailable",
                    duration = SnackbarDuration.Indefinite,
                    onResult = { latestResultCount++ },
                )
            }
        }
        composeRule.onNodeWithText("Unavailable").assertIsDisplayed()

        composeRule.runOnIdle { hostState.currentSnackbarData?.dismiss() }
        composeRule.waitUntil { hostState.currentSnackbarData == null }

        composeRule.onNodeWithText("Unavailable").assertDoesNotExist()
        composeRule.runOnIdle {
            assertThat(firstResultCount).isEqualTo(0)
            assertThat(latestResultCount).isEqualTo(1)
        }
    }

    @Test
    fun newerMessageReplacesCurrentMessage() {
        composeRule.runOnIdle {
            controller.show("First", duration = SnackbarDuration.Indefinite)
        }
        composeRule.onNodeWithText("First").assertIsDisplayed()

        composeRule.runOnIdle {
            controller.show("Latest", duration = SnackbarDuration.Indefinite)
        }

        composeRule.onNodeWithText("First").assertDoesNotExist()
        composeRule.onNodeWithText("Latest").assertIsDisplayed()
    }

    @Test
    fun ordinaryFeedbackDefaultsToShortDuration() {
        composeRule.runOnIdle { controller.show("Saved") }
        composeRule.waitUntil { hostState.currentSnackbarData != null }

        composeRule.runOnIdle {
            assertThat(hostState.currentSnackbarData?.visuals?.duration)
                .isEqualTo(SnackbarDuration.Short)
        }
    }
}
