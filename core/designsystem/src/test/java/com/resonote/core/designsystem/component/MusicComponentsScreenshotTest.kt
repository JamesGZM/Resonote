package com.resonote.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.then
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
class MusicComponentsScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun musicItems_themeAndFontMatrix() {
        val cases = listOf(
            MusicScreenshotCase("light", ResonoteThemeMode.LIGHT),
            MusicScreenshotCase("dark", ResonoteThemeMode.DARK),
            MusicScreenshotCase("amoled", ResonoteThemeMode.AMOLED),
            MusicScreenshotCase("fontScale200", ResonoteThemeMode.LIGHT, 2f),
        )
        var currentCase by mutableStateOf(cases.first())
        composeRule.setContent {
            val configuration = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)) then
                DeviceConfigurationOverride.FontScale(currentCase.fontScale)
            DeviceConfigurationOverride(override = configuration) {
                ResonoteTheme(themeMode = currentCase.themeMode) {
                    Column {
                        ResonoteMusicItem(
                            title = "那些年我们一起听过的歌：夏日回忆特别版",
                            supportingText = "陈粒 · Resonote Ensemble · 特别合作艺术家",
                            duration = "5:41",
                            qualityLabel = "LOSSLESS",
                            isVip = true,
                            onClick = {},
                            onMoreClick = {},
                        )
                        ResonoteMusicItem(
                            title = "静默轨道",
                            supportingText = "Resonote Ensemble",
                            duration = "4:12",
                            qualityLabel = "HI-RES",
                            isVip = true,
                            isPlaying = true,
                            onClick = {},
                            onMoreClick = {},
                        )
                        ResonoteMusicItem(
                            title = "Loading",
                            supportingText = "Loading",
                            duration = "--:--",
                            artworkState = ResonoteArtworkState.LOADING,
                            onClick = {},
                            onMoreClick = {},
                        )
                        ResonoteMusicItem(
                            title = "未收录封面",
                            supportingText = "Resonote",
                            duration = "3:36",
                            artworkState = ResonoteArtworkState.MISSING,
                            onClick = {},
                            onMoreClick = {},
                        )
                    }
                }
            }
        }
        cases.forEach { case ->
            currentCase = case
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage(
                filePath = "src/test/screenshots/MusicComponents/MusicItems_${case.name}.png",
                roborazziOptions = DefaultRoborazziOptions,
            )
        }
    }

    @Test
    fun playlistItem_playCountBadge() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(196.dp, 260.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    ResonotePlaylistItem(
                        metadata = ResonotePlaylistMetadata(
                            title = "深夜独白：安静的陪伴与城市微光",
                            playCount = "12.8万",
                        ),
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            }
        }

        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/MusicComponents/PlaylistItem_playCountBadge.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun playlistItem_supportingText() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(196.dp, 260.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    ResonotePlaylistItem(
                        metadata = ResonotePlaylistMetadata(
                            title = "凌晨公路与钠灯",
                            supportingText = "42 首",
                        ),
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            }
        }

        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/MusicComponents/PlaylistItem_supportingText.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }
}

private data class MusicScreenshotCase(val name: String, val themeMode: ResonoteThemeMode, val fontScale: Float = 1f)
