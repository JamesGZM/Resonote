package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.model.NetworkSong
import org.junit.Test

class OnlineSongMappingTest {
    @Test
    fun remoteImageUrlNormalizesSizeAndHttps() {
        assertThat("http://imge.kugou.com/{size}/cover.jpg".toRemoteImageUrl(480))
            .isEqualTo("https://imge.kugou.com/480/cover.jpg")
        assertThat("//imge.kugou.com/{size}/cover.jpg".toRemoteImageUrl(240))
            .isEqualTo("https://imge.kugou.com/240/cover.jpg")
    }

    @Test
    fun remoteImageUrlRejectsBlankValues() {
        assertThat("  ".toRemoteImageUrl(480)).isNull()
        assertThat((null as String?).toRemoteImageUrl(480)).isNull()
    }

    @Test
    fun previewDurationIsPreservedForPlayback() {
        val song = NetworkSong(
            hash = "preview",
            title = "Preview",
            artist = null,
            coverUrl = null,
            albumId = null,
            albumAudioId = null,
            durationMillis = 180_000,
            highQualityHash = null,
            losslessHash = null,
            vip = true,
            previewDurationMillis = 60_000,
        ).toOnlineSong()

        assertThat(song.previewDurationMillis).isEqualTo(60_000)
    }
}
