package com.resonote.app

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlineSong
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
}
