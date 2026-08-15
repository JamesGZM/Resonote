package com.resonote.core.playback.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackMode
import org.junit.Test

class PlaybackCompletionPolicyTest {
    @Test
    fun vipPreviewInQueueAdvancesAtSourceDuration() {
        assertThat(action(mode = PlaybackMode.ListLoop, queueSize = 3, positionMillis = 59_700))
            .isEqualTo(PlaybackCompletionAction.Advance)
    }

    @Test
    fun vipPreviewAsOnlyQueueItemPauses() {
        assertThat(action(mode = PlaybackMode.ListLoop, queueSize = 1, positionMillis = 59_700))
            .isEqualTo(PlaybackCompletionAction.Pause)
    }

    @Test
    fun vipPreviewInSingleLoopPauses() {
        assertThat(action(mode = PlaybackMode.SingleLoop, queueSize = 3, positionMillis = 59_700))
            .isEqualTo(PlaybackCompletionAction.Pause)
    }

    @Test
    fun vipPreviewDoesNotCompleteBeforeSourceDuration() {
        assertThat(action(mode = PlaybackMode.ListLoop, queueSize = 3, positionMillis = 50_000)).isNull()
    }

    @Test
    fun fullLengthVipSourceIsNotTreatedAsPreview() {
        assertThat(
            action(
                mode = PlaybackMode.ListLoop,
                queueSize = 3,
                positionMillis = 59_700,
                sourceDurationMillis = 180_000,
                previewDurationMillis = 60_000,
            ),
        ).isNull()
    }

    @Test
    fun unresolvedVipSongCanExposeDeclaredPreviewDuration() {
        val item = PlaybackItem(song(isVip = true, previewDurationMillis = 60_000))

        assertThat(item.vipPreviewDurationMillisOrNull()).isEqualTo(60_000)
    }

    @Test
    fun resolvedSourceWithUnknownDurationIgnoresDeclaredPreviewMetadata() {
        assertThat(
            action(
                mode = PlaybackMode.ListLoop,
                queueSize = 3,
                positionMillis = 59_700,
                sourceDurationMillis = 0,
                previewDurationMillis = 60_000,
            ),
        ).isNull()
    }

    @Test
    fun onlineVipPreviewRefreshesAfterEntitlementChange() {
        val preview = PlaybackItem(song(isVip = true, previewDurationMillis = 60_000)).withResolvedSource(
            ResolvedSongSource("https://media.example/preview.mp3", 60_000, "mp3"),
        )
        val full = PlaybackItem(song(isVip = true, previewDurationMillis = 60_000)).withResolvedSource(
            ResolvedSongSource("https://media.example/full.mp3", 180_000, "mp3"),
        )

        assertThat(preview.shouldRefreshOnlineSource(force = false)).isTrue()
        assertThat(full.shouldRefreshOnlineSource(force = false)).isFalse()
        assertThat(full.shouldRefreshOnlineSource(force = true)).isTrue()
    }

    @Test
    fun nonVipSourceIsNotTreatedAsPreview() {
        assertThat(action(mode = PlaybackMode.ListLoop, queueSize = 3, positionMillis = 59_700, isVip = false)).isNull()
    }

    @Test
    fun normalPlaybackEndKeepsSingleLoopBehavior() {
        assertThat(playbackEndedCompletionAction(PlaybackMode.SingleLoop))
            .isEqualTo(PlaybackCompletionAction.Replay)
        assertThat(playbackEndedCompletionAction(PlaybackMode.ListLoop))
            .isEqualTo(PlaybackCompletionAction.Advance)
    }

    private fun action(
        mode: PlaybackMode,
        queueSize: Int,
        positionMillis: Long,
        isVip: Boolean = true,
        sourceDurationMillis: Long = 60_000,
        previewDurationMillis: Long? = 45_000,
    ): PlaybackCompletionAction? = vipPreviewCompletionAction(
        item = PlaybackItem(song(isVip, previewDurationMillis)).withResolvedSource(
            ResolvedSongSource("https://media.example/preview.mp3", sourceDurationMillis, "mp3"),
        ),
        mode = mode,
        queueSize = queueSize,
        positionMillis = positionMillis,
    )

    private fun song(isVip: Boolean, previewDurationMillis: Long?) = OnlineSong(
        hash = "preview",
        title = "Preview",
        artist = "Artist",
        albumTitle = "Album",
        albumId = "1",
        albumAudioId = "2",
        coverUrl = null,
        durationMillis = 180_000,
        quality = AudioQuality.Standard,
        vip = isVip,
        previewDurationMillis = previewDurationMillis,
    )
}
