package com.resonote.feature.vip.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.screenshottesting.DefaultRoborazziOptions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-420dpi")
class DailyVipScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dailyVip_ready() {
        setScreen(DailyVipUiState.Ready("2026-08-13"))

        composeRule.onNodeWithTag("daily-vip-dialog").assertIsDisplayed()
        capture("ready")
    }

    @Test
    fun dailyVip_upgradeChoice() {
        setScreen(DailyVipUiState.UpgradeChoice("2026-08-13", alreadyClaimed = false))

        composeRule.onNodeWithText("升级为概念版 VIP？").assertIsDisplayed()
        composeRule.onNodeWithText("继续升级").assertIsDisplayed()
        capture("upgrade_choice")
    }

    @Test
    fun dailyVip_riskBlocked() {
        setScreen(DailyVipUiState.RiskBlocked("2026-08-13"))

        composeRule.onNodeWithText("需要在官方客户端处理").assertIsDisplayed()
        capture("risk_blocked")
    }

    private fun setScreen(state: DailyVipUiState) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    DailyVipDialog(
                        state = state,
                        onDismiss = {},
                        onClaim = {},
                        onUpgrade = {},
                        onDeclineUpgrade = {},
                        onRetry = {},
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/DailyVip/DailyVipCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }
}
