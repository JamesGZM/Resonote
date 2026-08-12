package com.resonote.feature.auth.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.AuthAccountOption
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
class LoginScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun login_mobileInitial() {
        setScreen(LoginUiState(), sessionExpired = false)

        composeRule.onNodeWithText("登录，继续你的声音旅程").assertExists()
        composeRule.onNodeWithText("登录").assertIsNotEnabled()
        capture("mobile")
    }

    @Test
    fun login_expiredWithAccountPicker() {
        setScreen(
            LoginUiState(
                mobile = "13800000000",
                code = "246810",
                accounts = listOf(
                    AuthAccountOption("42", "海岸线", null, "VIP 8"),
                    AuthAccountOption("84", "凌晨电台", null, null),
                ),
            ),
            sessionExpired = true,
        )

        composeRule.onNodeWithTag("account-picker").assertExists()
        composeRule.onNodeWithText("登录已过期").assertExists()
        composeRule.onNodeWithTag("login-scroll").performScrollToNode(hasTestTag("account-picker"))
        capture("accounts")
    }

    private fun setScreen(state: LoginUiState, sessionExpired: Boolean) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    LoginScreen(
                        state = state,
                        sessionExpired = sessionExpired,
                        onBack = {},
                        onMethodSelected = {},
                        onMobileChanged = {},
                        onCodeChanged = {},
                        onUsernameChanged = {},
                        onPasswordChanged = {},
                        onPasswordVisibilityToggle = {},
                        onSendCode = {},
                        onLogin = {},
                        onAccountSelected = {},
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/Login/LoginCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }
}
