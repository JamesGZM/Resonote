package com.resonote.app

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlineSong
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
class OnlineSongActionsScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun songActions_ownedPlaylistContext() {
        var addClicks = 0
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    OnlineSongActionsSheet(
                        request = OnlineSongActionRequest(song(), onRemoveRequest = {}),
                        onDismiss = {},
                        onPlay = {},
                        onAppendToQueue = {},
                        onAddToPlaylist = { addClicks += 1 },
                        onShowInfo = {},
                        onShareUnavailable = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("添加到歌单").assertExists()
        composeRule.onNodeWithText("从当前歌单移除").assertExists()
        composeRule.onNodeWithText("添加到歌单").performClick()
        assertThat(addClicks).isEqualTo(1)
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/SongActions/SongActionsCompact_owned.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    private fun song() = OnlineSong(
        hash = "evening-signal",
        title = "晚风信号",
        artist = "林澈 · 潮汐记忆",
        coverUrl = null,
        albumId = "album-1",
        albumAudioId = "audio-1",
        durationMillis = 248_000,
        quality = AudioQuality.HighResolution,
        vip = false,
        albumTitle = "潮汐记忆",
    )
}
