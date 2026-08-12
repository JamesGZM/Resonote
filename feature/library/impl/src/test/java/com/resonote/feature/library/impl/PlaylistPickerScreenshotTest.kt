package com.resonote.feature.library.impl

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
import com.resonote.core.model.UserPlaylist
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
class PlaylistPickerScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun picker_onlyShowsWritablePlaylists() {
        var selectedListId: String? = null
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    PlaylistPickerSheet(
                        state = MyUiState.Authenticated(
                            userId = "account-a",
                            playlists = MySectionState.Available(
                                listOf(
                                    playlist("liked", "我喜欢", isLike = true),
                                    playlist("night", "深夜独白"),
                                    playlist("collected", "他人的收藏", isMine = false),
                                ),
                            ),
                        ),
                        song = song(),
                        onDismiss = {},
                        onRetryPlaylists = {},
                        onPlaylistClick = { selectedListId = it.listId },
                        onDismissFailure = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("他人的收藏").assertDoesNotExist()
        composeRule.onNodeWithText("深夜独白").performClick()
        assertThat(selectedListId).isEqualTo("night")
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/PlaylistPicker/PlaylistPickerCompact_available.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    private fun playlist(
        listId: String,
        name: String,
        isMine: Boolean = true,
        isLike: Boolean = false,
    ) = UserPlaylist(
        listId = listId,
        globalId = "global-$listId",
        name = name,
        coverUrl = null,
        count = if (isLike) 186 else 42,
        isMine = isMine,
        isLike = isLike,
    )

    private fun song() = OnlineSong(
        hash = "evening-signal",
        title = "晚风信号",
        artist = "林澈",
        coverUrl = null,
        albumId = "album-1",
        albumAudioId = "audio-1",
        durationMillis = 248_000,
        quality = AudioQuality.HighResolution,
        vip = false,
    )
}
