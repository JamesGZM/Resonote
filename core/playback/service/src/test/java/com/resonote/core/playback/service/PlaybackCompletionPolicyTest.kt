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
                positionMillis = 179_700,
                sourceDurationMillis = 180_000,
                previewDurationMillis = null,
            ),
        ).isNull()
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
        sourceDurationMillis: Long = 180_000,
        previewDurationMillis: Long? = 60_000,
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
