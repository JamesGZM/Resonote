package com.resonote.feature.local.impl

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.KaraokeMixSettings
import com.resonote.core.model.KaraokeProject
import com.resonote.core.model.KaraokeProjectId
import com.resonote.core.model.KaraokeProjectStatus
import com.resonote.core.model.KaraokeSourceMode
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaId
import com.resonote.core.screenshottesting.DefaultRoborazziOptions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-420dpi")
class LocalMusicScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun localMusic_empty() {
        setScreen(LocalMusicUiState(isLoading = false))
        composeRule.onNodeWithTag("resonote-empty-state").assertIsDisplayed()
        capture("empty")
    }

    @Test
    fun localMusic_contentAndImportResult() {
        setScreen(
            LocalMusicUiState(
                media = listOf(
                    media("one", "潮汐来信", "林澈", 96_000, 24),
                    media("two", "Northbound", "Mira", 44_100, 16),
                    media("three", "凌晨公路", null, 48_000, null),
                ),
                isLoading = false,
                importState = LocalImportUiState.Completed(
                    total = 4,
                    imported = 3,
                    skipped = 0,
                    failures = listOf(com.resonote.core.model.LocalMediaImportFailure.UnsupportedFormat),
                ),
            ),
            playingMediaId = "one",
        )
        capture("content")
    }

    @Test
    fun localMusic_karaokeWorks() {
        setScreen(
            LocalMusicUiState(
                isLoading = false,
                selectedTab = LocalMusicTab.KaraokeWorks,
                karaokeProjectsLoading = false,
                karaokeProjects = listOf(
                    KaraokeProject(
                        id = KaraokeProjectId("work"),
                        songHash = "hash",
                        songTitle = "潮汐记忆",
                        artist = "林澈",
                        artworkUri = null,
                        sourceMode = KaraokeSourceMode.Accompaniment,
                        trimStartMillis = 0,
                        status = KaraokeProjectStatus.Edited,
                        mixSettings = KaraokeMixSettings(),
                        durationMillis = 218_000,
                        createdAtEpochMillis = 1_723_456_789,
                        updatedAtEpochMillis = 1_723_456_789,
                        exportedContentUri = null,
                    ),
                ),
            ),
        )
        capture("karaoke_works")
    }

    @Test
    fun localMusic_karaokeEmptyUsesSharedContentState() {
        setScreen(
            LocalMusicUiState(
                isLoading = false,
                selectedTab = LocalMusicTab.KaraokeWorks,
                karaokeProjectsLoading = false,
            ),
        )

        composeRule.onNodeWithTag("resonote-empty-state").assertIsDisplayed()
        capture("karaoke_empty")
    }

    @Test
    fun karaokeMixEditor_keepsActionsAboveBottomContent() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    KaraokeMixEditorScreen(
                        project = karaokeProject(),
                        previewing = false,
                        onBack = {},
                        onPreview = {},
                        onSave = {},
                        bottomContentPadding = 80.dp,
                    )
                }
            }
        }
        composeRule.onNodeWithContentDescription("保存混音").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("试听效果").assertIsDisplayed()
        capture("karaoke_mix_editor")
    }

    @Test
    fun karaokeMixEditor_presetUpdatesBandsAndDraggingCreatesCustomMix() {
        var savedSettings: KaraokeMixSettings? = null
        composeRule.setContent {
            ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                KaraokeMixEditorScreen(
                    project = karaokeProject(),
                    previewing = false,
                    onBack = {},
                    onPreview = {},
                    onSave = { savedSettings = it },
                    bottomContentPadding = 0.dp,
                )
            }
        }

        composeRule.onNodeWithTag("karaoke-vocal-gain").performSemanticsAction(SemanticsActions.SetProgress) {
            it(2f)
        }
        composeRule.onNodeWithTag("karaoke-accompaniment-gain")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(-3f) }
        composeRule.onNodeWithTag("karaoke-eq-preset-Rock").performClick()
        composeRule.onNodeWithTag("karaoke-eq-low").performSemanticsAction(SemanticsActions.SetProgress) {
            it(1f)
        }
        composeRule.onNodeWithTag("karaoke-eq-mid").performSemanticsAction(SemanticsActions.SetProgress) {
            it(2f)
        }
        composeRule.onNodeWithTag("karaoke-eq-preset-Custom").assertIsSelected()
        composeRule.onNodeWithTag("karaoke-eq-high").performScrollTo().assertIsDisplayed()
        capture("karaoke_mix_editor_equalizer")
        composeRule.onNodeWithContentDescription("保存混音").performClick()

        composeRule.runOnIdle {
            assertEquals(2f, savedSettings?.vocalGainDb ?: Float.NaN, 0f)
            assertEquals(-3f, savedSettings?.accompanimentGainDb ?: Float.NaN, 0f)
            assertEquals(1f, savedSettings?.vocalLowEqDb ?: Float.NaN, 0f)
            assertEquals(2f, savedSettings?.vocalMidEqDb ?: Float.NaN, 0f)
            assertEquals(4f, savedSettings?.vocalHighEqDb ?: Float.NaN, 0f)
        }
    }

    private fun setScreen(state: LocalMusicUiState, playingMediaId: String? = null) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    LocalMusicScreen(
                        state = state,
                        playingMediaId = playingMediaId,
                        bottomContentPadding = 24.dp,
                        onBack = {},
                        onPickFiles = {},
                        onPickDirectory = {},
                        onQueryChange = {},
                        onSortChange = {},
                        onPlayAll = {},
                        onPlayMedia = {},
                        onCancelImport = {},
                        onResolveDuplicate = {},
                        onDismissImportResult = {},
                        onRequestDelete = {},
                        onDismissDelete = {},
                        onConfirmDelete = {},
                        onDismissDeleteFailure = {},
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/LocalMusic/LocalMusicCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    private fun media(id: String, title: String, artist: String?, sampleRate: Int, bitDepth: Int?) = LocalMedia(
        id = LocalMediaId(id),
        displayName = "$title.flac",
        title = title,
        artist = artist,
        albumTitle = "夜航集",
        artworkUri = null,
        durationMillis = 218_000,
        mimeType = "audio/flac",
        fileExtension = "flac",
        sizeBytes = 42_400_000,
        sampleRateHz = sampleRate,
        bitDepth = bitDepth,
        bitrateBitsPerSecond = 1_411_000,
        importedAtEpochMillis = 1_723_456_789,
    )

    private fun karaokeProject() = KaraokeProject(
        id = KaraokeProjectId("work"),
        songHash = "hash",
        songTitle = "潮汐记忆",
        artist = "林澈",
        artworkUri = null,
        sourceMode = KaraokeSourceMode.Accompaniment,
        trimStartMillis = 0,
        status = KaraokeProjectStatus.Edited,
        mixSettings = KaraokeMixSettings(),
        durationMillis = 218_000,
        createdAtEpochMillis = 1_723_456_789,
        updatedAtEpochMillis = 1_723_456_789,
        exportedContentUri = null,
    )
}
