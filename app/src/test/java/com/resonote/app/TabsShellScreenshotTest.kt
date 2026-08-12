package com.resonote.app

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import com.github.takahirom.roborazzi.captureRoboImage
import com.resonote.core.data.HomeRepository
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.HomeContent
import com.resonote.core.model.PlaylistSummary
import com.resonote.core.model.HomeRefreshResult
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.RadioRecommendationResult
import com.resonote.core.model.RecommendationMode
import com.resonote.core.screenshottesting.DefaultRoborazziOptions
import com.resonote.feature.home.impl.HomeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-420dpi")
class TabsShellScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tabsShell_compactHome() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    val homeViewModel = remember { HomeViewModel(ScreenshotHomeRepository()) }
                    TabsShell(homeViewModel = homeViewModel)
                }
            }
        }

        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/TabsShell/TabsShellCompact_home.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    private class ScreenshotHomeRepository : HomeRepository {
        private val homeContent =
            HomeContent(
                dailyRecommendations = List(6) { song("daily-$it") },
                recommendedPlaylists = List(6) {
                    PlaylistSummary("playlist-$it", "推荐歌单 ${it + 1}", null, 12_000L * (it + 1))
                },
                newSongs = List(6) { song("new-$it") },
            )
        private val mutableContent = MutableStateFlow<HomeContent?>(homeContent)
        override val content: StateFlow<HomeContent?> = mutableContent

        override suspend fun refresh(): HomeRefreshResult = HomeRefreshResult.Updated(homeContent, emptyList())

        override suspend fun loadRadio(mode: RecommendationMode): RadioRecommendationResult =
            RadioRecommendationResult.Available(listOf(song("radio")))
    }

    private companion object {
        fun song(id: String) =
            OnlineSong(
                hash = id,
                title = "歌曲 $id",
                artist = "Resonote Artist",
                coverUrl = null,
                albumId = "1",
                albumAudioId = "2",
                durationMillis = 180_000,
                quality = AudioQuality.Lossless,
                vip = false,
            )
    }
}
