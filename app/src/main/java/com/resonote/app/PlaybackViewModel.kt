package com.resonote.app

import androidx.lifecycle.ViewModel
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.DeviceHistoryItem
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.MusicDownloadController
import com.resonote.core.playback.PlaybackController
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
internal class PlaybackViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val musicDownloadController: MusicDownloadController,
) : ViewModel() {
    constructor(playbackController: PlaybackController) : this(playbackController, EmptyMusicDownloadController)

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
                track = track,
                resolvedSource = source.takeIf { index == startIndex },
            )
        }
        playbackController.playAll(items, startIndex)
    }

    fun appendCloud(tracks: List<CloudTrack>) {
        playbackController.append(
            tracks.map(::PlaybackItem),
        )
    }

    fun appendOnline(song: OnlineSong) {
        playbackController.append(listOf(PlaybackItem(song)))
    }

    fun playNextOnline(song: OnlineSong): Boolean {
        val item = PlaybackItem(song)
        if (state.value.currentItem == null) {
            playbackController.play(item)
            return false
        } else {
            playbackController.playNext(listOf(item))
            return true
        }
    }

    fun playLocal(media: LocalMedia) {
        playbackController.play(PlaybackItem(media))
    }

    fun playAllLocal(media: List<LocalMedia>, startIndex: Int = 0) {
        playbackController.playAll(media.map(::PlaybackItem), startIndex)
    }

    fun playLocalItems(items: List<PlaybackItem>, startIndex: Int = 0) {
        playbackController.playAll(items, startIndex)
    }

    fun playLocalItem(item: PlaybackItem) {
        playbackController.play(item)
    }

    fun download(song: OnlineSong) = musicDownloadController.download(song)

    fun playDeviceHistory(items: List<DeviceHistoryItem>, startIndex: Int = 0) {
        playbackController.playAll(items.map { PlaybackItem(it.record) }, startIndex)
    }

    fun togglePlayPause() = playbackController.togglePlayPause()

    fun pause() = playbackController.pause()

    fun next() = playbackController.next()

    fun seekTo(positionMillis: Long) = playbackController.seekTo(positionMillis)

    fun setMode(mode: PlaybackMode) = playbackController.setMode(mode)

    fun refreshCurrentOnlineSource(force: Boolean = false) = playbackController.refreshCurrentOnlineSource(force)

    fun selectQueueItem(index: Int) = playbackController.selectQueueItem(index)

    fun removeQueueItem(index: Int) = playbackController.removeQueueItem(index)

    fun clearQueue() = playbackController.clear()

    private object EmptyMusicDownloadController : MusicDownloadController {
        override val downloads = kotlinx.coroutines.flow.MutableStateFlow(
            emptyList<com.resonote.core.playback.MusicDownload>(),
        )
        override fun download(song: OnlineSong) = Unit
        override fun pause(id: String) = Unit
        override fun resume(id: String) = Unit
        override fun retry(id: String) = Unit
        override fun remove(id: String) = Unit
        override fun pauseAll() = Unit
        override fun resumeAll() = Unit
        override fun completedSource(
            songHash: String,
            quality: com.resonote.core.model.OnlinePlaybackQuality?,
        ): ResolvedSongSource? = null
    }
}
