package com.resonote.feature.player.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.LyricsRepository
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaId
import com.resonote.core.model.LyricLine
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.playback.PlaybackController
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun currentSongLoadsSortedLyricsFromRealRepositoryBoundary() = runTest(dispatcher) {
        val controller = FakePlaybackController(song("first"))
        val repository = FakeLyricsRepository(
            CollectionLoadResult.Available(listOf(LyricLine(2_000, "second"), LyricLine(1_000, "first"))),
        )
        val viewModel = PlayerViewModel(controller, repository)
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }

        advanceUntilIdle()

        assertThat(repository.requests).containsExactly("first" to "audio-first")
        assertThat((viewModel.uiState.value.lyrics as LyricsUiState.Content).lines.map { it.text })
            .containsExactly("first", "second").inOrder()
        collection.cancel()
    }

    @Test
    fun songChangeReloadsLyricsAndFailureLeavesPlaybackUntouched() = runTest(dispatcher) {
        val controller = FakePlaybackController(song("first"))
        val repository = FakeLyricsRepository(CollectionLoadResult.Failed(ContentFailure.Network))
        val viewModel = PlayerViewModel(controller, repository)
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        controller.setSong(song("second"))
        advanceUntilIdle()

        assertThat(repository.requests).containsExactly(
            "first" to "audio-first",
            "second" to "audio-second",
        ).inOrder()
        assertThat(viewModel.uiState.value.lyrics).isEqualTo(LyricsUiState.Error(ContentFailure.Network))
        assertThat(viewModel.uiState.value.playback.currentMetadata?.mediaId).isEqualTo("second")
        collection.cancel()
    }

    @Test
    fun lyricSeekAndVisibleQueueCommandsDelegateToSharedController() {
        val controller = FakePlaybackController(song("first"))
        val viewModel = PlayerViewModel(controller, FakeLyricsRepository(CollectionLoadResult.Available(emptyList())))

        viewModel.seekTo(42_000)
        viewModel.selectQueueItem(2)
        viewModel.removeQueueItem(1)
        viewModel.setMode(PlaybackMode.Shuffle)
        viewModel.setPlaybackSpeed(PlaybackSpeed.OneAndHalf)

        assertThat(controller.seekPosition).isEqualTo(42_000)
        assertThat(controller.selectedIndex).isEqualTo(2)
        assertThat(controller.removedIndex).isEqualTo(1)
        assertThat(controller.selectedMode).isEqualTo(PlaybackMode.Shuffle)
        assertThat(controller.selectedSpeed).isEqualTo(PlaybackSpeed.OneAndHalf)
    }

    @Test
    fun localMediaDoesNotRequestOnlineLyrics() = runTest(dispatcher) {
        val controller = FakePlaybackController(PlaybackItem(localMedia()))
        val repository = FakeLyricsRepository(CollectionLoadResult.Available(emptyList()))
        val viewModel = PlayerViewModel(controller, repository)
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }

        advanceUntilIdle()

        assertThat(repository.requests).isEmpty()
        assertThat(viewModel.uiState.value.lyrics).isEqualTo(LyricsUiState.Unavailable)
        collection.cancel()
    }

    private class FakeLyricsRepository(private val result: CollectionLoadResult<List<LyricLine>>) : LyricsRepository {
        val requests = mutableListOf<Pair<String, String?>>()

        override suspend fun loadLyrics(hash: String, albumAudioId: String?): CollectionLoadResult<List<LyricLine>> {
            requests += hash to albumAudioId
            return result
        }
    }

    private class FakePlaybackController(initialItem: PlaybackItem) : PlaybackController {
        constructor(initialSong: OnlineSong) : this(PlaybackItem(initialSong))

        override val state = MutableStateFlow(PlaybackState(queue = listOf(initialItem), currentIndex = 0))
        var seekPosition = -1L
        var selectedIndex = -1
        var removedIndex = -1
        var selectedMode: PlaybackMode? = null
        var selectedSpeed: PlaybackSpeed? = null

        fun setSong(song: OnlineSong) {
            state.value = PlaybackState(queue = listOf(PlaybackItem(song)), currentIndex = 0)
        }

        override fun play(item: PlaybackItem) = Unit
        override fun playAll(items: List<PlaybackItem>, startIndex: Int) = Unit
        override fun playNext(items: List<PlaybackItem>) = Unit
        override fun append(items: List<PlaybackItem>) = Unit
        override fun selectQueueItem(index: Int) {
            selectedIndex = index
        }
        override fun removeQueueItem(index: Int) {
            removedIndex = index
        }
        override fun moveQueueItem(fromIndex: Int, toIndex: Int) = Unit
        override fun togglePlayPause() = Unit
        override fun pause() = Unit
        override fun next() = Unit
        override fun previous() = Unit
        override fun seekTo(positionMillis: Long) {
            seekPosition = positionMillis
        }
        override fun setMode(mode: PlaybackMode) {
            selectedMode = mode
        }
        override fun setPlaybackSpeed(speed: PlaybackSpeed) {
            selectedSpeed = speed
        }
        override fun refreshCurrentOnlineSource(force: Boolean) = Unit
        override fun clear() = Unit
    }

    private companion object {
        fun song(hash: String) = OnlineSong(
            hash = hash,
            title = "潮汐记忆",
            artist = "林澈",
            coverUrl = null,
            albumId = "album",
            albumAudioId = "audio-$hash",
            durationMillis = 248_000,
            quality = AudioQuality.Lossless,
            vip = false,
        )

        fun localMedia() = LocalMedia(
            id = LocalMediaId("local-id"),
            displayName = "signals.flac",
            title = "Signals",
            artist = "artist",
            albumTitle = "album",
            artworkUri = null,
            durationMillis = 180_000,
            mimeType = "audio/flac",
            fileExtension = "flac",
            sizeBytes = 4_096,
            sampleRateHz = 96_000,
            bitDepth = 24,
            bitrateBitsPerSecond = 2_304_000,
            importedAtEpochMillis = 1_000,
        )
    }
}
