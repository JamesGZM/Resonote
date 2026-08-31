package com.resonote.core.playback.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.OnlineSong
import org.junit.Test

class MusicDownloadMetadataCodecTest {
    @Test
    fun roundTripPreservesSongAndPlaybackSelection() {
        val metadata = MusicDownloadMetadata(
            song = OnlineSong(
                hash = "song-hash",
                title = "Signals",
                artist = "Resonote",
                coverUrl = "https://example.test/cover.jpg",
                albumId = "album-id",
                albumAudioId = "audio-id",
                durationMillis = 183_000,
                quality = AudioQuality.Lossless,
                vip = true,
                albumTitle = "Flight Mode",
                fileId = "file-id",
                previewDurationMillis = 30_000,
            ),
            quality = OnlinePlaybackQuality.HighResolution,
            extension = "flac",
        )

        assertThat(MusicDownloadMetadataCodec.decode(MusicDownloadMetadataCodec.encode(metadata)))
            .isEqualTo(metadata)
    }

    @Test
    fun malformedDataIsIgnored() {
        assertThat(MusicDownloadMetadataCodec.decode(byteArrayOf(1, 2, 3))).isNull()
    }
}
