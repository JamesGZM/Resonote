package com.resonote.core.playback.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaId
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.PlaybackItem
import org.junit.Test

class PlaybackQueueTest {
    @Test
    fun selectingNewSongInsertsItAfterCurrentWithoutReplacingQueue() {
        val queue = PlaybackQueue().apply { replace(listOf(item("first"), item("second")), 0) }

        queue.selectOrInsert(item("search"))

        assertThat(queue.items.map { it.metadata.mediaId }).containsExactly("first", "search", "second").inOrder()
        assertThat(queue.currentItem?.metadata?.mediaId).isEqualTo("search")
    }

    @Test
    fun replacingQueueDeduplicatesAndKeepsRequestedSongSelected() {
        val queue = PlaybackQueue()

        queue.replace(listOf(item("first"), item("second"), item("first")), 2)

        assertThat(queue.items.map { it.metadata.mediaId }).containsExactly("first", "second").inOrder()
        assertThat(queue.currentItem?.metadata?.mediaId).isEqualTo("first")
    }

    @Test
    fun sameRawIdFromOnlineAndLocalRemainIndependentQueueItems() {
        val queue = PlaybackQueue()

        queue.replace(listOf(item("shared"), PlaybackItem(localMedia("shared"))), 1)

        assertThat(queue.items.map(PlaybackItem::queueKey))
            .containsExactly("online:shared", "local:shared")
            .inOrder()
        assertThat(queue.currentItem?.queueKey).isEqualTo("local:shared")
    }

    @Test
    fun appendingResolvedDuplicateUpgradesExistingQueueItem() {
        val queue = PlaybackQueue().apply { replace(listOf(item("cloud")), 0) }
        val source = ResolvedSongSource("https://media.example/cloud.mp3", 60_000, "mp3")

        queue.append(listOf(item("cloud", source), item("next")))

        assertThat(queue.items.map { it.metadata.mediaId }).containsExactly("cloud", "next").inOrder()
        assertThat(queue.items.first().resolvedSource).isEqualTo(source)
    }

    @Test
    fun sequentialNavigationStopsAtEdgesWhileLoopNavigationWraps() {
        val queue = PlaybackQueue().apply { replace(listOf(item("first"), item("second")), 1) }

        assertThat(queue.next(wrap = false)).isNull()
        assertThat(queue.next(wrap = true)?.metadata?.mediaId).isEqualTo("first")
        assertThat(queue.previous(wrap = true)?.metadata?.mediaId).isEqualTo("second")
    }

    @Test
    fun shuffleNeverSelectsCurrentItemWhenAlternativesExist() {
        val queue = PlaybackQueue().apply { replace(listOf(item("first"), item("second")), 0) }

        val selected = queue.selectRandom { 0 }

        assertThat(selected?.metadata?.mediaId).isEqualTo("second")
    }

    @Test
    fun removingCurrentSelectsFollowingItemOrNewLastItem() {
        val queue = PlaybackQueue().apply {
            replace(listOf(item("first"), item("second"), item("third")), 1)
        }

        val middleRemoval = queue.removeAt(1)

        assertThat(middleRemoval?.removedCurrent).isTrue()
        assertThat(queue.currentItem?.metadata?.mediaId).isEqualTo("third")

        val lastRemoval = queue.removeAt(1)

        assertThat(lastRemoval?.removedCurrent).isTrue()
        assertThat(queue.currentItem?.metadata?.mediaId).isEqualTo("first")
    }

    @Test
    fun removingItemBeforeCurrentKeepsSameSongSelected() {
        val queue = PlaybackQueue().apply {
            replace(listOf(item("first"), item("second"), item("third")), 2)
        }

        val removal = queue.removeAt(0)

        assertThat(removal?.removedCurrent).isFalse()
        assertThat(queue.currentIndex).isEqualTo(1)
        assertThat(queue.currentItem?.metadata?.mediaId).isEqualTo("third")
    }

    @Test
    fun movingItemsPreservesCurrentSongSelection() {
        val queue = PlaybackQueue().apply {
            replace(listOf(item("first"), item("second"), item("third")), 1)
        }

        assertThat(queue.move(2, 0)).isTrue()

        assertThat(queue.items.map { it.metadata.mediaId }).containsExactly("third", "first", "second").inOrder()
        assertThat(queue.currentIndex).isEqualTo(2)
        assertThat(queue.currentItem?.metadata?.mediaId).isEqualTo("second")
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

    private fun localMedia(id: String) = LocalMedia(
        id = LocalMediaId(id),
        displayName = "$id.flac",
        title = id,
        artist = "artist",
        albumTitle = null,
        artworkUri = null,
        durationMillis = 60_000,
        mimeType = "audio/flac",
        fileExtension = "flac",
        sizeBytes = 4_096,
        sampleRateHz = 96_000,
        bitDepth = 24,
        bitrateBitsPerSecond = 2_304_000,
        importedAtEpochMillis = 1_000,
    )
}
