package com.resonote.core.playback.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.PlaybackSessionEntryKind
import com.resonote.core.data.PlaybackSessionSnapshot
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.LocalMediaId
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.PlaybackFormat
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackMetadata
import com.resonote.core.playback.PlaybackOrigin
import com.resonote.core.playback.PlaybackStatus
import org.junit.Test

class PlaybackSessionMappingTest {
    @Test
    fun restoredSnapshotIsPausedAndPreservesQueueIndexModeAndProgress() {
        val entries = listOf(
            PlaybackItem(onlineSong("first")).toSessionEntry(),
            PlaybackItem(onlineSong("second")).toSessionEntry(),
        )

        val state = PlaybackSessionSnapshot(entries, 1, 42_000, "Shuffle")
            .toPlaybackState(PlaybackSpeed.OneAndQuarter)

        assertThat(state?.status).isEqualTo(PlaybackStatus.Paused)
        assertThat(state?.queue?.map { it.metadata.mediaId }).containsExactly("first", "second").inOrder()
        assertThat(state?.currentIndex).isEqualTo(1)
        assertThat(state?.positionMillis).isEqualTo(42_000)
        assertThat(state?.mode?.name).isEqualTo("Shuffle")
        assertThat(state?.playbackSpeed).isEqualTo(PlaybackSpeed.OneAndQuarter)
    }

    @Test
    fun restoredVipProgressIsNotClampedToPreviewBeforeSourceResolution() {
        val vipItem = PlaybackItem(
            onlineSong("vip").copy(
                vip = true,
                previewDurationMillis = 60_000,
            ),
        )

        val state = PlaybackSessionSnapshot(
            entries = listOf(vipItem.toSessionEntry()),
            currentIndex = 0,
            positionMillis = 157_000,
            mode = "ListLoop",
        ).toPlaybackState(PlaybackSpeed.Normal)

        assertThat(state?.positionMillis).isEqualTo(157_000)
        assertThat(state?.durationMillis).isEqualTo(180_000)
    }

    @Test
    fun mappingsPreserveQueueMetadataAndOriginsWithoutResolvedSource() {
        val online = PlaybackItem(
            OnlineSong(
                hash = "online",
                title = "Online",
                artist = "Artist",
                coverUrl = "cover",
                albumId = "album",
                albumAudioId = "audio",
                durationMillis = 180_000,
                quality = AudioQuality.Lossless,
                vip = true,
                albumTitle = "Album",
                fileId = "file",
                previewDurationMillis = 60_000,
            ),
            ResolvedSongSource("https://temporary", 180_000, "mp3"),
        )
        val cloud = PlaybackItem(CloudTrack("cloud", "Cloud", "Artist", "Album", null, 90_000, "audio"))
        val local = PlaybackItem(
            metadata = PlaybackMetadata(
                mediaId = "local",
                title = "Local",
                artist = null,
                albumTitle = null,
                artworkUri = null,
                durationMillis = 80_000,
                format = PlaybackFormat.Local("audio/flac", "flac", 96_000, 24, 2_400_000),
                isVip = false,
            ),
            origin = PlaybackOrigin.Local(LocalMediaId("local")),
        )

        val restored = listOf(online, cloud, local).map { it.toSessionEntry().toPlaybackItem() }

        assertThat(restored.map { it?.queueKey }).containsExactly("online:online", "cloud:cloud", "local:local")
            .inOrder()
        assertThat(restored.map { it?.resolvedSource }).containsExactly(null, null, null)
        assertThat(online.toSessionEntry().kind).isEqualTo(PlaybackSessionEntryKind.Online)
        assertThat(restored.first()?.metadata).isEqualTo(online.metadata)
        assertThat(restored.last()?.metadata).isEqualTo(local.metadata)
    }

    private fun onlineSong(id: String) = OnlineSong(
        hash = id,
        title = "Title $id",
        artist = "Artist",
        coverUrl = null,
        albumId = "album",
        albumAudioId = "audio",
        durationMillis = 180_000,
        quality = AudioQuality.Standard,
        vip = false,
    )
}
