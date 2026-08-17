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
    fun vipPreviewDoesNotCompleteBeforeDeclaredDuration() {
        assertThat(action(mode = PlaybackMode.ListLoop, queueSize = 3, positionMillis = 40_000)).isNull()
    }

    @Test
    fun authenticatedFullLengthVipSourceIsNotTreatedAsPreview() {
        assertThat(
            action(
                mode = PlaybackMode.ListLoop,
                queueSize = 3,
                positionMillis = 59_700,
                sourceDurationMillis = 180_000,
                previewDurationMillis = 60_000,
                isPreviewSource = false,
            ),
        ).isNull()
    }

    @Test
    fun anonymousPreviewUsesDeclaredBoundaryEvenWhenApiReportsFullSongDuration() {
        assertThat(
            action(
                mode = PlaybackMode.ListLoop,
                queueSize = 3,
                positionMillis = 59_700,
                sourceDurationMillis = 180_000,
                previewDurationMillis = 60_000,
                isPreviewSource = true,
            ),
        ).isEqualTo(PlaybackCompletionAction.Advance)
    }

    @Test
    fun unresolvedVipSongCanExposeDeclaredPreviewDuration() {
        val item = PlaybackItem(song(isVip = true, previewDurationMillis = 60_000))

        assertThat(item.vipPreviewDurationMillisOrNull()).isEqualTo(60_000)
    }

    @Test
    fun authenticatedSourceWithUnknownDurationIgnoresDeclaredPreviewMetadata() {
        assertThat(
            action(
                mode = PlaybackMode.ListLoop,
                queueSize = 3,
                positionMillis = 59_700,
                sourceDurationMillis = 0,
                previewDurationMillis = 60_000,
                isPreviewSource = false,
            ),
        ).isNull()
    }

    @Test
    fun onlineVipPreviewRefreshesAfterEntitlementChange() {
        val restored = PlaybackItem(song(isVip = true, previewDurationMillis = 60_000))
        val preview = PlaybackItem(song(isVip = true, previewDurationMillis = 60_000)).withResolvedSource(
            ResolvedSongSource("https://media.example/preview.mp3", 180_000, "mp3", isPreview = true),
        )
        val full = PlaybackItem(song(isVip = true, previewDurationMillis = 60_000)).withResolvedSource(
            ResolvedSongSource("https://media.example/full.mp3", 180_000, "mp3"),
        )

        assertThat(restored.shouldRefreshOnlineSource(force = false)).isTrue()
        assertThat(preview.shouldRefreshOnlineSource(force = false)).isTrue()
        assertThat(full.shouldRefreshOnlineSource(force = false)).isFalse()
        assertThat(full.shouldRefreshOnlineSource(force = true)).isTrue()
    }

    @Test
    fun restoredPositionUsesResolvedEntitlementBoundary() {
        val preview = PlaybackItem(song(isVip = true, previewDurationMillis = 60_000)).withResolvedSource(
            ResolvedSongSource("https://media.example/preview.mp3", 180_000, "mp3", isPreview = true),
        )
        val full = PlaybackItem(song(isVip = true, previewDurationMillis = 60_000)).withResolvedSource(
            ResolvedSongSource("https://media.example/full.mp3", 180_000, "mp3"),
        )

        assertThat(preview.coercePlaybackPosition(157_000, 180_000)).isEqualTo(60_000)
        assertThat(full.coercePlaybackPosition(157_000, 180_000)).isEqualTo(157_000)
    }

    @Test
    fun unresolvedRestoredItemPrefersSnapshotOverStalePlayerPosition() {
        val restored = PlaybackItem(song(isVip = true, previewDurationMillis = 60_000))
        val playingPreview = restored.withResolvedSource(
            ResolvedSongSource("https://media.example/preview.mp3", 180_000, "mp3", isPreview = true),
        )

        assertThat(
            restored.sourceRefreshPositionMillis(
                loadedPlayerPositionMillis = 0,
                restoredPositionMillis = 124_234,
            ),
        ).isEqualTo(124_234)
        assertThat(
            playingPreview.sourceRefreshPositionMillis(
                loadedPlayerPositionMillis = 37_000,
                restoredPositionMillis = 124_234,
            ),
        ).isEqualTo(37_000)
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

    @Test
    fun automaticTransitionRetainsLoadedMediaWhileResolving() {
        assertThat(
            shouldRetainLoadedMediaWhileResolvingNext(
                automatic = true,
                loadedMediaItemCount = 1,
            ),
        ).isTrue()
    }

    @Test
    fun manualTransitionDoesNotRetainLoadedMediaWhileResolving() {
        assertThat(
            shouldRetainLoadedMediaWhileResolvingNext(
                automatic = false,
                loadedMediaItemCount = 1,
            ),
        ).isFalse()
    }

    @Test
    fun automaticTransitionWithoutLoadedMediaDoesNotRetainAnything() {
        assertThat(
            shouldRetainLoadedMediaWhileResolvingNext(
                automatic = true,
                loadedMediaItemCount = 0,
            ),
        ).isFalse()
    }

    private fun action(
        mode: PlaybackMode,
        queueSize: Int,
        positionMillis: Long,
        isVip: Boolean = true,
        sourceDurationMillis: Long = 60_000,
        previewDurationMillis: Long? = 45_000,
        isPreviewSource: Boolean = true,
    ): PlaybackCompletionAction? = vipPreviewCompletionAction(
        item = PlaybackItem(song(isVip, previewDurationMillis)).withResolvedSource(
            ResolvedSongSource(
                "https://media.example/preview.mp3",
                sourceDurationMillis,
                "mp3",
                isPreview = isPreviewSource,
            ),
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
