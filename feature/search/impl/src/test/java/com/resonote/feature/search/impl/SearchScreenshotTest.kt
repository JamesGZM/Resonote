package com.resonote.feature.search.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.Locales
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.ComplexSearchResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.SearchAlbum
import com.resonote.core.model.SearchArtist
import com.resonote.core.model.SearchKeyword
import com.resonote.core.model.SearchMv
import com.resonote.core.model.SearchPlaylist
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
    fun search_emptyUsesCommonState() {
        setSearchContent(
            SearchUiState(
                query = "不存在",
                result = SearchResultUiState.Empty("不存在", SearchCategory.ALL),
            ),
        )
        composeRule.onNodeWithTag("resonote-empty-state").assertExists()
    }

    @Test
    fun search_errorUsesCommonState() {
        setSearchContent(
            SearchUiState(
                query = "不存在",
                result = SearchResultUiState.Error("不存在", SearchCategory.ALL, ContentFailure.Network),
            ),
        )
        composeRule.onNodeWithTag("resonote-error-state").assertExists()
    }

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
        composeRule.onNodeWithContentDescription("听歌识曲").assertIsEnabled()
        capture("discovery")
    }

    @Test
    fun search_compactDiscoveryWithoutHistory() {
        setSearchContent(
            SearchUiState(
                hotKeywords = listOf(
                    SearchKeyword("新歌速递", ""),
                    SearchKeyword("经典粤语", ""),
                    SearchKeyword("通勤歌单", ""),
                ),
            ),
        )

        composeRule.onNodeWithText("最近搜索").assertDoesNotExist()
        composeRule.onNodeWithText("热门搜索").assertExists()
        capture("discovery_without_history")
    }

    @Test
    fun search_englishUsesLocalizedHistoryTitleAndHint() {
        setSearchContent(
            state = SearchUiState(history = listOf("Jazz")),
            languageTag = "en",
        )

        composeRule.onNodeWithText("Recent searches").assertExists()
        composeRule.onNodeWithText("Songs, artists, albums").assertExists()
    }

    @Test
    fun search_compactHistoryEditingAndActions() {
        var removedQuery: String? = null
        var cleared = false
        setSearchContent(
            state = SearchUiState(
                history = listOf("午夜爵士", "林俊杰", "城市民谣"),
                hotKeywords = listOf(SearchKeyword("新歌速递", "")),
            ),
            onRemoveHistory = { removedQuery = it },
            onClearHistory = { cleared = true },
        )

        composeRule.onNodeWithText("编辑").performClick()

        composeRule.onNodeWithText("清空").assertExists()
        composeRule.onNodeWithText("完成").assertExists()
        capture("discovery_editing")

        composeRule.onNodeWithContentDescription("删除搜索记录“午夜爵士”").performClick()
        assertThat(removedQuery).isEqualTo("午夜爵士")
        composeRule.onNodeWithText("清空").performClick()
        assertThat(cleared).isTrue()
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
            state = SearchUiState(
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
            playingMediaId = "signal",
        )

        composeRule.onNodeWithText("单曲").assertExists()
        composeRule.onNodeWithText("加载更多").assertExists()
        composeRule.onNodeWithContentDescription("More actions for 夜曲").assertDoesNotExist()
        capture("songs")
    }

    @Test
    fun search_compactAggregateUsesLegacySectionOrderAndSharedCards() {
        setSearchContent(
            state = SearchUiState(
                query = "林澈",
                result = SearchResultUiState.Content(
                    query = "林澈",
                    category = SearchCategory.ALL,
                    value = SearchContentUiState.Aggregate(
                        ComplexSearchResult(
                            artists = listOf(SearchArtist("artist", "林澈", null, 4, 28)),
                            songs = listOf(song("signal", "潮汐信号", "林澈", AudioQuality.Lossless, false)),
                            songsTotal = 28,
                            playlists = listOf(
                                SearchPlaylist("playlist", "沿海夜行", "林澈", null, 18, 12_600),
                                SearchPlaylist("playlist-2", "深夜声场", "Resonote", null, 24, 8_800),
                            ),
                            playlistsTotal = 3,
                            albums = listOf(
                                SearchAlbum("album", "潮汐记忆", "林澈", null, 12, "2026"),
                                SearchAlbum("album-2", "冬日档案", "林澈", null, 9, "2025"),
                            ),
                            albumsTotal = 4,
                            mvs = listOf(
                                SearchMv("mv", "海岸线现场", "林澈", null, 265_000),
                                SearchMv("mv-2", "潮汐信号 Live", "林澈", null, 218_000),
                            ),
                            mvsTotal = 2,
                        ),
                    ),
                ),
            ),
            playingMediaId = "signal",
        )

        composeRule.onAllNodesWithText("歌手").assertCountEquals(2)
        composeRule.onNodeWithText("潮汐信号").assertExists()
        composeRule.onNodeWithContentDescription("正在播放").assertExists()
        capture("aggregate")
        composeRule.onNodeWithTag("search-aggregate").performScrollToIndex(5)
        capture("aggregate_media_grids")
    }

    @Test
    fun search_compactPlaylistGrid() {
        val playlists = listOf(
            SearchPlaylist("coast", "沿海夜行", "林澈", null, 18, 126_000),
            SearchPlaylist("jazz", "午夜爵士俱乐部", "Resonote", null, 32, 88_000),
            SearchPlaylist("rain", "雨天通勤", "北岸", null, 24, 9_600),
        )
        setSearchContent(
            state = pageState(
                SearchCategory.PLAYLISTS,
                playlists.map(SearchResultItem::Playlist),
            ),
        )

        capture("playlists")
    }

    @Test
    fun search_compactAlbumGrid() {
        val albums = listOf(
            SearchAlbum("tide", "潮汐记忆", "林澈", null, 12, "2026"),
            SearchAlbum("winter", "冬日档案", "Winter Archive", null, 9, "2025"),
            SearchAlbum("forest", "写给森林的信", "北岸合唱团", null, 11, "2024"),
        )
        setSearchContent(
            state = pageState(
                SearchCategory.ALBUMS,
                albums.map(SearchResultItem::Album),
            ),
        )

        capture("albums")
    }

    @Test
    fun search_compactArtistGrid() {
        val artists = listOf(
            SearchArtist("lin", "林澈", null, 4, 28),
            SearchArtist("winter", "Winter Archive", null, 7, 42),
            SearchArtist("north", "北岸合唱团", null, 3, 19),
            SearchArtist("forest", "森林计划", null, 5, 31),
        )
        setSearchContent(
            state = pageState(
                SearchCategory.ARTISTS,
                artists.map(SearchResultItem::Artist),
            ),
        )

        capture("artists")
    }

    @Test
    fun search_clearActionEmitsEmptyQuery() {
        var updatedQuery: String? = null
        setSearchContent(
            state = SearchUiState(query = "夜"),
            onQueryChange = { updatedQuery = it },
        )

        composeRule.onNodeWithContentDescription("清除搜索内容").performClick()

        assertThat(updatedQuery).isEmpty()
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

    @Test
    fun search_compactMvPageDark() {
        setSearchContent(state = mvState(), themeMode = ResonoteThemeMode.DARK)
        capture("mvs_dark")
    }

    @Test
    fun search_compactMvPageAmoled() {
        setSearchContent(state = mvState(), themeMode = ResonoteThemeMode.AMOLED)
        capture("mvs_amoled")
    }

    private fun setSearchContent(
        state: SearchUiState,
        playingMediaId: String? = null,
        onQueryChange: (String) -> Unit = {},
        onRemoveHistory: (String) -> Unit = {},
        onClearHistory: () -> Unit = {},
        onMvClick: ((SearchMv) -> Unit)? = {},
        themeMode: ResonoteThemeMode = ResonoteThemeMode.LIGHT,
        languageTag: String = "zh-CN",
    ) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.Locales(LocaleList(Locale(languageTag))),
            ) {
                DeviceConfigurationOverride(
                    override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
                ) {
                    ResonoteTheme(themeMode = themeMode) {
                        SearchScreen(
                            state = state,
                            playingMediaId = playingMediaId,
                            onQueryChange = onQueryChange,
                            onSubmit = {},
                            onRetry = {},
                            onSelectCategory = {},
                            onLoadMore = {},
                            onRemoveHistory = onRemoveHistory,
                            onClearHistory = onClearHistory,
                            onBack = {},
                            onRecognitionClick = {},
                            onSongClick = {},
                            onSongMoreClick = null,
                            onPlaylistClick = {},
                            onAlbumClick = {},
                            onArtistClick = {},
                            onMvClick = onMvClick,
                        )
                    }
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

    private fun mvState() = SearchUiState(
        query = "潮汐",
        selectedCategory = SearchCategory.MVS,
        result = SearchResultUiState.Content(
            query = "潮汐",
            category = SearchCategory.MVS,
            value = SearchContentUiState.Page(
                items = listOf(
                    SearchResultItem.Mv(
                        SearchMv("tide-signal", "潮汐信号：海岸线现场", "林澈", null, 265_000),
                    ),
                ),
                page = 1,
                total = 1,
                hasMore = false,
            ),
        ),
    )

    private fun pageState(category: SearchCategory, items: List<SearchResultItem>) = SearchUiState(
        query = "夜",
        selectedCategory = category,
        result = SearchResultUiState.Content(
            query = "夜",
            category = category,
            value = SearchContentUiState.Page(
                items = items,
                page = 1,
                total = items.size,
                hasMore = false,
            ),
        ),
    )

    private fun song(id: String, title: String, artist: String, quality: AudioQuality, vip: Boolean) = OnlineSong(
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
