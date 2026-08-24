package com.resonote.feature.library.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.then
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
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-420dpi")
class MyScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun my_anonymous() {
        var loginClicks = 0
        setScreen(MyUiState.Anonymous, onLoginClick = { loginClicks++ })

        composeRule.onNodeWithTag("my-anonymous").assertIsDisplayed()
        composeRule.onNodeWithTag("resonote-empty-state").assertIsDisplayed()
        composeRule.onNodeWithText("登录后发现更多内容").assertIsDisplayed()
        composeRule.onNodeWithText("本地音乐属于此设备，不会因为登录或换号而被清除。")
            .assertDoesNotExist()
        composeRule.onNodeWithText("登录你的账号").performClick()
        assertThat(loginClicks).isEqualTo(1)
        capture("anonymous")
    }

    @Test
    fun my_authenticatedProfileAndPlaylists() {
        val state = authenticatedState()
        setScreen(state)

        composeRule.onNodeWithTag("my-profile").assertIsDisplayed()
        capture("profile")

        composeRule.onNodeWithText("收藏的 2").performClick()
        composeRule.onNodeWithText("北纬三十度").performScrollTo()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("北纬三十度").assertIsDisplayed()
        capture("playlists")
    }

    @Test
    fun dailyVipCheckInIsBesideUserIdAndClickable() {
        var dailyVipClicks = 0
        setScreen(authenticatedState(), onDailyVipClick = { dailyVipClicks++ })

        val userIdBounds = composeRule.onNodeWithTag("my-user-id", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val checkInBounds = composeRule.onNodeWithTag("my-daily-vip", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        assertThat(checkInBounds.left).isAtLeast(userIdBounds.right)
        assertThat(checkInBounds.center.y).isWithin(1f).of(userIdBounds.center.y)
        composeRule.onNodeWithText("签到").performClick()
        assertThat(dailyVipClicks).isEqualTo(1)
    }

    @Test
    fun emptySignatureUsesDefaultCopyWithoutCollapsingProfileLayout() {
        val state = authenticatedState()
        val profile = (state.profile as MySectionState.Available<UserProfile>).value.copy(signature = "")
        setScreen(state.copy(profile = MySectionState.Available(profile)))

        composeRule.onNodeWithTag("my-signature").assertIsDisplayed()
        composeRule.onNodeWithText("还没有填写个性签名").assertIsDisplayed()
        capture("profile_default_signature")
    }

    @Test
    fun profileStatsAreFourEqualColumns() {
        setScreen(authenticatedState())

        val stats = listOf("my-stat-follows", "my-stat-fans", "my-stat-listen-time", "my-stat-music-age")
            .map { tag ->
                composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            }
        val firstWidth = stats.first().width

        stats.drop(1).forEach { bounds ->
            assertThat(bounds.width).isWithin(2f).of(firstWidth)
        }
        val screenWidth = composeRule.onNodeWithTag("my-list").fetchSemanticsNode().boundsInRoot.width
        val expectedFirstCenter = screenWidth / 8f
        assertThat(stats.first().center.x).isWithin(2f).of(expectedFirstCenter)
        composeRule.onNodeWithText("8年").assertIsDisplayed()
        composeRule.onNodeWithText("乐龄").assertIsDisplayed()
    }

    @Test
    fun followingStatIsClickable() {
        var followingClicks = 0
        setScreen(authenticatedState(), onFollowingClick = { followingClicks++ })

        composeRule.onNodeWithTag("my-stat-follows").performClick()

        assertThat(followingClicks).isEqualTo(1)
    }

    @Test
    fun settingsEntryIsAvailableWithoutAccount() {
        var settingsClicks = 0
        setScreen(MyUiState.Anonymous, onSettingsClick = { settingsClicks++ })

        composeRule.onNodeWithTag("my-settings").performClick()

        assertThat(settingsClicks).isEqualTo(1)
    }

    @Test
    fun playlistGroupsSwitchVisibleContent() {
        setScreen(authenticatedState())

        composeRule.onNodeWithTag("my-playlist-header").assertHeightIsEqualTo(40.dp)
        composeRule.onNodeWithText("收藏的 2").performClick()

        composeRule.onNodeWithTag("my-playlist-header").assertHeightIsEqualTo(40.dp)
        composeRule.onNodeWithText("北纬三十度").assertIsDisplayed()
    }

    @Test
    fun emptyCollectedPlaylistsUseSharedContentState() {
        val playlists = listOf(
            playlist("我喜欢", 326, true, true),
            playlist("凌晨公路与钠灯", 42, true, false),
        )
        setScreen(authenticatedState().copy(playlists = MySectionState.Available(playlists)))

        composeRule.onNodeWithText("收藏的 0").performClick()

        composeRule.onNodeWithTag("resonote-empty-state").assertIsDisplayed()
        composeRule.onNodeWithText("还没有收藏歌单").assertIsDisplayed()
        capture("empty_collected")
    }

    @Test
    fun primaryContentUsesSharedHorizontalRails() {
        setScreen(authenticatedState())

        val leftRail = composeRule.onNodeWithTag("my-avatar", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.left
        listOf("my-playlist-title").forEach { tag ->
            val left = composeRule.onNodeWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot.left
            assertThat(left).isWithin(1f).of(leftRail)
        }
    }

    @Test
    fun quickEntriesAreEqualWidthAlignedAndEvenlySpaced() {
        setScreen(authenticatedState())

        val entries = listOf("my-liked", "my-history", "my-cloud", "my-local-music")
        val icons = listOf("my-liked-icon", "my-history-icon", "my-cloud-icon", "my-local-music-icon")
        val entryBounds = entries.map { tag ->
            composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        }
        val firstWidth = entryBounds.first().width
        entryBounds.drop(1).forEach { bounds ->
            assertThat(bounds.width).isWithin(1f).of(firstWidth)
        }
        val iconBounds = icons.map { tag ->
            composeRule.onNodeWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
        }
        val firstGap = iconBounds[1].center.x - iconBounds[0].center.x
        iconBounds.zipWithNext().forEach { (left, right) ->
            assertThat(right.center.x - left.center.x).isWithin(1f).of(firstGap)
        }

        val screenWidth = composeRule.onNodeWithTag("my-list").fetchSemanticsNode().boundsInRoot.width
        val horizontalInset = with(composeRule.density) { 5.dp.toPx() }
        val cellWidth = (screenWidth - horizontalInset * 2f) / 4f
        assertThat(iconBounds.first().center.x).isWithin(1f).of(horizontalInset + cellWidth / 2f)
        assertThat(iconBounds.last().center.x).isWithin(1f).of(screenWidth - horizontalInset - cellWidth / 2f)
    }

    @Test
    fun quickEntryLabelsStayWithinTheirCellsAtLargeFontScale() {
        setScreen(authenticatedState(), fontScale = 1.3f)

        val entryBounds = composeRule.onNodeWithTag("my-local-music", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val labelBounds = composeRule.onNodeWithText("本地音乐", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        assertThat(labelBounds.left).isAtLeast(entryBounds.left)
        assertThat(labelBounds.right).isAtMost(entryBounds.right)
    }

    @Test
    fun playlistCreationUsesResonoteDialog() {
        setScreen(authenticatedState())

        composeRule.onNodeWithContentDescription("新建歌单").performClick()
        composeRule.onNodeWithTag("my-create-playlist-name").performTextInput("深夜收藏")

        composeRule.onNodeWithTag("my-create-playlist-dialog").assertIsDisplayed()
        capture("create_playlist")
    }

    private fun setScreen(
        state: MyUiState,
        onLoginClick: () -> Unit = {},
        onDailyVipClick: () -> Unit = {},
        onFollowingClick: () -> Unit = {},
        onSettingsClick: () -> Unit = {},
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.FontScale(fontScale) then
                    DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    MyScreen(
                        state = state,
                        bottomContentPadding = 24.dp,
                        onLoginClick = onLoginClick,
                        onDailyVipClick = onDailyVipClick,
                        onFollowingClick = onFollowingClick,
                        onHistoryClick = {},
                        onCloudClick = {},
                        onLocalMusicClick = {},
                        onSettingsClick = onSettingsClick,
                        onRefresh = {},
                        onRetryProfile = {},
                        onRetryPlaylists = {},
                        onCreatePlaylist = {},
                        onDismissPlaylistCreation = {},
                        onAcknowledgePlaylistCreation = {},
                        onPlaylistClick = {},
                    )
                }
            }
        }
    }

    private fun authenticatedState() = MyUiState.Authenticated(
        userId = "2048264",
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
                musicAgeYears = 8,
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
