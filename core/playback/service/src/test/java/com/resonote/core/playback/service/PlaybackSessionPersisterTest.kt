package com.resonote.core.playback.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.PlaybackSessionRepository
import com.resonote.core.data.PlaybackSessionSnapshot
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.playback.PlaybackItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackSessionPersisterTest {
    @Test
    fun saveDoesNotWaitForStorageAndConflatesPendingSnapshots() = runTest {
        val repository = BlockingRepository()
        val persister = PlaybackSessionPersister(repository, backgroundScope)

        persister.save(listOf(item("first")), 0, 1_000, "ListLoop")
        runCurrent()
        repository.firstWriteStarted.await()
        persister.save(listOf(item("second")), 0, 2_000, "ListLoop")
        persister.save(listOf(item("latest")), 0, 3_000, "Shuffle")

        repository.releaseFirstWrite.complete(Unit)
        runCurrent()

        assertThat(repository.saved.map { it.entries.single().mediaId }).containsExactly("first", "latest").inOrder()
        assertThat(repository.saved.last().positionMillis).isEqualTo(3_000)
        assertThat(repository.saved.last().mode).isEqualTo("Shuffle")
    }

    @Test
    fun clearSupersedesPendingSave() = runTest {
        val repository = BlockingRepository()
        val persister = PlaybackSessionPersister(repository, backgroundScope)

        persister.save(listOf(item("first")), 0, 1_000, "ListLoop")
        runCurrent()
        repository.firstWriteStarted.await()
        persister.save(listOf(item("stale")), 0, 2_000, "ListLoop")
        persister.clear()

        repository.releaseFirstWrite.complete(Unit)
        runCurrent()

        assertThat(repository.saved.map { it.entries.single().mediaId }).containsExactly("first")
        assertThat(repository.clearCalls).isEqualTo(1)
    }

    private class BlockingRepository : PlaybackSessionRepository {
        val firstWriteStarted = CompletableDeferred<Unit>()
        val releaseFirstWrite = CompletableDeferred<Unit>()
        val saved = mutableListOf<PlaybackSessionSnapshot>()
        var clearCalls = 0

        override suspend fun load(): PlaybackSessionSnapshot? = null

        override suspend fun save(snapshot: PlaybackSessionSnapshot) {
            if (saved.isEmpty()) {
                firstWriteStarted.complete(Unit)
                releaseFirstWrite.await()
            }
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
