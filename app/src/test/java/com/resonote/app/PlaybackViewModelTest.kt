package com.resonote.app

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.DeviceHistoryItem
import com.resonote.core.model.DeviceHistoryRecord
import com.resonote.core.model.DeviceHistorySource
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaId
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.PlaybackController
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackFormat
import com.resonote.core.playback.PlaybackMode
import com.resonote.core.playback.PlaybackOrigin
import com.resonote.core.playback.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class PlaybackViewModelTest {
    @Test
    fun playAllMapsOnlineSongsToOnePlaybackGeneration() {
        val controller = FakePlaybackController()
        val viewModel = PlaybackViewModel(controller)

        viewModel.playAll(listOf(song("first"), song("second")), startIndex = 1)

        assertThat(controller.playedItems.map { it.metadata.mediaId }).containsExactly("first", "second").inOrder()
        assertThat(controller.playedIndex).isEqualTo(1)
    }

    @Test
    fun cloudPlaybackAttachesResolvedSourceOnlyToSelectedTrack() {
        val controller = FakePlaybackController()
        val viewModel = PlaybackViewModel(controller)
        val source = ResolvedSongSource("https://media.example/cloud.mp3", 180_000, "mp3")

        viewModel.playCloud(listOf(cloud("first"), cloud("second")), startIndex = 1, source = source)

        assertThat(controller.playedItems.map { it.metadata.mediaId }).containsExactly("first", "second").inOrder()
        assertThat(controller.playedItems[0].resolvedSource).isNull()
        assertThat(controller.playedItems[1].resolvedSource).isEqualTo(source)
        assertThat((controller.playedItems[0].origin as PlaybackOrigin.Cloud).track.hash).isEqualTo("first")
    }

    @Test
    fun cloudAppendAndInterruptionsDelegateWithoutReplacingCurrentQueue() {
        val controller = FakePlaybackController()
        val viewModel = PlaybackViewModel(controller)

        viewModel.appendCloud(listOf(cloud("first"), cloud("second")))
        viewModel.pause()

        assertThat(controller.appendedItems.map { it.metadata.mediaId }).containsExactly("first", "second").inOrder()
        assertThat(controller.appendedItems.map { (it.origin as PlaybackOrigin.Cloud).track.hash })
            .containsExactly("first", "second")
            .inOrder()
        assertThat(controller.pauseCalls).isEqualTo(1)
        assertThat(controller.playedItems).isEmpty()
    }

    @Test
    fun localPlaybackKeepsActualFormatAndStableLocalIdentity() {
        val controller = FakePlaybackController()
        val viewModel = PlaybackViewModel(controller)

        viewModel.playLocal(localMedia())

        val item = controller.playedItems.single()
        assertThat(item.queueKey).isEqualTo("local:local-id")
        assertThat((item.origin as PlaybackOrigin.Local).id).isEqualTo(LocalMediaId("local-id"))
        assertThat(item.metadata.format).isEqualTo(
            PlaybackFormat.Local("audio/flac", "flac", 96_000, 24, 2_304_000),
        )
    }

    @Test
    fun deviceHistoryRebuildsMixedOriginsAndKeepsSelectedIndex() {
        val controller = FakePlaybackController()
        val viewModel = PlaybackViewModel(controller)
        val items = listOf(
            history(DeviceHistorySource.Local, "local-history"),
            history(DeviceHistorySource.Cloud, "cloud-history"),
        )

        viewModel.playDeviceHistory(items, startIndex = 1)

        assertThat(controller.playedIndex).isEqualTo(1)
        assertThat(controller.playedItems.map(PlaybackItem::queueKey))
            .containsExactly("local:local-history", "cloud:cloud-history")
            .inOrder()
        assertThat((controller.playedItems[0].origin as PlaybackOrigin.Local).id.value)
            .isEqualTo("local-history")
        assertThat((controller.playedItems[1].origin as PlaybackOrigin.Cloud).track.albumAudioId)
            .isEqualTo("audio-cloud-history")
    }

    private class FakePlaybackController : PlaybackController {
        override val state = MutableStateFlow(PlaybackState())
        var playedItems = emptyList<PlaybackItem>()
        var playedIndex = -1
        var appendedItems = emptyList<PlaybackItem>()
        var pauseCalls = 0

        override fun play(item: PlaybackItem) {
            playedItems = listOf(item)
            playedIndex = 0
        }

        override fun playAll(items: List<PlaybackItem>, startIndex: Int) {
            playedItems = items
            playedIndex = startIndex
        }

        override fun append(items: List<PlaybackItem>) {
            appendedItems = items
        }

        override fun selectQueueItem(index: Int) = Unit

        override fun removeQueueItem(index: Int) = Unit

        override fun moveQueueItem(fromIndex: Int, toIndex: Int) = Unit

        override fun togglePlayPause() = Unit

        override fun pause() {
            pauseCalls += 1
        }

        override fun next() = Unit

        override fun previous() = Unit

        override fun seekTo(positionMillis: Long) = Unit

        override fun setMode(mode: PlaybackMode) = Unit

        override fun clear() = Unit
    }

    private companion object {
        fun song(hash: String) = OnlineSong(
            hash = hash,
            title = hash,
            artist = "artist",
            coverUrl = null,
            albumId = null,
            albumAudioId = null,
            durationMillis = 180_000,
            quality = AudioQuality.Standard,
            vip = false,
        )

        fun cloud(hash: String) = CloudTrack(
            hash = hash,
            title = hash,
            artist = "artist",
            album = "album",
            coverUrl = null,
            durationMillis = 180_000,
            albumAudioId = "audio-$hash",
        )

        fun localMedia() = LocalMedia(
            id = LocalMediaId("local-id"),
            displayName = "signals.flac",
            title = "Signals",
            artist = "artist",
            albumTitle = "album",
            artworkUri = null,
            durationMillis = 180_000,
            mimeType = "audio/flac",
            fileExtension = "flac",
            sizeBytes = 4_096,
            sampleRateHz = 96_000,
            bitDepth = 24,
            bitrateBitsPerSecond = 2_304_000,
            importedAtEpochMillis = 1_000,
        )

        fun history(source: DeviceHistorySource, mediaId: String) = DeviceHistoryItem(
            record = DeviceHistoryRecord(
                source = source,
                mediaId = mediaId,
                title = mediaId,
                artist = "artist",
                albumTitle = "album",
                artworkUri = null,
                durationMillis = 180_000,
                albumAudioId = "audio-$mediaId",
            ),
            lastPlayedAtEpochMillis = 2_000,
            playCount = 2,
        )
    }
}
