package com.resonote.feature.history.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.DeviceHistoryItem
import com.resonote.core.model.DeviceHistoryRecord
import com.resonote.core.model.DeviceHistorySource
import com.resonote.core.model.OnlineSong
import com.resonote.core.screenshottesting.DefaultRoborazziOptions
import com.resonote.feature.history.api.HistoryTab
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-420dpi")
class HistoryScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onlineArchive() {
        setScreen(
            HistoryUiState(
                selectedTab = HistoryTab.Online,
                accountState = HistoryAccountState.Authenticated,
                online = OnlineHistoryUiState.Available(
                    listOf(
                        song("harbor", "离港之前", "林澈", "夜航日志", AudioQuality.Lossless),
                        song("tide", "潮汐信号", "Winter Archive", "沿岸电台", AudioQuality.HighResolution),
                        song("letter", "岛屿来信", "苏眠", "向海而生", AudioQuality.HighQuality),
                        song("window", "舷窗微光", "木星旅馆", null, AudioQuality.Standard),
                    ),
                ),
                deviceLoading = false,
            ),
        )

        composeRule.onNodeWithTag("history-archive").assertIsDisplayed()
        composeRule.onNodeWithText("最近播放").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("刷新在线记录").assertIsDisplayed()
        composeRule.onNodeWithText("账号收听足迹").assertIsDisplayed()
        capture("online")
    }

    @Test
    fun deviceArchive() {
        setScreen(
            HistoryUiState(
                selectedTab = HistoryTab.Device,
                accountState = HistoryAccountState.Anonymous,
                deviceItems = listOf(
                    device("local", "深夜电台", "林澈", DeviceHistorySource.Local, 3),
                    device("cloud", "海岸线以北", "Winter Archive", DeviceHistorySource.Cloud, 1),
                    device("demo", "留声机里的夏天", "苏眠", DeviceHistorySource.Local, 7),
                ),
                deviceLoading = false,
            ),
        )

        composeRule.onNodeWithText("这台设备的播放档案").assertIsDisplayed()
        composeRule.onNodeWithText("云盘 · Winter Archive · 播放 1 次").assertIsDisplayed()
        capture("device")
    }

    private fun setScreen(state: HistoryUiState) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    HistoryScreen(
                        state = state,
                        playingMediaId = "cloud",
                        bottomContentPadding = 32.dp,
                        onBack = {},
                        onLoginRequest = {},
                        onSelectTab = {},
                        onRefreshOnline = {},
                        onPlayOnline = { _, _ -> },
                        onPlayDevice = { _, _ -> },
                        onDeleteDevice = {},
                        onClearDevice = {},
                        onDismissMutationFailure = {},
                    )
                }
            }
        }
    }

    private fun song(hash: String, title: String, artist: String, album: String?, quality: AudioQuality) = OnlineSong(
        hash = hash,
        title = title,
        artist = artist,
        coverUrl = null,
        albumId = null,
        albumAudioId = "audio-$hash",
        durationMillis = 218_000,
        quality = quality,
        vip = false,
        albumTitle = album,
    )

    private fun device(id: String, title: String, artist: String, source: DeviceHistorySource, playCount: Long) =
        DeviceHistoryItem(
            record = DeviceHistoryRecord(
                source = source,
                mediaId = id,
                title = title,
                artist = artist,
                albumTitle = "夜航档案",
                artworkUri = null,
                durationMillis = 241_000,
                albumAudioId = "audio-$id",
            ),
            lastPlayedAtEpochMillis = 2_000,
            playCount = playCount,
        )

    private fun capture(name: String) {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/History/HistoryCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }
}
