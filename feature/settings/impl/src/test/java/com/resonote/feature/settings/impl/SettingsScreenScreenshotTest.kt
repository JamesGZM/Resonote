package com.resonote.feature.settings.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.PlaybackSpeed
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
class SettingsScreenScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settings_compactPlaybackPreference() {
        setScreen()

        composeRule.onNodeWithTag("settings-playback-speed").assertIsDisplayed()
        capture("playback")
    }

    @Test
    fun playbackSpeedDialogDispatchesSelection() {
        var selected: PlaybackSpeed? = null
        setScreen(onPlaybackSpeedChange = { selected = it })

        composeRule.onNodeWithText("Playback speed").performClick()
        composeRule.onNodeWithText("1.5×").performClick()

        assertThat(selected).isEqualTo(PlaybackSpeed.OneAndHalf)
    }

    @Test
    fun backActionRemainsReachable() {
        var backClicks = 0
        setScreen(onBack = { backClicks++ })

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertThat(backClicks).isEqualTo(1)
    }

    @Test
    fun dynamicColorOnlyAppearsWhenPlatformSupportsIt() {
        setScreen(supportsDynamicColor = false)

        composeRule.onNodeWithTag("settings-dynamic-color").assertDoesNotExist()
    }

    private fun setScreen(
        onBack: () -> Unit = {},
        onPlaybackSpeedChange: (PlaybackSpeed) -> Unit = {},
        supportsDynamicColor: Boolean = true,
    ) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    SettingsScreen(
                        state = SettingsUiState.Ready(PlaybackSpeed.Normal),
                        onBack = onBack,
                        onRetry = {},
                        onPlaybackSpeedChange = onPlaybackSpeedChange,
                        supportsDynamicColor = supportsDynamicColor,
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/Settings/SettingsCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }
}
