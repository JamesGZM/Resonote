package com.resonote.core.playback.service

import android.app.ForegroundServiceStartNotAllowedException
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ForegroundSafeMediaNotificationProviderTest {
    @Test
    fun foregroundServiceStartNotAllowedRunsFallback() {
        var fallbackInvoked = false

        runForegroundServiceStartGuarded(
            onForegroundServiceStartNotAllowed = { fallbackInvoked = true },
            updateNotification = {
                throw ForegroundServiceStartNotAllowedException("background start rejected")
            },
        )

        assertThat(fallbackInvoked).isTrue()
    }

    @Test
    fun unrelatedRuntimeExceptionIsRethrown() {
        val failure = IllegalStateException("unexpected notification failure")

        val thrown = runCatching {
            runForegroundServiceStartGuarded(
                onForegroundServiceStartNotAllowed = {},
                updateNotification = { throw failure },
            )
        }.exceptionOrNull()

        assertThat(thrown).isSameInstanceAs(failure)
    }
}
