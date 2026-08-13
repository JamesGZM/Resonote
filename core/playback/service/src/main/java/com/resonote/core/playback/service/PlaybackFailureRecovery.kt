package com.resonote.core.playback.service

import com.resonote.core.model.ContentFailure
import com.resonote.core.playback.PlaybackIssue
import com.resonote.core.playback.PlaybackState

internal class PlaybackFailureRecovery(
    private val maxConsecutiveSkips: Int,
) {
    init {
        require(maxConsecutiveSkips > 0) { "maxConsecutiveSkips must be positive" }
    }

    private var consecutiveFailures = 0

    fun onFailure(): Boolean {
        if (consecutiveFailures >= maxConsecutiveSkips) {
            consecutiveFailures = 0
            return false
        }
        consecutiveFailures += 1
        return true
    }

    fun onPlaybackStarted() {
        consecutiveFailures = 0
    }

    fun reset() {
        consecutiveFailures = 0
    }
}

internal fun PlaybackIssue.allowsAutomaticSkip(): Boolean = when (this) {
    is PlaybackIssue.Unavailable,
    is PlaybackIssue.PlayerFailure,
    -> true
    is PlaybackIssue.SourceFailure -> when (failure) {
        ContentFailure.AuthenticationRequired,
        is ContentFailure.RiskVerificationRequired,
        ContentFailure.RiskBlocked,
        -> false
        ContentFailure.Network,
        ContentFailure.Protocol,
        ContentFailure.ServiceRejected,
        -> true
    }
}

internal fun PlaybackState.withNonInterruptingIssue(issue: PlaybackIssue): PlaybackState =
    copy(issue = issue)
