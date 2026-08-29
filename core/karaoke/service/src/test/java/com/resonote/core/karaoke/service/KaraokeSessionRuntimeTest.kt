package com.resonote.core.karaoke.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.KaraokeRecordingCommitResult
import com.resonote.core.karaoke.KaraokeSessionFailure
import com.resonote.core.karaoke.KaraokeSessionState
import com.resonote.core.karaoke.KaraokeSessionStatus
import com.resonote.core.model.KaraokeProjectId
import com.resonote.core.model.KaraokeSourceMode
import com.resonote.core.playback.PlaybackStatus
import org.junit.Test

class KaraokeSessionRuntimeTest {
    @Test
    fun countdownCompletionStartsPlaybackThatWasPausedForCountdown() {
        assertThat(PlaybackStatus.Paused.shouldStartAfterKaraokeCountdown()).isTrue()
        assertThat(PlaybackStatus.Playing.shouldStartAfterKaraokeCountdown()).isFalse()
    }

    @Test
    fun sourceSwitchCompletesInOffStateAndRequiresManualRestart() {
        val result = recordingState().completeSourceSwitch(
            target = KaraokeSourceMode.Original,
            persisted = true,
            captureCommit = KaraokeRecordingCommitResult.Discarded,
        )

        assertThat(result.status).isEqualTo(KaraokeSessionStatus.Off)
        assertThat(result.continuousRecordingArmed).isFalse()
        assertThat(result.selectedSourceMode).isEqualTo(KaraokeSourceMode.Original)
        assertThat(result.failure).isNull()
    }

    @Test
    fun failedSourceSwitchReturnsToOperableNonRecordingState() {
        val result = recordingState().completeSourceSwitch(
            target = KaraokeSourceMode.Original,
            persisted = false,
            captureCommit = KaraokeRecordingCommitResult.Saved,
        )

        assertThat(result.status).isEqualTo(KaraokeSessionStatus.Off)
        assertThat(result.continuousRecordingArmed).isFalse()
        assertThat(result.selectedSourceMode).isEqualTo(KaraokeSourceMode.Accompaniment)
        assertThat(result.sourceChangeInProgress).isFalse()
        assertThat(result.failure).isEqualTo(KaraokeSessionFailure.SourceUnavailable)
    }

    @Test
    fun stopCompletionDisarmsContinuousRecordingAndRequiresManualRestart() {
        val result = recordingState().copy(savingInProgress = true).completeKaraokeStop()

        assertThat(result.status).isEqualTo(KaraokeSessionStatus.Off)
        assertThat(result.continuousRecordingArmed).isFalse()
        assertThat(result.savingInProgress).isFalse()
    }

    private fun recordingState() = KaraokeSessionState(
        enabled = true,
        continuousRecordingArmed = true,
        status = KaraokeSessionStatus.Recording(
            projectId = KaraokeProjectId("project"),
            elapsedMillis = 1_000,
            hasOfficialAccompaniment = true,
        ),
        availableSourceModes = setOf(KaraokeSourceMode.Accompaniment, KaraokeSourceMode.Original),
        selectedSourceMode = KaraokeSourceMode.Accompaniment,
        sourceChangeInProgress = true,
    )
}
