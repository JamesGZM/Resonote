package com.resonote.app

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.ResolvedSongSource
import com.resonote.feature.cloud.impl.CloudPlaybackRequest
import com.resonote.feature.home.impl.HomePlaybackRequest
import org.junit.Test

class PrototypePlaybackStateTest {
    @Test
    fun searchSongIsInsertedAfterCurrentWithoutReplacingQueue() {
        val state = PrototypePlaybackState()
        state.play(HomePlaybackRequest(listOf(song("first"), song("second")), 0))

        state.play(song("search"))

        assertThat(state.queue.map { it.hash }).containsExactly("first", "search", "second").inOrder()
        assertThat(state.currentSongId).isEqualTo("search")
        assertThat(state.isPlaying).isTrue()
    }

    @Test
    fun existingSearchSongIsSelectedWithoutCreatingDuplicate() {
        val state = PrototypePlaybackState()
        state.play(HomePlaybackRequest(listOf(song("first"), song("second")), 0))

        state.play(song("second"))

        assertThat(state.queue.map { it.hash }).containsExactly("first", "second").inOrder()
        assertThat(state.currentSongId).isEqualTo("second")
    }

    @Test
    fun playlistPlayAllReplacesQueueAndStartsAtFirstSong() {
        val state = PrototypePlaybackState()
        state.play(song("existing"))

        state.playAll(listOf(song("playlist-1"), song("playlist-2")))

        assertThat(state.queue.map { it.hash }).containsExactly("playlist-1", "playlist-2").inOrder()
        assertThat(state.currentSongId).isEqualTo("playlist-1")
        assertThat(state.isPlaying).isTrue()
    }

    @Test
    fun cloudPlaybackUsesUnifiedQueueAndPreservesResolvedSource() {
        val state = PrototypePlaybackState()
        val tracks = listOf(cloudTrack("cloud-1"), cloudTrack("cloud-2"))
        val source = ResolvedSongSource("https://media.example/cloud.mp3", 240_000, "mp3")

        state.playCloud(CloudPlaybackRequest(tracks, startIndex = 1, source = source))

        assertThat(state.queue.map { it.hash }).containsExactly("cloud-1", "cloud-2").inOrder()
        assertThat(state.currentSongId).isEqualTo("cloud-2")
        assertThat(state.currentResolvedSource).isEqualTo(source)
    }

    @Test
    fun cloudAppendAddsToExistingQueueWithoutDuplicates() {
        val state = PrototypePlaybackState()
        state.playAll(listOf(song("online"), song("cloud-1")))

        state.appendCloud(listOf(cloudTrack("cloud-1"), cloudTrack("cloud-2")))

        assertThat(state.queue.map { it.hash }).containsExactly("online", "cloud-1", "cloud-2").inOrder()
        assertThat(state.currentSongId).isEqualTo("online")
    }

    private fun song(id: String) = OnlineSong(
        hash = id,
        title = id,
        artist = "artist",
        coverUrl = null,
        albumId = null,
        albumAudioId = null,
        durationMillis = 60_000,
        quality = AudioQuality.Standard,
        vip = false,
    )

    private fun cloudTrack(hash: String) = CloudTrack(
        hash = hash,
        title = "Cloud $hash",
        artist = "Artist",
        album = "Album",
        coverUrl = null,
        durationMillis = 240_000,
        albumAudioId = "audio-$hash",
    )
}
