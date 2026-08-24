package com.resonote.feature.risk.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.Locales
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.then
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
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
class RiskVerificationScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun riskVerification_loadingUsesBasePageAndToolbar() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.Locales(LocaleList(Locale("zh-CN"))) then
                    DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    RiskVerificationScreen(
                        state = RiskVerificationUiState.Loading,
                        onBack = {},
                        onSmsCodeChanged = {},
                        onSubmitSms = {},
                        onTencentProof = { _, _, _ -> },
                        onTencentFailure = {},
                        onRetry = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("安全验证").assertExists()
        composeRule.onNodeWithContentDescription("返回").assertExists()
        composeRule.onNodeWithText("此操作需要完成安全验证").assertExists()
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/RiskVerification/RiskVerificationCompact_loading.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }
}
