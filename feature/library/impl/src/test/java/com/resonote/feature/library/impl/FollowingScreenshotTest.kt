package com.resonote.feature.library.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.FollowedArtist
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
class FollowingScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun followingContentUsesFullWidthRowsAndIndependentActions() {
        val artists = (1..8).map { FollowedArtist(it.toString(), "关注歌手 $it", null) }
        var openedArtist: FollowedArtist? = null
        var unfollowedArtist: FollowedArtist? = null
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    FollowingScreen(
                        state = FollowingUiState.Content(artists, artists.size, hasMore = false),
                        bottomContentPadding = 24.dp,
                        onBack = {},
                        onRetry = {},
                        onRefresh = {},
                        onLoadMore = {},
                        onArtistClick = { openedArtist = it },
                        onUnfollow = { unfollowedArtist = it },
                    )
                }
            }
        }

        composeRule.onNodeWithTag("following-item-1").performClick()
        composeRule.onNodeWithTag("following-button-2").performClick()

        assertThat(openedArtist).isEqualTo(artists[0])
        assertThat(unfollowedArtist).isEqualTo(artists[1])
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/Following/FollowingCompact_content.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }
}
