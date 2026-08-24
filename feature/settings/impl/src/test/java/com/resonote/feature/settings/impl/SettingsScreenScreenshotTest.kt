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
import com.resonote.core.model.AppRelease
import com.resonote.core.model.OnlinePlaybackQuality
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
    fun settings_compactHome() {
        setScreen()

        composeRule.onNodeWithTag("settings-playback").assertIsDisplayed()
        capture("home")
    }

    @Test
    fun appearanceSheetMatchesSettingsHierarchy() {
        setScreen()

        composeRule.onNodeWithTag("settings-appearance").performClick()
        composeRule.onNodeWithText("Choose how Resonote looks on this device").assertIsDisplayed()
        capture("appearance")
    }

    @Test
    fun languageSheetDispatchesSelection() {
        var selected: AppLanguage? = null
        setScreen(onLanguageChange = { selected = it })

        composeRule.onNodeWithTag("settings-language").performClick()
        composeRule.onNodeWithText("Simplified Chinese").performClick()

        assertThat(selected).isEqualTo(AppLanguage.SimplifiedChinese)
    }

    @Test
    fun authenticatedSettingsShowsLogoutAndConfirmsAction() {
        var logoutClicks = 0
        setScreen(isAuthenticated = true, onLogout = { logoutClicks++ })

        composeRule.onNodeWithTag("settings-logout").assertIsDisplayed()
        capture("home-authenticated")
        composeRule.onNodeWithTag("settings-logout").performClick()
        composeRule.onNodeWithTag("settings-logout-confirm").performClick()

        assertThat(logoutClicks).isEqualTo(1)
    }

    @Test
    fun playbackSettingsCompact() {
        setPlaybackScreen()

        composeRule.onNodeWithTag("settings-playback-speed").assertIsDisplayed()
        capture("playback")
    }

    @Test
    fun playbackSpeedDialogDispatchesSelection() {
        var selected: PlaybackSpeed? = null
        setPlaybackScreen(onPlaybackSpeedChange = { selected = it })

        composeRule.onNodeWithText("Playback speed").performClick()
        composeRule.onNodeWithText("1.5×").performClick()

        assertThat(selected).isEqualTo(PlaybackSpeed.OneAndHalf)
    }

    @Test
    fun onlineQualitySheetDispatchesSelection() {
        var selected: OnlinePlaybackQuality? = null
        setPlaybackScreen(onOnlinePlaybackQualityChange = { selected = it })

        composeRule.onNodeWithText("Online audio quality").performClick()
        composeRule.onNodeWithText("Lossless · FLAC").performClick()

        assertThat(selected).isEqualTo(OnlinePlaybackQuality.Lossless)
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

        composeRule.onNodeWithTag("settings-appearance").performClick()
        composeRule.onNodeWithText("Dynamic color").assertDoesNotExist()
    }

    @Test
    fun availableUpdateRowOpensRelease() {
        var updateClicks = 0
        composeRule.setContent {
            ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                AboutSettingsScreen(
                    version = "0.1.2",
                    updateState = AboutUpdateState.Available(
                        AppRelease("v0.2.0", "https://github.com/release"),
                    ),
                    onBack = {},
                    onProjectClick = {},
                    onUpdateClick = { updateClicks++ },
                )
            }
        }

        composeRule.onNodeWithTag("about-update-dot", useUnmergedTree = true).assertIsDisplayed()
        capture("about-update")
        composeRule.onNodeWithText("Version").performClick()

        assertThat(updateClicks).isEqualTo(1)
    }

    @Test
    fun checkingUpdateUsesRefreshLoadingIndicator() {
        composeRule.setContent {
            ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                AboutSettingsScreen(
                    version = "0.1.2",
                    updateState = AboutUpdateState.Checking,
                    onBack = {},
                    onProjectClick = {},
                    onUpdateClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Version").assertIsDisplayed()
        capture("about-loading")
    }

    @Test
    fun aboutRowsDispatchDetailNavigation() {
        var privacyClicks = 0
        var licenseClicks = 0
        var libraryClicks = 0
        composeRule.setContent {
            ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                AboutSettingsScreen(
                    version = "0.1.2",
                    updateState = AboutUpdateState.Latest("0.1.2"),
                    onBack = {},
                    onProjectClick = {},
                    onUpdateClick = {},
                    onPrivacyClick = { privacyClicks++ },
                    onLicenseClick = { licenseClicks++ },
                    onLibrariesClick = { libraryClicks++ },
                )
            }
        }

        composeRule.onNodeWithText("Privacy").performClick()
        composeRule.onNodeWithText("Open-source license").performClick()
        composeRule.onNodeWithText("Open-source libraries").performClick()

        assertThat(privacyClicks).isEqualTo(1)
        assertThat(licenseClicks).isEqualTo(1)
        assertThat(libraryClicks).isEqualTo(1)
    }

    @Test
    fun openSourceLibrariesCompact() {
        composeRule.setContent {
            ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                OpenSourceLibrariesRoute(onBack = {})
            }
        }

        composeRule.onNodeWithText("AndroidX & Jetpack Compose").assertIsDisplayed()
        capture("about-libraries")
    }

    private fun setScreen(
        onBack: () -> Unit = {},
        supportsDynamicColor: Boolean = true,
        isAuthenticated: Boolean = false,
        onLanguageChange: (AppLanguage) -> Unit = {},
        onLogout: () -> Unit = {},
    ) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    SettingsScreen(
                        state = SettingsUiState.Ready(
                            playbackSpeed = PlaybackSpeed.Normal,
                            isAuthenticated = isAuthenticated,
                        ),
                        onBack = onBack,
                        onRetry = {},
                        onLanguageChange = onLanguageChange,
                        onLogout = onLogout,
                        supportsDynamicColor = supportsDynamicColor,
                    )
                }
            }
        }
    }

    private fun setPlaybackScreen(
        onPlaybackSpeedChange: (PlaybackSpeed) -> Unit = {},
        onOnlinePlaybackQualityChange: (OnlinePlaybackQuality) -> Unit = {},
    ) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    PlaybackSettingsScreen(
                        state = SettingsUiState.Ready(PlaybackSpeed.Normal),
                        onBack = {},
                        onRetry = {},
                        onPlaybackSpeedChange = onPlaybackSpeedChange,
                        onOnlinePlaybackQualityChange = onOnlinePlaybackQualityChange,
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
