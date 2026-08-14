package com.resonote.core.designsystem.theme

import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ThemeInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun themeKeepsMaterialRippleEnabledGlobally() {
        var rippleConfiguration: Any? = null

        composeRule.setContent {
            ResonoteTheme {
                val currentRippleConfiguration = LocalRippleConfiguration.current
                SideEffect {
                    rippleConfiguration = currentRippleConfiguration
                }
            }
        }

        composeRule.runOnIdle {
            assertNotNull(rippleConfiguration)
        }
    }
}
