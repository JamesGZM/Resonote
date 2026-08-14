package com.resonote.feature.video.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.VideoRepository
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
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
class VideoViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun resolvedUrlBecomesReadyAndSameHashDoesNotReload() = runTest(dispatcher) {
        val repository = FakeVideoRepository(
            ArrayDeque(listOf(CollectionLoadResult.Available("https://media.example/video.mp4"))),
        )
        val viewModel = VideoViewModel(repository)

        viewModel.load("mv-hash")
        advanceUntilIdle()
        viewModel.load("mv-hash")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            VideoUiState.Ready("https://media.example/video.mp4"),
        )
        assertThat(repository.hashes).containsExactly("mv-hash")
    }

    @Test
    fun missingUrlBecomesUnavailable() = runTest(dispatcher) {
        val repository = FakeVideoRepository(
            ArrayDeque(listOf(CollectionLoadResult.Available(null))),
        )
        val viewModel = VideoViewModel(repository)

        viewModel.load("mv-hash")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(VideoUiState.Unavailable)
    }

    @Test
    fun typedFailureCanRecoverOnlyAfterExplicitRetry() = runTest(dispatcher) {
        val repository = FakeVideoRepository(
            ArrayDeque(
                listOf(
                    CollectionLoadResult.Failed(ContentFailure.Network),
                    CollectionLoadResult.Available("https://media.example/recovered.mp4"),
                ),
            ),
        )
        val viewModel = VideoViewModel(repository)

        viewModel.load("mv-hash")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isEqualTo(VideoUiState.Failed(ContentFailure.Network))

        viewModel.retry()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            VideoUiState.Ready("https://media.example/recovered.mp4"),
        )
        assertThat(repository.hashes).containsExactly("mv-hash", "mv-hash").inOrder()
    }

    private class FakeVideoRepository(private val results: ArrayDeque<CollectionLoadResult<String?>>) :
        VideoRepository {
        val hashes = mutableListOf<String>()

        override suspend fun resolveVideoUrl(hash: String): CollectionLoadResult<String?> {
            hashes += hash
            return results.removeFirst()
        }
    }
}
