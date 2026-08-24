@file:androidx.media3.common.util.UnstableApi

package com.resonote.core.playback.service

import android.app.ForegroundServiceStartNotAllowedException
import android.os.Build
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList

internal class ForegroundSafeMediaNotificationProvider(
    private val delegate: MediaNotification.Provider,
    private val onForegroundServiceStartNotAllowed: () -> Unit,
) : MediaNotification.Provider {
    override fun createNotification(
        mediaSession: MediaSession,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback,
    ): MediaNotification = delegate.createNotification(
        mediaSession,
        mediaButtonPreferences,
        actionFactory,
    ) { notification ->
        runForegroundServiceStartGuarded(onForegroundServiceStartNotAllowed) {
            onNotificationChangedCallback.onNotificationChanged(notification)
        }
    }

    override fun handleCustomCommand(session: MediaSession, action: String, extras: android.os.Bundle): Boolean =
        delegate.handleCustomCommand(session, action, extras)

    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo =
        delegate.notificationChannelInfo
}

internal inline fun runForegroundServiceStartGuarded(
    onForegroundServiceStartNotAllowed: () -> Unit,
    updateNotification: () -> Unit,
) {
    try {
        updateNotification()
    } catch (failure: RuntimeException) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            failure is ForegroundServiceStartNotAllowedException
        ) {
            onForegroundServiceStartNotAllowed()
        } else {
            throw failure
        }
    }
}
