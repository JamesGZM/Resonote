package com.resonote.core.playback.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.ContentFailure
import com.resonote.core.playback.PlaybackIssue
import com.resonote.core.playback.PlaybackState
import com.resonote.core.playback.PlaybackStatus
import org.junit.Test

class PlaybackFailureRecoveryTest {
    @Test
    fun allowsFiveConsecutiveAutomaticSkipsThenStops() {
        val recovery = PlaybackFailureRecovery(maxConsecutiveSkips = 5)

        assertThat(List(5) { recovery.onFailure() }).containsExactly(true, true, true, true, true).inOrder()
        assertThat(recovery.onFailure()).isFalse()
    }

    @Test
    fun successfulPlaybackStartsANewFailureWindow() {
        val recovery = PlaybackFailureRecovery(maxConsecutiveSkips = 2)
        assertThat(recovery.onFailure()).isTrue()
        assertThat(recovery.onFailure()).isTrue()

        recovery.onPlaybackStarted()

        assertThat(recovery.onFailure()).isTrue()
        assertThat(recovery.onFailure()).isTrue()
        assertThat(recovery.onFailure()).isFalse()
    }

    @Test
    fun interactiveRecoveryFailuresDoNotAutomaticallySkip() {
        assertThat(PlaybackIssue.SourceFailure(ContentFailure.AuthenticationRequired).allowsAutomaticSkip()).isFalse()
        assertThat(PlaybackIssue.SourceFailure(ContentFailure.RiskBlocked).allowsAutomaticSkip()).isFalse()
        assertThat(PlaybackIssue.SourceFailure(ContentFailure.Network).allowsAutomaticSkip()).isTrue()
    }

    @Test
    fun rejectedSingleKeepsCurrentPlaybackState() {
        val playing = PlaybackState(
            status = PlaybackStatus.Playing,
            positionMillis = 12_000L,
            durationMillis = 180_000L,
            bufferedPositionMillis = 30_000L,
        )
        val issue = PlaybackIssue.SourceFailure(ContentFailure.ServiceRejected)

        val failedAttempt = playing.withNonInterruptingIssue(issue)

        assertThat(failedAttempt).isEqualTo(playing.copy(issue = issue))
        assertThat(failedAttempt.status).isEqualTo(PlaybackStatus.Playing)
    }
}
