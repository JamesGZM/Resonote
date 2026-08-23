package com.resonote.feature.cloud.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.CloudRepository
import com.resonote.core.model.CloudPage
import com.resonote.core.model.CloudStorage
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.PlaybackUnavailableReason
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CloudViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun initialLoadPublishesTracksStorageAndPaging() = runTest(dispatcher) {
        val repository = FakeCloudRepository(
            pages = mutableMapOf(1 to availablePage(1, listOf(track("one")), total = 2, hasMore = true)),
        )
        val viewModel = CloudViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.initialLoading).isFalse()
        assertThat(state.tracks.map { it.hash }).containsExactly("one")
        assertThat(state.total).isEqualTo(2)
        assertThat(state.hasMore).isTrue()
        assertThat(state.storage).isEqualTo(CloudStorage(usedBytes = 256, maxBytes = 1_024))
        assertThat(repository.pageRequests).containsExactly(1)
    }

    @Test
    fun loadMoreDeduplicatesTracksAndKeepsLatestPage() = runTest(dispatcher) {
        val repository = FakeCloudRepository(
            pages = mutableMapOf(
                1 to availablePage(1, listOf(track("one"), track("shared")), total = 3, hasMore = true),
                2 to availablePage(2, listOf(track("shared"), track("two")), total = 3, hasMore = false),
            ),
        )
        val viewModel = CloudViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.tracks.map { it.hash }).containsExactly("one", "shared", "two").inOrder()
        assertThat(state.page).isEqualTo(2)
        assertThat(state.hasMore).isFalse()
    }

    @Test
    fun searchLoadsAllRemainingPagesBeforeFiltering() = runTest(dispatcher) {
        val repository = FakeCloudRepository(
            pages = mutableMapOf(
                1 to availablePage(1, listOf(track("one", title = "港口")), total = 3, hasMore = true),
                2 to availablePage(2, listOf(track("two", artist = "林澈")), total = 3, hasMore = true),
                3 to availablePage(3, listOf(track("three", title = "潮汐")), total = 3, hasMore = false),
            ),
        )
        val viewModel = CloudViewModel(repository)

        viewModel.updateQuery("林澈")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(repository.pageRequests).containsExactly(1, 2, 3).inOrder()
        assertThat(state.isIndexing).isFalse()
        assertThat(state.hasMore).isFalse()
        assertThat(state.visibleTracks.map { it.hash }).containsExactly("two")
    }

    @Test
    fun sortIsAppliedWithoutReloading() = runTest(dispatcher) {
        val repository = FakeCloudRepository(
            pages = mutableMapOf(
                1 to availablePage(
                    1,
                    listOf(
                        track("z", title = "终点", artist = "Beta", duration = 100_000),
                        track("a", title = "岸线", artist = "Alpha", duration = 300_000),
                    ),
                    total = 2,
                    hasMore = false,
                ),
            ),
        )
        val viewModel = CloudViewModel(repository)
        advanceUntilIdle()

        viewModel.updateSort(CloudSort.Duration)

        assertThat(viewModel.uiState.value.visibleTracks.map { it.hash }).containsExactly("a", "z").inOrder()
        assertThat(repository.pageRequests).containsExactly(1)
    }

    @Test
    fun playbackRequestEmitsOnlyAfterRealSourceResolution() = runTest(dispatcher) {
        val source = ResolvedSongSource("https://media.example/cloud.mp3", 240_000, "mp3")
        val repository = FakeCloudRepository(
            pages = mutableMapOf(1 to availablePage(1, listOf(track("one"), track("two")), 2, false)),
            resolveResults = ArrayDeque(listOf(ResolveSongSourceResult.Resolved(source))),
        )
        val viewModel = CloudViewModel(repository)
        val requests = mutableListOf<CloudPlaybackRequest>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.playbackRequests.collect(requests::add)
        }
        advanceUntilIdle()

        viewModel.playTrack("two")
        assertThat(requests).isEmpty()
        advanceUntilIdle()

        assertThat(repository.resolvedHashes).containsExactly("two")
        assertThat(requests).hasSize(1)
        assertThat(requests.single().startIndex).isEqualTo(1)
        assertThat(requests.single().source).isEqualTo(source)
        assertThat(viewModel.uiState.value.playback).isEqualTo(CloudPlaybackUiState.Idle)
    }

    @Test
    fun unavailablePlaybackDoesNotEmitAndCanRetry() = runTest(dispatcher) {
        val source = ResolvedSongSource("https://media.example/recovered.mp3", 200_000, "mp3")
        val repository = FakeCloudRepository(
            pages = mutableMapOf(1 to availablePage(1, listOf(track("one")), 1, false)),
            resolveResults = ArrayDeque(
                listOf(
                    ResolveSongSourceResult.Unavailable(PlaybackUnavailableReason.Cloud),
                    ResolveSongSourceResult.Resolved(source),
                ),
            ),
        )
        val viewModel = CloudViewModel(repository)
        val requests = mutableListOf<CloudPlaybackRequest>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.playbackRequests.collect(requests::add)
        }
        advanceUntilIdle()

        viewModel.playAll()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.playback).isEqualTo(
            CloudPlaybackUiState.Failed("one", CloudPlaybackIssue.Unavailable),
        )
        assertThat(requests).isEmpty()

        viewModel.retryPlayback()
        advanceUntilIdle()
        assertThat(requests).hasSize(1)
        assertThat(repository.resolvedHashes).containsExactly("one", "one").inOrder()
    }

    @Test
    fun initialFailureRemainsTypedAndRefreshCanRecover() = runTest(dispatcher) {
        val repository = FakeCloudRepository(
            pages = mutableMapOf(
                1 to CollectionLoadResult.Failed(ContentFailure.Network),
            ),
        )
        val viewModel = CloudViewModel(repository)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.failure).isEqualTo(ContentFailure.Network)

        repository.pages[1] = availablePage(1, listOf(track("recovered")), 1, false)
        viewModel.refresh()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.failure).isNull()
        assertThat(viewModel.uiState.value.tracks.single().hash).isEqualTo("recovered")
    }

    private class FakeCloudRepository(
        val pages: MutableMap<Int, CollectionLoadResult<CloudPage>>,
        private val resolveResults: ArrayDeque<ResolveSongSourceResult> = ArrayDeque(),
    ) : CloudRepository {
        val pageRequests = mutableListOf<Int>()
        val resolvedHashes = mutableListOf<String>()

        override suspend fun loadTracks(page: Int, pageSize: Int): CollectionLoadResult<CloudPage> {
            pageRequests += page
            return requireNotNull(pages[page]) { "Missing fake page $page" }
        }

        override suspend fun resolveSource(track: CloudTrack): ResolveSongSourceResult {
            resolvedHashes += track.hash
            return resolveResults.removeFirst()
        }
    }

    private companion object {
        fun availablePage(page: Int, tracks: List<CloudTrack>, total: Int, hasMore: Boolean) =
            CollectionLoadResult.Available(
                CloudPage(
                    tracks = tracks,
                    page = page,
                    total = total,
                    hasMore = hasMore,
                    storage = if (page == 1) CloudStorage(256, 1_024) else null,
                ),
            )

        fun track(hash: String, title: String = "Track $hash", artist: String = "Artist", duration: Long = 240_000) =
            CloudTrack(
                hash = hash,
                title = title,
                artist = artist,
                album = "Album",
                coverUrl = null,
                durationMillis = duration,
                albumAudioId = "audio-$hash",
            )
    }
}
