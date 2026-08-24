package com.resonote.core.playback.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.PlaybackSessionRepository
import com.resonote.core.data.PlaybackSessionSnapshot
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackSessionCoordinatorTest {
    @Test
    fun persistsValidQueueAndClearsEmptyQueue() = runTest {
        val repository = RecordingRepository()
        val coordinator = PlaybackSessionCoordinator(repository, backgroundScope)
        val queue = PlaybackQueue().apply { replace(listOf(item("song")), 0) }

        coordinator.persist(
            queue = queue,
            state = PlaybackState(mode = PlaybackMode.Shuffle, positionMillis = 1_234),
        )
        runCurrent()

        assertThat(repository.saved.single().entries.single().mediaId).isEqualTo("song")
        assertThat(repository.saved.single().positionMillis).isEqualTo(1_234)
        assertThat(repository.saved.single().mode).isEqualTo("Shuffle")

        coordinator.persist(PlaybackQueue(), PlaybackState())
        runCurrent()

        assertThat(repository.clearCalls).isEqualTo(1)
    }

    @Test
    fun loadsSnapshotUsingRequestedPlaybackSpeed() = runTest {
        val snapshot = PlaybackSessionSnapshot(
            entries = listOf(item("song").toSessionEntry()),
            currentIndex = 0,
            positionMillis = 2_000,
            mode = PlaybackMode.ListLoop.name,
        )
        val coordinator = PlaybackSessionCoordinator(RecordingRepository(snapshot), backgroundScope)

        val restored = coordinator.load(PlaybackSpeed.OneAndQuarter)

        assertThat(restored?.currentItem?.metadata?.mediaId).isEqualTo("song")
        assertThat(restored?.playbackSpeed).isEqualTo(PlaybackSpeed.OneAndQuarter)
    }

    private class RecordingRepository(private val snapshot: PlaybackSessionSnapshot? = null) :
        PlaybackSessionRepository {
        val saved = mutableListOf<PlaybackSessionSnapshot>()
        var clearCalls = 0

        override suspend fun load(): PlaybackSessionSnapshot? = snapshot

        override suspend fun save(snapshot: PlaybackSessionSnapshot) {
            saved += snapshot
        }

        override suspend fun clear() {
            clearCalls += 1
        }
    }

    private fun item(id: String) = PlaybackItem(
        OnlineSong(
            hash = id,
            title = "Title $id",
            artist = "Artist",
            coverUrl = null,
            albumId = null,
            albumAudioId = null,
            durationMillis = 180_000,
            quality = AudioQuality.Standard,
            vip = false,
        ),
    )
}
