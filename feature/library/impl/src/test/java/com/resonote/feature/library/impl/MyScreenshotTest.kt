package com.resonote.feature.library.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.UserPlaylist
import com.resonote.core.model.UserProfile
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
class MyScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun my_anonymous() {
        var loginClicks = 0
        setScreen(MyUiState.Anonymous, onLoginClick = { loginClicks++ })

        composeRule.onNodeWithTag("my-anonymous").assertIsDisplayed()
        composeRule.onNodeWithText("登录账号").performClick()
        assertThat(loginClicks).isEqualTo(1)
        capture("anonymous")
    }

    @Test
    fun my_authenticatedProfileAndPlaylists() {
        val state = authenticatedState()
        setScreen(state)

        composeRule.onNodeWithTag("my-profile").assertIsDisplayed()
        composeRule.onNodeWithText("我喜欢").assertExists()
        capture("profile")

        composeRule.onNodeWithTag("my-list").performScrollToIndex(3)
        composeRule.waitForIdle()
        capture("playlists")
    }

    private fun setScreen(state: MyUiState, onLoginClick: () -> Unit = {}) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    MyScreen(
                        state = state,
                        bottomContentPadding = 24.dp,
                        onLoginClick = onLoginClick,
                        onRefresh = {},
                        onRetryProfile = {},
                        onRetryPlaylists = {},
                        onPlaylistClick = {},
                    )
                }
            }
        }
    }

    private fun authenticatedState() = MyUiState.Authenticated(
        profile = MySectionState.Available(
            UserProfile(
                userId = "2048264",
                nickname = "海岸线收听者",
                avatarUrl = null,
                backgroundUrl = null,
                signature = "沿着海岸线，收藏每一种声音。",
                fans = 12_600,
                follows = 128,
                listenMinutes = 9_840,
                isVip = true,
                vipLabel = "SVIP",
            ),
        ),
        playlists = MySectionState.Available(
            listOf(
                playlist("我喜欢", 326, true, true),
                playlist("凌晨公路与钠灯", 42, true, false),
                playlist("潮汐来信", 18, true, false),
                playlist("北纬三十度", 64, false, false),
                playlist("慢速列车", 27, false, false),
            ),
        ),
    )

    private fun playlist(name: String, count: Long, isMine: Boolean, isLike: Boolean) = UserPlaylist(
        listId = "list-$name",
        globalId = "global-$name",
        name = name,
        coverUrl = null,
        count = count,
        isMine = isMine,
        isLike = isLike,
    )

    private fun capture(name: String) {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/My/MyCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }
}
