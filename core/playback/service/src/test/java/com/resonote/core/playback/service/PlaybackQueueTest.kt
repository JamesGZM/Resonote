package com.resonote.core.playback.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.PlaybackItem
import org.junit.Test

class PlaybackQueueTest {
    @Test
    fun selectingNewSongInsertsItAfterCurrentWithoutReplacingQueue() {
        val queue = PlaybackQueue().apply { replace(listOf(item("first"), item("second")), 0) }

        queue.selectOrInsert(item("search"))

        assertThat(queue.items.map { it.song.hash }).containsExactly("first", "search", "second").inOrder()
        assertThat(queue.currentItem?.song?.hash).isEqualTo("search")
    }

    @Test
    fun replacingQueueDeduplicatesAndKeepsRequestedSongSelected() {
        val queue = PlaybackQueue()

        queue.replace(listOf(item("first"), item("second"), item("first")), 2)

        assertThat(queue.items.map { it.song.hash }).containsExactly("first", "second").inOrder()
        assertThat(queue.currentItem?.song?.hash).isEqualTo("first")
    }

    @Test
    fun appendingResolvedDuplicateUpgradesExistingQueueItem() {
        val queue = PlaybackQueue().apply { replace(listOf(item("cloud")), 0) }
        val source = ResolvedSongSource("https://media.example/cloud.mp3", 60_000, "mp3")

        queue.append(listOf(item("cloud", source), item("next")))

        assertThat(queue.items.map { it.song.hash }).containsExactly("cloud", "next").inOrder()
        assertThat(queue.items.first().resolvedSource).isEqualTo(source)
    }

    @Test
    fun sequentialNavigationStopsAtEdgesWhileLoopNavigationWraps() {
        val queue = PlaybackQueue().apply { replace(listOf(item("first"), item("second")), 1) }

        assertThat(queue.next(wrap = false)).isNull()
        assertThat(queue.next(wrap = true)?.song?.hash).isEqualTo("first")
        assertThat(queue.previous(wrap = true)?.song?.hash).isEqualTo("second")
    }

    @Test
    fun shuffleNeverSelectsCurrentItemWhenAlternativesExist() {
        val queue = PlaybackQueue().apply { replace(listOf(item("first"), item("second")), 0) }

        val selected = queue.selectRandom { 0 }

        assertThat(selected?.song?.hash).isEqualTo("second")
    }

    private fun item(hash: String, source: ResolvedSongSource? = null) = PlaybackItem(
        song = OnlineSong(
            hash = hash,
            title = hash,
            artist = "artist",
            coverUrl = null,
            albumId = null,
            albumAudioId = null,
            durationMillis = 60_000,
            quality = AudioQuality.Standard,
            vip = false,
        ),
        resolvedSource = source,
    )
}
