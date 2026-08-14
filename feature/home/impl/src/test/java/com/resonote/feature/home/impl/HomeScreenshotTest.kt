package com.resonote.feature.home.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
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
class HomeScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun home_compact_scrollStates() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    HomeScreen(
                        state = HomeFixtures.state(),
                        playingMediaId = HomeFixtures.songs.first().id,
                        bottomContentPadding = 120.dp,
                        onSearchClick = {},
                        onRecognitionClick = {},
                        onPlayRadio = {},
                        onOpenRankings = {},
                        onOpenFeaturedPlaylists = {},
                        onSongClick = { _, _ -> },
                        onSongMoreClick = {},
                        onPlayAll = {},
                        onPlaylistClick = {},
                    )
                }
            }
        }

        capture("top")
        composeRule.onNodeWithTag("home-list").performScrollToNode(hasTestTag("home-playlists-header"))
        composeRule.waitForIdle()
        capture("middle")
        composeRule.onNodeWithTag("home-list").performScrollToIndex(8)
        composeRule.waitForIdle()
        capture("bottom")
    }

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/Home/HomeCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }
}
