package com.resonote.feature.settings.impl

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.LyricsPreferencesRepository
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.AppRelease
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.EqualizerPreset
import com.resonote.core.model.LyricsPreferences
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.playback.MusicDownload
import com.resonote.core.playback.MusicDownloadState
import com.resonote.core.screenshottesting.DefaultRoborazziOptions
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun downloadManagementShowsTaskStates() {
        composeRule.setContent {
            ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                DownloadManagementScreen(
                    downloads = listOf(
                        download("one", "Northbound", MusicDownloadState.Downloading, 42f),
                        download("two", "Flight Mode", MusicDownloadState.Completed),
                        download("three", "Signals", MusicDownloadState.Failed),
                    ),
                    onBack = {},
                    onPause = {},
                    onResume = {},
                    onRetry = {},
                    onRemove = {},
                    onPauseAll = {},
                    onResumeAll = {},
                )
            }
        }

        composeRule.onNodeWithTag("download-management-list").assertIsDisplayed()
        capture("downloads")
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
        composeRule.onNodeWithTag("settings-equalizer").assertExists()
        capture("playback")
    }

    @Test
    fun playbackSettingsOpensEqualizerPresets() {
        var clicks = 0
        setPlaybackScreen(onEqualizerClick = { clicks++ })

        composeRule.onNodeWithTag("settings-equalizer").performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun equalizerPresetPage() {
        setEqualizerScreen()

        composeRule.onNodeWithTag("equalizer-high").assertIsDisplayed()
        capture("equalizer")
    }

    @Test
    fun equalizerPresetPageRespectsMiniPlayerInset() {
        setEqualizerScreen(bottomContentPadding = 120.dp)

        composeRule.onNodeWithTag("equalizer-high").performScrollTo().assertIsDisplayed()
        val rootBottom = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.bottom
        val highBottom = composeRule.onNodeWithTag("equalizer-high").fetchSemanticsNode().boundsInRoot.bottom
        val miniPlayerInset = with(composeRule.density) { 120.dp.toPx() }
        assertThat(rootBottom - highBottom).isAtLeast(miniPlayerInset)
        capture("equalizer-mini-player-inset")
    }

    @Test
    fun equalizerPresetPageDispatchesSelection() {
        var selected: EqualizerPreset? = null
        setEqualizerScreen(onPresetChange = { selected = it })

        composeRule.onNodeWithText("Rock").performClick()

        assertThat(selected).isEqualTo(EqualizerPreset.Rock)
    }

    @Test
    fun equalizerKnobDispatchesUpdatedGains() {
        var gains: Triple<Int, Int, Int>? = null
        setEqualizerScreen(onGainsChange = { low, mid, high -> gains = Triple(low, mid, high) })

        composeRule.onNodeWithTag("equalizer-low").performSemanticsAction(SemanticsActions.SetProgress) {
            it(3f)
        }

        assertThat(gains).isEqualTo(Triple(3, 0, -1))
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
    fun lyricsSupplementalSheetUsesTwoIndependentEnabledSwitches() {
        var translationEnabled: Boolean? = null
        var transliterationEnabled: Boolean? = null
        composeRule.setContent {
            ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                LyricsSupplementalTextSheet(
                    translationEnabled = true,
                    transliterationEnabled = true,
                    onTranslationEnabledChange = { translationEnabled = it },
                    onTransliterationEnabledChange = { transliterationEnabled = it },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithTag("lyrics-translation-switch").assertIsOn().performClick()
        composeRule.onNodeWithTag("lyrics-transliteration-switch").assertIsOn().performClick()

        assertThat(translationEnabled).isFalse()
        assertThat(transliterationEnabled).isFalse()
    }

    @Test
    fun lyricsSettingsCompactLinksToDesktopLyricsSettings() {
        var desktopLyricsClicks = 0
        val repository = object : LyricsPreferencesRepository {
            override val preferences = MutableStateFlow(LyricsPreferences())

            override suspend fun setPreferences(value: LyricsPreferences) {
                preferences.value = value
            }

            override suspend fun reset() {
                preferences.value = LyricsPreferences()
            }
        }
        val viewModel = LyricsSettingsViewModel(repository)
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    LyricsSettingsRoute(
                        onBack = {},
                        onDesktopLyricsClick = { desktopLyricsClicks++ },
                        viewModel = viewModel,
                    )
                }
            }
        }

        composeRule.onNodeWithTag("desktop-lyrics-settings").assertIsDisplayed().performClick()
        assertThat(desktopLyricsClicks).isEqualTo(1)
        capture("lyrics")
    }

    @Test
    fun desktopLyricsSettingsCompactShowsControllerBehavior() {
        val repository = object : LyricsPreferencesRepository {
            override val preferences = MutableStateFlow(LyricsPreferences())

            override suspend fun setPreferences(value: LyricsPreferences) {
                preferences.value = value
            }

            override suspend fun reset() {
                preferences.value = LyricsPreferences()
            }
        }
        val viewModel = LyricsSettingsViewModel(repository)
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    DesktopLyricsSettingsRoute(
                        onBack = {},
                        viewModel = viewModel,
                    )
                }
            }
        }

        composeRule.onNodeWithTag("desktop-lyrics-switch").assertIsDisplayed()
        composeRule.onNodeWithText("#AE2A4B").assertDoesNotExist()
        composeRule.onNodeWithText("#000000").assertDoesNotExist()
        composeRule.onNodeWithText("Overall opacity").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop-lyrics-opacity").assertExists()
        composeRule.onNodeWithTag("desktop-lyrics-background-color").assertExists()
        composeRule.onNodeWithTag("desktop-lyrics-foreground-color").assertExists()
        composeRule.onNodeWithTag("desktop-lyrics-shadow").assertExists()
        composeRule.onNodeWithTag("desktop-lyrics-width-knob").assertDoesNotExist()
        composeRule.onNodeWithTag("desktop-lyrics-font-size-knob").assertDoesNotExist()
        composeRule.onNodeWithTag("desktop-lyrics-outline-width-knob").assertDoesNotExist()
        composeRule.onNodeWithTag("desktop-lyrics-shadow-x-knob").assertDoesNotExist()
        composeRule.onNodeWithText("Controller behavior").assertDoesNotExist()
        capture("desktop-lyrics")
    }

    @Test
    fun desktopLyricsSettingsUsesOpacityKnob() {
        val repository = lyricsPreferencesRepository()
        setDesktopLyricsScreen(repository)

        composeRule.onNodeWithTag("desktop-lyrics-opacity").performClick()
        composeRule.onNodeWithTag("desktop-lyrics-opacity-knob")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "100%"))
        composeRule.onNodeWithTag("desktop-lyrics-restore-default").assertIsDisplayed()
        capture("desktop-lyrics-opacity")

        composeRule.onNodeWithTag("desktop-lyrics-opacity-knob")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(64f) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop-lyrics-opacity-knob")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "64%"))
        assertThat(repository.preferences.value.desktopLyricsSurfaceOpacity).isEqualTo(64)
    }

    @Test
    fun desktopLyricsSettingsUsesWidthKnob() {
        val repository = lyricsPreferencesRepository()
        setDesktopLyricsScreen(repository)

        composeRule.onNodeWithTag("desktop-lyrics-width").performClick()
        composeRule.onNodeWithTag("desktop-lyrics-width-knob")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "100"))
        composeRule.onNodeWithTag("desktop-lyrics-restore-default").assertIsDisplayed()
        capture("desktop-lyrics-width")

        composeRule.onNodeWithTag("desktop-lyrics-width-knob")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(71f) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop-lyrics-width-knob")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "71"))
        assertThat(repository.preferences.value.desktopLyricsWidthPercent).isEqualTo(71)

        composeRule.onNodeWithTag("desktop-lyrics-restore-default").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop-lyrics-width-knob")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "100"))
        assertThat(repository.preferences.value.desktopLyricsWidthPercent).isEqualTo(100)
    }

    @Test
    fun desktopLyricsSettingsUsesFontSizeKnob() {
        val repository = lyricsPreferencesRepository()
        setDesktopLyricsScreen(repository)

        composeRule.onNodeWithTag("desktop-lyrics-font-size").performClick()
        composeRule.onNodeWithTag("desktop-lyrics-font-size-knob")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "24"))
        composeRule.onNodeWithTag("desktop-lyrics-restore-default").assertIsDisplayed()
        capture("desktop-lyrics-font-size")

        composeRule.onNodeWithTag("desktop-lyrics-font-size-knob")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(31f) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop-lyrics-font-size-knob")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "31"))
        assertThat(repository.preferences.value.desktopLyricsFontSizeSp).isEqualTo(31)
    }

    @Test
    fun desktopLyricsSettingsUsesOutlineKnob() {
        val repository = lyricsPreferencesRepository()
        setDesktopLyricsScreen(repository)

        composeRule.onNodeWithTag("desktop-lyrics-outline").performClick()
        composeRule.onNodeWithTag("desktop-lyrics-outline-color-picker").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop-lyrics-restore-default").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop-lyrics-outline-width-knob")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "0.0"))
        capture("desktop-lyrics-outline")

        composeRule.onNodeWithTag("desktop-lyrics-outline-width-knob")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(2.3f) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop-lyrics-outline-width-knob")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "2.3"))
        assertThat(repository.preferences.value.desktopLyricsOutlineWidthDp).isWithin(0.001f).of(2.3f)

        composeRule.onNodeWithTag("desktop-lyrics-outline-color-picker").performClick()

        composeRule.onNodeWithTag("desktop-lyrics-shadow-palette").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop-lyrics-outline-sheet").assertExists()
        composeRule.onNodeWithTag("desktop-lyrics-color-restore-default").assertDoesNotExist()
    }

    @Test
    fun desktopLyricsSettingsOpensShadowControlsAndColorPalette() {
        val repository = object : LyricsPreferencesRepository {
            override val preferences = MutableStateFlow(LyricsPreferences())

            override suspend fun setPreferences(value: LyricsPreferences) {
                preferences.value = value
            }

            override suspend fun reset() {
                preferences.value = LyricsPreferences()
            }
        }
        val viewModel = LyricsSettingsViewModel(repository)
        composeRule.setContent {
            ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                DesktopLyricsSettingsRoute(onBack = {}, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithTag("desktop-lyrics-shadow").performScrollTo().performClick()

        composeRule.onNodeWithTag("desktop-lyrics-shadow-x-knob")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "0.0"))
        composeRule.onNodeWithTag("desktop-lyrics-restore-default").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop-lyrics-shadow-y-knob")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "1.0"))
        composeRule.onNodeWithTag("desktop-lyrics-shadow-softness-knob")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "2.0"))
        capture("desktop-lyrics-shadow")

        composeRule.onNodeWithTag("desktop-lyrics-shadow-x-knob")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(-3.7f) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop-lyrics-shadow-x-knob")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "-3.7"))
        assertThat(repository.preferences.value.desktopLyricsShadowOffsetXDp).isWithin(0.001f).of(-3.7f)

        composeRule.onNodeWithTag("desktop-lyrics-shadow-color-picker").performClick()

        composeRule.onNodeWithTag("desktop-lyrics-shadow-palette").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop-lyrics-shadow-sheet").assertExists()
        composeRule.onNodeWithTag("desktop-lyrics-color-restore-default").assertDoesNotExist()
        composeRule.onNodeWithText("#000000").assertDoesNotExist()
    }

    @Test
    fun desktopLyricsPrimaryColorAndTimeoutSheetsShowRestoreDefault() {
        val repository = lyricsPreferencesRepository()
        setDesktopLyricsScreen(repository)

        composeRule.onNodeWithTag("desktop-lyrics-background-color").performClick()
        composeRule.onNodeWithTag("desktop-lyrics-color-restore-default").assertIsDisplayed()
        capture("desktop-lyrics-background-color")
        composeRule.onNodeWithText("Done").performClick()

        composeRule.onNodeWithTag("desktop-lyrics-controls-timeout").performScrollTo().performClick()
        composeRule.onNodeWithTag("desktop-lyrics-restore-default").assertIsDisplayed()
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
        onEqualizerClick: () -> Unit = {},
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
                        onEqualizerClick = onEqualizerClick,
                    )
                }
            }
        }
    }

    private fun setEqualizerScreen(
        onPresetChange: (EqualizerPreset) -> Unit = {},
        onGainsChange: (Int, Int, Int) -> Unit = { _, _, _ -> },
        bottomContentPadding: Dp = 32.dp,
    ) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    EqualizerSettingsScreen(
                        state = SettingsUiState.Ready(
                            playbackSpeed = PlaybackSpeed.Normal,
                            equalizerEnabled = true,
                            equalizerLowDb = 6,
                            equalizerMidDb = 0,
                            equalizerHighDb = -1,
                            equalizerCustom = true,
                        ),
                        onBack = {},
                        onRetry = {},
                        onPresetChange = onPresetChange,
                        onGainsChange = onGainsChange,
                        bottomContentPadding = bottomContentPadding,
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

    private fun download(id: String, title: String, state: MusicDownloadState, progress: Float? = null) = MusicDownload(
        id = "download:$id",
        song = OnlineSong(
            hash = id,
            title = title,
            artist = "Resonote",
            coverUrl = null,
            albumId = null,
            albumAudioId = null,
            durationMillis = 180_000,
            quality = AudioQuality.Lossless,
            vip = false,
        ),
        quality = OnlinePlaybackQuality.Lossless,
        sourceUri = "https://example.test/$id.flac",
        extension = "flac",
        state = state,
        progressPercent = progress,
        bytesDownloaded = 24_000_000,
        totalBytes = 48_000_000,
        updatedAtEpochMillis = 1_723_456_789,
    )

    private fun lyricsPreferencesRepository() = TestLyricsPreferencesRepository()

    private class TestLyricsPreferencesRepository : LyricsPreferencesRepository {
        override val preferences = MutableStateFlow(LyricsPreferences())

        override suspend fun setPreferences(value: LyricsPreferences) {
            preferences.value = value
        }

        override suspend fun reset() {
            preferences.value = LyricsPreferences()
        }
    }

    private fun setDesktopLyricsScreen(repository: LyricsPreferencesRepository) {
        val viewModel = LyricsSettingsViewModel(repository)
        composeRule.setContent {
            ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                DesktopLyricsSettingsRoute(onBack = {}, viewModel = viewModel)
            }
        }
    }
}
