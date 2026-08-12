package com.resonote.feature.search.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
import com.resonote.core.model.SearchKeyword
import com.resonote.core.model.SearchMv
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
class SearchScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun search_compactDiscovery() {
        setSearchContent(
            SearchUiState(
                history = listOf("午夜爵士", "林俊杰", "城市民谣"),
                hotKeywords = listOf(
                    SearchKeyword("新歌速递", ""),
                    SearchKeyword("经典粤语", ""),
                    SearchKeyword("通勤歌单", ""),
                ),
            ),
        )

        composeRule.onNodeWithText("最近搜索").assertExists()
        composeRule.onNodeWithText("热门搜索").assertExists()
        capture("discovery")
    }

    @Test
    fun search_compactSongPage() {
        val songs = listOf(
            song("night", "夜曲", "周杰伦", AudioQuality.Lossless, true),
            song("signal", "晚风信号", "林澈 · 潮汐记忆", AudioQuality.HighResolution, false),
            song("forest", "写给森林的信", "北岸合唱团", AudioQuality.Standard, false),
            song("snow", "雪线以北", "远山计划", AudioQuality.HighQuality, false),
            song("room", "蓝色房间", "Lin & The Archive", AudioQuality.Lossless, true),
        )
        setSearchContent(
            SearchUiState(
                query = "夜",
                selectedCategory = SearchCategory.SONGS,
                result = SearchResultUiState.Content(
                    query = "夜",
                    category = SearchCategory.SONGS,
                    value = SearchContentUiState.Page(
                        items = songs.map(SearchResultItem::Song),
                        page = 1,
                        total = 36,
                        hasMore = true,
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText("单曲").assertExists()
        composeRule.onNodeWithText("加载更多").assertExists()
        composeRule.onNodeWithContentDescription("More actions for 夜曲").assertDoesNotExist()
        capture("songs")
    }

    @Test
    fun search_compactMvPageAndPreservesMetadataOnClick() {
        val mv = SearchMv(
            hash = "tide-signal",
            name = "潮汐信号：海岸线现场",
            singer = "林澈 · Winter Archive",
            coverUrl = "https://img.example/tide.jpg",
            durationMillis = 265_000,
        )
        var selected: SearchMv? = null
        setSearchContent(
            state = SearchUiState(
                query = "潮汐",
                selectedCategory = SearchCategory.MVS,
                result = SearchResultUiState.Content(
                    query = "潮汐",
                    category = SearchCategory.MVS,
                    value = SearchContentUiState.Page(
                        items = listOf(
                            SearchResultItem.Mv(mv),
                            SearchResultItem.Mv(
                                SearchMv("night-route", "夜航路线", "林澈", null, 247_000),
                            ),
                        ),
                        page = 1,
                        total = 2,
                        hasMore = false,
                    ),
                ),
            ),
            onMvClick = { selected = it },
        )

        composeRule.onNodeWithText("潮汐信号：海岸线现场").performClick()

        assertThat(selected).isEqualTo(mv)
        capture("mvs")
    }

    private fun setSearchContent(
        state: SearchUiState,
        onMvClick: ((SearchMv) -> Unit)? = null,
    ) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    SearchScreen(
                        state = state,
                        onQueryChange = {},
                        onSubmit = {},
                        onRetry = {},
                        onSelectCategory = {},
                        onLoadMore = {},
                        onRemoveHistory = {},
                        onClearHistory = {},
                        onBack = {},
                        onRecognitionClick = null,
                        onSongClick = {},
                        onSongMoreClick = null,
                        onPlaylistClick = null,
                        onAlbumClick = null,
                        onArtistClick = null,
                        onMvClick = onMvClick,
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/Search/SearchCompact_$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    private fun song(
        id: String,
        title: String,
        artist: String,
        quality: AudioQuality,
        vip: Boolean,
    ) = OnlineSong(
        hash = id,
        title = title,
        artist = artist,
        coverUrl = null,
        albumId = null,
        albumAudioId = null,
        durationMillis = 248_000,
        quality = quality,
        vip = vip,
    )
}
