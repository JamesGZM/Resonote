package com.resonote.feature.home.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.HomeRepository
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.HomeContent
import com.resonote.core.model.HomeIssue
import com.resonote.core.model.HomePlaylist
import com.resonote.core.model.HomeRefreshResult
import com.resonote.core.model.HomeSection
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.RadioRecommendationResult
import com.resonote.core.model.RecommendationMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoadPublishesRepositoryContentWithoutPrefetchingRadio() = runTest(dispatcher) {
        val content = content()
        val repository = FakeHomeRepository(content)
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value as HomeUiState.Content
        assertThat(state.content.dailySongs.map { it.id }).containsExactly("daily-0", "daily-1").inOrder()
        assertThat(state.content.radio?.id).isEqualTo("daily-0")
        assertThat(repository.refreshCalls).isEqualTo(1)
        assertThat(repository.radioCalls).isEqualTo(0)
    }

    @Test
    fun radioPlaybackLoadsOnDemand() = runTest(dispatcher) {
        val repository = FakeHomeRepository(content())
        val viewModel = HomeViewModel(repository)
        advanceUntilIdle()

        val request = viewModel.radioPlaybackRequest()

        assertThat(request?.songs?.map { it.hash }).containsExactly("radio-0")
        assertThat(request?.startIndex).isEqualTo(0)
        assertThat(repository.radioCalls).isEqualTo(1)
    }

    @Test
    fun playbackRequestPreservesCollectionOrderAndClickedIndex() = runTest(dispatcher) {
        val repository = FakeHomeRepository(content())
        val viewModel = HomeViewModel(repository)
        advanceUntilIdle()

        val request = viewModel.playbackRequest(HomeSongCollection.DAILY_RECOMMENDATIONS, "daily-1")

        assertThat(request?.songs?.map { it.hash }).containsExactly("daily-0", "daily-1").inOrder()
        assertThat(request?.startIndex).isEqualTo(1)
    }

    @Test
    fun firstLoadFailurePublishesRetryableError() = runTest(dispatcher) {
        val issue = HomeIssue(HomeSection.DailyRecommendations, ContentFailure.Network)
        val repository = FakeHomeRepository(initialContent = null, refreshResult = HomeRefreshResult.Failed(listOf(issue)))
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value as HomeUiState.Error
        assertThat(state.issues).containsExactly(HomeSection.DailyRecommendations)
    }

    @Test
    fun partialFailureKeepsContentAndExposesFailedSection() = runTest(dispatcher) {
        val content = content()
        val issue = HomeIssue(HomeSection.NewSongs, ContentFailure.Protocol)
        val repository = FakeHomeRepository(content, HomeRefreshResult.Updated(content, listOf(issue)))
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value as HomeUiState.Content
        assertThat(state.content.newSongs).isNotEmpty()
        assertThat(state.issues).containsExactly(HomeSection.NewSongs)
    }

    private class FakeHomeRepository(
        initialContent: HomeContent?,
        private val refreshResult: HomeRefreshResult =
            requireNotNull(initialContent).let { HomeRefreshResult.Updated(it, emptyList()) },
    ) : HomeRepository {
        private val mutableContent = MutableStateFlow(initialContent)
        override val content: StateFlow<HomeContent?> = mutableContent
        var refreshCalls = 0
        var radioCalls = 0

        override suspend fun refresh(): HomeRefreshResult {
            refreshCalls += 1
            if (refreshResult is HomeRefreshResult.Updated) mutableContent.value = refreshResult.content
            return refreshResult
        }

        override suspend fun loadRadio(mode: RecommendationMode): RadioRecommendationResult {
            radioCalls += 1
            return RadioRecommendationResult.Available(listOf(song("radio-0")))
        }
    }

    private companion object {
        fun content() = HomeContent(
            dailyRecommendations = listOf(song("daily-0"), song("daily-1")),
            recommendedPlaylists = listOf(HomePlaylist("playlist-0", "Playlist", null, 12_000)),
            newSongs = listOf(song("new-0")),
        )

        fun song(id: String) = OnlineSong(
            hash = id,
            title = "Title $id",
            artist = "Artist",
            coverUrl = null,
            albumId = "1",
            albumAudioId = "2",
            durationMillis = 180_000,
            quality = AudioQuality.Lossless,
            vip = false,
        )
    }
}
