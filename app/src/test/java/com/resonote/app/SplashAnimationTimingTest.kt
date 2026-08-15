package com.resonote.app

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SplashAnimationTimingTest {
    @Test
    fun animationJustStartedWaitsForItsFullDuration() {
        val remainingDurationMillis = remainingSplashAnimationDurationMillis(
            animationStartMillis = 1_000L,
            animationDurationMillis = 750L,
            currentTimeMillis = 1_000L,
            animationsEnabled = true,
        )

        assertThat(remainingDurationMillis).isEqualTo(750L)
    }

    @Test
    fun animationInProgressWaitsOnlyForRemainingDuration() {
        val remainingDurationMillis = remainingSplashAnimationDurationMillis(
            animationStartMillis = 1_000L,
            animationDurationMillis = 750L,
            currentTimeMillis = 1_500L,
            animationsEnabled = true,
        )

        assertThat(remainingDurationMillis).isEqualTo(250L)
    }

    @Test
    fun completedAnimationDoesNotDelaySplashExit() {
        val remainingDurationMillis = remainingSplashAnimationDurationMillis(
            animationStartMillis = 1_000L,
            animationDurationMillis = 750L,
            currentTimeMillis = 2_000L,
            animationsEnabled = true,
        )

        assertThat(remainingDurationMillis).isEqualTo(0L)
    }

    @Test
    fun disabledAnimationsDoNotDelaySplashExit() {
        val remainingDurationMillis = remainingSplashAnimationDurationMillis(
            animationStartMillis = 1_000L,
            animationDurationMillis = 750L,
            currentTimeMillis = 1_000L,
            animationsEnabled = false,
        )

        assertThat(remainingDurationMillis).isEqualTo(0L)
    }

    @Test
    fun missingPlatformAnimationTimingDoesNotDelaySplashExit() {
        val remainingDurationMillis = remainingSplashAnimationDurationMillis(
            animationStartMillis = 0L,
            animationDurationMillis = 0L,
            currentTimeMillis = 1_000L,
            animationsEnabled = true,
        )

        assertThat(remainingDurationMillis).isEqualTo(0L)
    }
}
