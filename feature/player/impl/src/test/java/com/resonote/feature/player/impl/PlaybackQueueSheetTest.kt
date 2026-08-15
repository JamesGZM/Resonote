package com.resonote.feature.player.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackMode
import com.resonote.core.playback.PlaybackState
import com.resonote.core.playback.PlaybackStatus
import com.resonote.core.screenshottesting.DefaultRoborazziOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-420dpi")
@OptIn(ExperimentalRoborazziApi::class)
class PlaybackQueueSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedQueue_exposesHeaderActionsAndIndependentItemActions() {
        var selectedIndex: Int? = null
        var removedIndex: Int? = null
        var selectedMode: PlaybackMode? = null
        var clearCount = 0
        setQueueContent(
            playback = playbackState(),
            onSelect = { selectedIndex = it },
            onRemove = { removedIndex = it },
            onClear = { clearCount++ },
            onModeChange = { selectedMode = it },
        )

        composeRule.onNodeWithText("播放队列").assertExists()
        composeRule.onNodeWithText("列表循环 · 3首").assertExists()
        composeRule.onNodeWithContentDescription("播放模式：列表循环").performClick()
        composeRule.onNodeWithContentDescription("清空").performClick()
        composeRule.onNodeWithContentDescription("移除 失重来信").performClick()

        assertEquals(PlaybackMode.Shuffle, selectedMode)
        assertEquals(1, clearCount)
        assertEquals(1, removedIndex)
        assertNull(selectedIndex)

        composeRule.onNodeWithText("失重来信").performClick()

        assertEquals(1, selectedIndex)
        composeRule.onNodeWithContentDescription("失重来信 的更多操作").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("长按并拖动排序").assertDoesNotExist()
    }

    @Test
    fun emptyQueue_disablesClearAction() {
        setQueueContent(playback = PlaybackState(mode = PlaybackMode.ListLoop))

        composeRule.onNodeWithText("列表循环 · 0首").assertExists()
        composeRule.onNodeWithText("播放队列为空").assertExists()
        composeRule.onNodeWithContentDescription("清空").assertIsNotEnabled()
    }

    @Test
    fun playbackQueue_populated() {
        setQueueContent(playback = playbackState(songCount = 8))
        composeRule.waitForIdle()

        captureScreenRoboImage(
            filePath = "src/test/screenshots/Player/PlaybackQueue_populated.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    private fun setQueueContent(
        playback: PlaybackState,
        onSelect: (Int) -> Unit = {},
        onRemove: (Int) -> Unit = {},
        onClear: () -> Unit = {},
        onModeChange: (PlaybackMode) -> Unit = {},
    ) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                        PlaybackQueueSheet(
                            playback = playback,
                            onDismiss = {},
                            onSelect = onSelect,
                            onRemove = onRemove,
                            onClear = onClear,
                            onModeChange = onModeChange,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        fun playbackState(songCount: Int = 3): PlaybackState = PlaybackState(
            queue = List(songCount) { index ->
                when (index) {
                    0 -> song("current", "潮汐记忆", "林澈")
                    1 -> song("next", "失重来信", "沉岛乐队")
                    2 -> song("third", "凌晨四点以后依然没有结束的漫长梦境", "周遥")
                    else -> song("extra-$index", "队列歌曲 ${index + 1}", "Resonote")
                }
            }.map(::PlaybackItem),
            currentIndex = 0,
            status = PlaybackStatus.Playing,
            mode = PlaybackMode.ListLoop,
        )

        fun song(hash: String, title: String, artist: String) = OnlineSong(
            hash = hash,
            title = title,
            artist = artist,
            coverUrl = null,
            albumId = "night-signal",
            albumAudioId = "audio-$hash",
            durationMillis = 248_000,
            quality = AudioQuality.Lossless,
            vip = false,
        )
    }
}
