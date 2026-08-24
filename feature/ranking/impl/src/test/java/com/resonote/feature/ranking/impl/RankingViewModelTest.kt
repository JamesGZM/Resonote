package com.resonote.feature.ranking.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.RankingRepository
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.Ranking
import com.resonote.core.model.SongPage
import com.resonote.feature.ranking.api.RankingNavKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RankingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun firstPagePublishesRealSongsAndEntryMetadata() = runTest(dispatcher) {
        val repository = FakeRankingRepository()
        val viewModel = RankingViewModel(repository)

        viewModel.load(key())
        advanceUntilIdle()

        val state = viewModel.uiState.value as RankingUiState.Content
        assertThat(state.metadata.title).isEqualTo("潮汐热歌榜")
        assertThat(state.songs.map { it.hash }).containsExactly("rank-1", "rank-2").inOrder()
        assertThat(state.total).isEqualTo(3)
        assertThat(repository.requests).containsExactly("ranking" to 1)
    }

    @Test
    fun emptyFirstPageBecomesExplicitEmptyState() = runTest(dispatcher) {
        val viewModel = RankingViewModel(FakeRankingRepository(emptyFirstPage = true))

        viewModel.load(key())
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            RankingUiState.Empty(RankingMetadata("ranking", "潮汐热歌榜", null)),
        )
    }

    @Test
    fun failedFirstPageRetainsMetadataForRecovery() = runTest(dispatcher) {
        val viewModel = RankingViewModel(FakeRankingRepository(failFirstPage = true))

        viewModel.load(key())
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            RankingUiState.Error(
                RankingMetadata("ranking", "潮汐热歌榜", null),
                ContentFailure.Network,
            ),
        )
    }

    @Test
    fun nextPageAppendsUniqueSongs() = runTest(dispatcher) {
        val viewModel = RankingViewModel(FakeRankingRepository())
        viewModel.load(key())
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value as RankingUiState.Content
        assertThat(state.songs.map { it.hash }).containsExactly("rank-1", "rank-2", "rank-3").inOrder()
        assertThat(state.page).isEqualTo(2)
        assertThat(state.hasMore).isFalse()
    }

    @Test
    fun nextPageFailureKeepsLoadedSongs() = runTest(dispatcher) {
        val viewModel = RankingViewModel(FakeRankingRepository(failSecondPage = true))
        viewModel.load(key())
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value as RankingUiState.Content
        assertThat(state.songs.map { it.hash }).containsExactly("rank-1", "rank-2").inOrder()
        assertThat(state.loadMoreFailure).isEqualTo(ContentFailure.Network)
        assertThat(state.isLoadingMore).isFalse()
    }

    @Test
    fun refreshReplacesSongsWithNewFirstPage() = runTest(dispatcher) {
        val viewModel = RankingViewModel(
            FakeRankingRepository(
                refreshedFirstPage = SongPage(listOf(song("rank-refreshed")), 1, 1, false),
            ),
        )
        viewModel.load(key())
        advanceUntilIdle()

        viewModel.refresh()
        assertThat((viewModel.uiState.value as RankingUiState.Content).isRefreshing).isTrue()
        advanceUntilIdle()

        val state = viewModel.uiState.value as RankingUiState.Content
        assertThat(state.songs.map { it.hash }).containsExactly("rank-refreshed")
        assertThat(state.isRefreshing).isFalse()
        assertThat(state.hasMore).isFalse()
    }

    @Test
    fun refreshFailureKeepsSongsUntilFailureIsAcknowledged() = runTest(dispatcher) {
        val viewModel = RankingViewModel(FakeRankingRepository(failRefresh = true))
        viewModel.load(key())
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        val failed = viewModel.uiState.value as RankingUiState.Content
        assertThat(failed.songs.map { it.hash }).containsExactly("rank-1", "rank-2").inOrder()
        assertThat(failed.refreshFailure).isEqualTo(ContentFailure.Network)
        assertThat(failed.isRefreshing).isFalse()

        viewModel.acknowledgeRefreshFailure()

        assertThat((viewModel.uiState.value as RankingUiState.Content).refreshFailure).isNull()
    }

    @Test
    fun loadMoreIsIgnoredWhileRefreshing() = runTest(dispatcher) {
        val repository = FakeRankingRepository()
        val viewModel = RankingViewModel(repository)
        viewModel.load(key())
        advanceUntilIdle()

        viewModel.refresh()
        viewModel.loadMore()
        advanceUntilIdle()

        assertThat(repository.requests).containsExactly("ranking" to 1, "ranking" to 1).inOrder()
    }

    private class FakeRankingRepository(
        private val emptyFirstPage: Boolean = false,
        private val failFirstPage: Boolean = false,
        private val failSecondPage: Boolean = false,
        private val failRefresh: Boolean = false,
        private val refreshedFirstPage: SongPage? = null,
    ) : RankingRepository {
        val requests = mutableListOf<Pair<String, Int>>()

        override suspend fun loadRankings(): CollectionLoadResult<List<Ranking>> = error("unused")

        override suspend fun loadSongs(rankId: String, page: Int, pageSize: Int): CollectionLoadResult<SongPage> {
            requests += rankId to page
            if (page == 1 && failFirstPage) return CollectionLoadResult.Failed(ContentFailure.Network)
            val isRefresh = page == 1 && requests.count { it.second == 1 } > 1
            if (isRefresh && failRefresh) return CollectionLoadResult.Failed(ContentFailure.Network)
            if (isRefresh && refreshedFirstPage != null) return CollectionLoadResult.Available(refreshedFirstPage)
            if (page == 2 && failSecondPage) return CollectionLoadResult.Failed(ContentFailure.Network)
            if (emptyFirstPage) return CollectionLoadResult.Available(SongPage(emptyList(), 1, 0, false))
            return CollectionLoadResult.Available(
                if (page == 1) {
                    SongPage(listOf(song("rank-1"), song("rank-2")), 1, 3, true)
                } else {
                    SongPage(listOf(song("rank-2"), song("rank-3")), 2, 3, false)
                },
            )
        }
    }

    private companion object {
        fun key() = RankingNavKey("ranking", "潮汐热歌榜")

        fun song(id: String) = OnlineSong(
            hash = id,
            title = id,
            artist = "林澈",
            coverUrl = null,
            albumId = "album",
            albumAudioId = "audio-$id",
            durationMillis = 180_000,
            quality = AudioQuality.Lossless,
            vip = false,
        )
    }
}
