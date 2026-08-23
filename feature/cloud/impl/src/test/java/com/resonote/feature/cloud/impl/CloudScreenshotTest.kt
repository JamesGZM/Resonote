package com.resonote.feature.cloud.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.CloudStorage
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.ContentFailure
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
class CloudScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cloud_emptyUsesCommonState() {
        setScreen(CloudUiState(initialLoading = false))
        composeRule.onNodeWithTag("resonote-empty-state").assertExists()
    }

    @Test
    fun cloud_errorUsesCommonState() {
        setScreen(CloudUiState(initialLoading = false, failure = ContentFailure.Network))
        composeRule.onNodeWithTag("resonote-error-state").assertExists()
    }

    @Test
    fun cloud_listTop() {
        setScreen(state())

        composeRule.onNodeWithTag("cloud-summary").assertIsDisplayed()
        composeRule.onNodeWithText("播放全部").assertIsDisplayed()
        capture("list_top")
    }

    @Test
    fun cloud_searchIndexAndPlaybackError() {
        setScreen(
            state().copy(
                query = "海岸",
                isIndexing = true,
                hasMore = true,
                playback = CloudPlaybackUiState.Failed(
                    "harbor",
                    CloudPlaybackIssue.Failed(com.resonote.core.model.ContentFailure.Network),
                ),
            ),
        )

        composeRule.onNodeWithText("正在载入完整云盘，以搜索全部歌曲…").assertExists()
        composeRule.onNodeWithTag("cloud-playback-error").assertExists()
        capture("search_error")
    }

    private fun setScreen(state: CloudUiState) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    CloudScreen(
                        state = state,
                        playingMediaId = "tide",
                        bottomContentPadding = 32.dp,
                        onBack = {},
                        onRefresh = {},
                        onQueryChange = {},
                        onSortChange = {},
                        onPlayAll = {},
                        onPlayTrack = {},
                        onAppendTracks = {},
                        onLoadMore = {},
                        onRetryMore = {},
                        onRetryPlayback = {},
                        onDismissPlaybackIssue = {},
                    )
                }
            }
        }
    }

    private fun state() = CloudUiState(
        tracks = listOf(
            track("harbor", "离港之前", "林澈", "夜航日志", 238_000),
            track("tide", "潮汐信号", "Winter Archive", "沿岸电台", 265_000),
            track("letter", "潮汐来信", "苏眠", "岛屿来信", 192_000),
            track("route", "夜航路线", "林澈", "夜航日志", 247_000),
            track("window", "舷窗微光", "木星旅馆", null, 215_000),
            track("coast", "回到海岸", "林澈", "夜航日志", 281_000),
        ),
        page = 1,
        total = 128,
        hasMore = true,
        storage = CloudStorage(usedBytes = 12_884_901_888, maxBytes = 53_687_091_200),
        initialLoading = false,
    )

    private fun track(hash: String, title: String, artist: String, album: String?, duration: Long) = CloudTrack(
        hash = hash,
        title = title,
        artist = artist,
        album = album,
        coverUrl = null,
        durationMillis = duration,
        albumAudioId = "audio-$hash",
    )

    private fun capture(name: String) {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/Cloud/CloudCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }
}
