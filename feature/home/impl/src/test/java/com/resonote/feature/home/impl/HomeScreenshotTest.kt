package com.resonote.feature.home.impl

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.Locales
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
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
        setHomeContent("en-US")

        composeRule.onNodeWithText("首页不可见电台曲名").assertDoesNotExist()
        composeRule.onNodeWithText("首页不可见电台歌手").assertDoesNotExist()
        capture("top")
        composeRule.onNodeWithTag("home-list").performScrollToNode(hasTestTag("home-playlists-header"))
        composeRule.waitForIdle()
        capture("middle")
        composeRule.onNodeWithTag("home-list").performScrollToIndex(8)
        composeRule.waitForIdle()
        capture("bottom")
    }

    @Test
    fun home_compact_chineseTop() {
        setHomeContent("zh-CN")

        composeRule.onNodeWithText("首页不可见电台曲名").assertDoesNotExist()
        composeRule.onNodeWithText("首页不可见电台歌手").assertDoesNotExist()
        capture("top_zh")
    }

    @Test
    fun home_compact_refreshing() {
        setHomeContent("zh-CN", isRefreshing = true)

        capture("refreshing")
    }

    @Test
    fun home_pullToRefresh_triggersRefresh() {
        var refreshCalls = 0
        setHomeContent("zh-CN", onRefresh = { refreshCalls += 1 })

        composeRule.onNodeWithTag("home-pull-to-refresh").performTouchInput {
            swipeDown(startY = centerY, endY = bottom)
        }
        composeRule.waitForIdle()

        assertThat(refreshCalls).isEqualTo(1)
    }

    @Test
    fun home_content_doesNotExposeRetryAction() {
        setHomeContent("zh-CN")

        composeRule.onNodeWithText("重试").assertDoesNotExist()
    }

    @Test
    fun home_emptyError_exposesRetryAction() {
        var retryCalls = 0
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.Locales(LocaleList(Locale("zh-CN"))),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    HomeLoadError(onRetry = { retryCalls += 1 }, modifier = Modifier)
                }
            }
        }

        composeRule.onNodeWithText("重试").performClick()

        assertThat(retryCalls).isEqualTo(1)
    }

    private fun setHomeContent(languageTag: String, isRefreshing: Boolean = false, onRefresh: () -> Unit = {}) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.Locales(LocaleList(Locale(languageTag))),
            ) {
                DeviceConfigurationOverride(
                    override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
                ) {
                    ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                        HomeScreen(
                            state = HomeFixtures.state().copy(
                                radio = HomeSongUiModel(
                                    id = "radio-only",
                                    title = "首页不可见电台曲名",
                                    artist = "首页不可见电台歌手",
                                    duration = "3:30",
                                ),
                            ),
                            isRefreshing = isRefreshing,
                            playingMediaId = HomeFixtures.songs.first().id,
                            bottomContentPadding = 120.dp,
                            onRefresh = onRefresh,
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
        }
    }

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/Home/HomeCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }
}
