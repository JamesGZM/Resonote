package com.resonote.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.resonote.core.model.OnlineSong
import com.resonote.feature.home.impl.HomePlaybackRequest

@Stable
internal class PrototypePlaybackState {
    var queue by mutableStateOf<List<OnlineSong>>(emptyList())
        private set
    var currentSongId by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set

    val currentSong: OnlineSong?
        get() = queue.firstOrNull { it.hash == currentSongId }

    fun play(request: HomePlaybackRequest) {
        queue = request.songs
        select(request.songs[request.startIndex])
    }

    fun play(song: OnlineSong) {
        val existingIndex = queue.indexOfFirst { it.hash == song.hash }
        if (existingIndex < 0) {
            val currentIndex = queue.indexOfFirst { it.hash == currentSongId }
            val insertionIndex = if (currentIndex < 0) queue.size else currentIndex + 1
            queue = queue.toMutableList().apply { add(insertionIndex, song) }
        }
        select(song)
    }

    fun togglePlay() {
        if (currentSong != null) isPlaying = !isPlaying
    }

    fun playNext() {
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.hash == currentSongId }.coerceAtLeast(0)
        select(queue[(currentIndex + 1).mod(queue.size)])
    }

    private fun select(song: OnlineSong) {
        currentSongId = song.hash
        isPlaying = true
        progress = 0f
    }
}

@Composable
internal fun rememberPrototypePlaybackState(): PrototypePlaybackState = remember { PrototypePlaybackState() }
