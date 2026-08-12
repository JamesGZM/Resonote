package com.resonote.app

import androidx.lifecycle.ViewModel
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.PlaybackController
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackOrigin
import com.resonote.core.playback.PlaybackMode
import com.resonote.core.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
internal class PlaybackViewModel @Inject constructor(
    private val playbackController: PlaybackController,
) : ViewModel() {
    val state: StateFlow<PlaybackState> = playbackController.state

    fun play(song: OnlineSong) {
        playbackController.play(PlaybackItem(song))
    }

    fun playAll(songs: List<OnlineSong>, startIndex: Int = 0) {
        playbackController.playAll(songs.map(::PlaybackItem), startIndex)
    }

    fun playCloud(tracks: List<CloudTrack>, startIndex: Int, source: ResolvedSongSource) {
        val items = tracks.mapIndexed { index, track ->
            PlaybackItem(
                song = track.toOnlineSong(),
                origin = PlaybackOrigin.Cloud(track),
                resolvedSource = source.takeIf { index == startIndex },
            )
        }
        playbackController.playAll(items, startIndex)
    }

    fun appendCloud(tracks: List<CloudTrack>) {
        playbackController.append(
            tracks.map { PlaybackItem(it.toOnlineSong(), origin = PlaybackOrigin.Cloud(it)) },
        )
    }

    fun togglePlayPause() = playbackController.togglePlayPause()

    fun pause() = playbackController.pause()

    fun next() = playbackController.next()

    fun seekTo(positionMillis: Long) = playbackController.seekTo(positionMillis)

    fun setMode(mode: PlaybackMode) = playbackController.setMode(mode)

    fun selectQueueItem(index: Int) = playbackController.selectQueueItem(index)

    fun removeQueueItem(index: Int) = playbackController.removeQueueItem(index)

    fun moveQueueItem(fromIndex: Int, toIndex: Int) = playbackController.moveQueueItem(fromIndex, toIndex)

    fun clearQueue() = playbackController.clear()
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
