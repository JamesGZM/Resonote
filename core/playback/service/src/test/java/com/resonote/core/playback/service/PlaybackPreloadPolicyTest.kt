package com.resonote.core.playback.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaId
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackMode
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackStatus
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaybackPreloadPolicyTest {
    @Test
    fun startsOnceWhenPlayingEntersThirtySecondWindow() {
        assertThat(
            shouldStartPlaybackPreload(PlaybackStatus.Playing, 90_000, 120_000, alreadyAttempted = false),
        ).isTrue()
        assertThat(
            shouldStartPlaybackPreload(PlaybackStatus.Playing, 89_999, 120_000, alreadyAttempted = false),
        ).isFalse()
        assertThat(
            shouldStartPlaybackPreload(PlaybackStatus.Paused, 90_000, 120_000, alreadyAttempted = false),
        ).isFalse()
        assertThat(
            shouldStartPlaybackPreload(PlaybackStatus.Playing, 90_000, 120_000, alreadyAttempted = true),
        ).isFalse()
    }

    @Test
    fun deterministicModesChooseNextWithoutMutatingQueue() {
        val queue = PlaybackQueue().apply { replace(listOf(item("first"), item("second")), 1) }

        assertThat(playbackPreloadCandidate(queue, PlaybackMode.Sequential)).isNull()
        assertThat(playbackPreloadCandidate(queue, PlaybackMode.ListLoop)?.queueKey).isEqualTo("online:first")
        assertThat(playbackPreloadCandidate(queue, PlaybackMode.Shuffle)).isNull()
        assertThat(playbackPreloadCandidate(queue, PlaybackMode.SingleLoop)).isNull()
        assertThat(queue.currentItem?.queueKey).isEqualTo("online:second")
    }

    @Test
    fun localNextItemDoesNotEnterStreamingCache() {
        val queue = PlaybackQueue().apply {
            replace(listOf(item("online"), PlaybackItem(localMedia("local"))), 0)
        }

        assertThat(playbackPreloadCandidate(queue, PlaybackMode.Sequential)).isNull()
    }

    @Test
    fun preloadRequestUsesStableKeyAndFourMegabyteLimit() {
        val dataSpec = buildPlaybackPreloadDataSpec("https://media.example/song.mp3", "online:song:Standard:full")

        assertThat(dataSpec.key).isEqualTo("online:song:Standard:full")
        assertThat(dataSpec.position).isEqualTo(0)
        assertThat(dataSpec.length).isEqualTo(PLAYBACK_PRELOAD_BYTES)
    }

    @Test
    fun prefetchedSourceExpiresAfterSixtySeconds() {
        assertThat(isPrefetchedSourceFresh(1_000, 61_000)).isTrue()
        assertThat(isPrefetchedSourceFresh(1_000, 61_001)).isFalse()
        assertThat(isPrefetchedSourceFresh(2_000, 1_000)).isFalse()
    }

    private fun item(hash: String) = PlaybackItem(
        OnlineSong(
            hash = hash,
            title = hash,
            artist = "artist",
            coverUrl = null,
            albumId = null,
            albumAudioId = null,
            durationMillis = 120_000,
            quality = AudioQuality.Standard,
            vip = false,
        ),
    )

    private fun localMedia(id: String) = LocalMedia(
        id = LocalMediaId(id),
        displayName = "$id.flac",
        title = id,
        artist = "artist",
        albumTitle = null,
        artworkUri = null,
        durationMillis = 120_000,
        mimeType = "audio/flac",
        fileExtension = "flac",
        sizeBytes = 4_096,
        sampleRateHz = 96_000,
        bitDepth = 24,
        bitrateBitsPerSecond = 2_304_000,
        importedAtEpochMillis = 1_000,
    )
}
