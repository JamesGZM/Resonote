package com.resonote.core.playback.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaybackSeekPolicyTest {
    @Test
    fun stalePositionIsIgnoredWhileSeekAcknowledgementIsPending() {
        assertThat(
            shouldRetainPendingSeekPosition(
                targetPositionMillis = 90_000,
                reportedPositionMillis = 30_000,
                elapsedSinceRequestMillis = 500,
            ),
        ).isTrue()
    }

    @Test
    fun acknowledgedOrTimedOutSeekUsesPlayerPosition() {
        assertThat(
            shouldRetainPendingSeekPosition(
                targetPositionMillis = 90_000,
                reportedPositionMillis = 90_800,
                elapsedSinceRequestMillis = 500,
            ),
        ).isFalse()
        assertThat(
            shouldRetainPendingSeekPosition(
                targetPositionMillis = 90_000,
                reportedPositionMillis = 30_000,
                elapsedSinceRequestMillis = 2_500,
            ),
        ).isFalse()
    }

    @Test
    fun previousRestartsWhenLoadedMediaMatchesQueueCurrentItem() {
        assertThat(
            shouldRestartCurrentOnPrevious(
                loadedQueueKey = "online:current",
                currentQueueKey = "online:current",
                positionMillis = 5_001,
            ),
        ).isTrue()
    }

    @Test
    fun previousNavigatesQueueWhenAutomaticTransitionRetainsEndedMedia() {
        assertThat(
            shouldRestartCurrentOnPrevious(
                loadedQueueKey = "online:finished",
                currentQueueKey = "online:next",
                positionMillis = 180_000,
            ),
        ).isFalse()
    }
}
