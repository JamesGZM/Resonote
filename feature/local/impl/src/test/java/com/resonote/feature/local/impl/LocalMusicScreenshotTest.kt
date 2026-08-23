package com.resonote.feature.local.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaId
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
}
