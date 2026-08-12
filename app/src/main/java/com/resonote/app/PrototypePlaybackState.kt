package com.resonote.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.ResolvedSongSource
import com.resonote.feature.cloud.impl.CloudPlaybackRequest
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
    var currentResolvedSource by mutableStateOf<ResolvedSongSource?>(null)
        private set

    val currentSong: OnlineSong?
        get() = queue.firstOrNull { it.hash == currentSongId }

    fun play(request: HomePlaybackRequest) {
        playAll(request.songs, request.startIndex)
    }

    fun playAll(songs: List<OnlineSong>, startIndex: Int = 0) {
        require(songs.isNotEmpty()) { "Playback queue must not be empty" }
        require(startIndex in songs.indices) { "startIndex must point to a song" }
        queue = songs
        currentResolvedSource = null
        select(songs[startIndex])
    }

    fun play(song: OnlineSong) {
        val existingIndex = queue.indexOfFirst { it.hash == song.hash }
        if (existingIndex < 0) {
            val currentIndex = queue.indexOfFirst { it.hash == currentSongId }
            val insertionIndex = if (currentIndex < 0) queue.size else currentIndex + 1
            queue = queue.toMutableList().apply { add(insertionIndex, song) }
        }
        currentResolvedSource = null
        select(song)
    }

    fun playCloud(request: CloudPlaybackRequest) {
        playAll(request.tracks.map(CloudTrack::toOnlineSong), request.startIndex)
        currentResolvedSource = request.source
    }

    fun appendCloud(tracks: List<CloudTrack>) {
        val existingHashes = queue.mapTo(mutableSetOf(), OnlineSong::hash)
        queue = queue + tracks.filter { existingHashes.add(it.hash) }.map(CloudTrack::toOnlineSong)
    }

    fun togglePlay() {
        if (currentSong != null) isPlaying = !isPlaying
    }

    fun playNext() {
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.hash == currentSongId }.coerceAtLeast(0)
        currentResolvedSource = null
        select(queue[(currentIndex + 1).mod(queue.size)])
    }

    private fun select(song: OnlineSong) {
        currentSongId = song.hash
        isPlaying = true
        progress = 0f
    }
}

private fun CloudTrack.toOnlineSong() = OnlineSong(
    hash = hash,
    title = title,
    artist = artist,
    coverUrl = coverUrl,
    albumId = null,
    albumAudioId = albumAudioId,
    durationMillis = durationMillis,
    quality = AudioQuality.Standard,
    vip = false,
    albumTitle = album,
)

@Composable
internal fun rememberPrototypePlaybackState(): PrototypePlaybackState = remember { PrototypePlaybackState() }
