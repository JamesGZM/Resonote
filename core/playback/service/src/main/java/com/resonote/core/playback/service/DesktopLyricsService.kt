@file:Suppress("DEPRECATION")

package com.resonote.core.playback.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.resonote.core.data.LyricsPreferencesRepository
import com.resonote.core.data.LyricsRepository
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.LyricsDocument
import com.resonote.core.model.LyricsPreferences
import com.resonote.core.model.PlaybackMode
import com.resonote.core.playback.DesktopLyricsNavigation
import com.resonote.core.playback.PlaybackController
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackOrigin
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DesktopLyricsService : Service() {
    @Inject lateinit var playbackController: PlaybackController

    @Inject lateinit var lyricsRepository: LyricsRepository

    @Inject lateinit var preferencesRepository: LyricsPreferencesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lyricsState = MutableStateFlow<DesktopLyricsState>(DesktopLyricsState.Waiting)
    private lateinit var window: DesktopLyricsWindow
    private var latestPreferences = LyricsPreferences()
    private var appliedPreferences: LyricsPreferences? = null
    private var hasRenderedLyricsContent = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceNotification()
        window = DesktopLyricsWindow(
            context = this,
            onPositionChanged = ::persistPosition,
            onTogglePlayPause = playbackController::togglePlayPause,
            onPrevious = playbackController::previous,
            onNext = playbackController::next,
            onCycleMode = ::cyclePlaybackMode,
            onLockedChanged = ::persistLock,
            onOpenSettings = ::openSettings,
            onClose = ::disableAndStop,
        )
        observeLyrics()
        observeRendering()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE -> disableAndStop()
            ACTION_TOGGLE_LOCK -> toggleLock()
            ACTION_RESET_POSITION -> persistPosition(window.resetPosition())
            ACTION_SHOW, ACTION_REFRESH, null -> Unit
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        window.clampToScreen()
    }

    override fun onDestroy() {
        window.hide()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeLyrics() {
        scope.launch {
            playbackController.state
                .map { it.currentItem?.lyricsTarget() }
                .distinctUntilChanged()
                .collectLatest { target ->
                    lyricsState.value = when (target) {
                        null -> DesktopLyricsState.Unavailable
                        else -> {
                            lyricsState.value = DesktopLyricsState.Loading
                            when (val result = lyricsRepository.loadLyrics(target.hash, target.albumAudioId)) {
                                is CollectionLoadResult.Available -> if (result.value.lines.isEmpty()) {
                                    DesktopLyricsState.Empty
                                } else {
                                    DesktopLyricsState.Content(
                                        LyricsDocument(result.value.lines.sortedBy { it.timeMillis }),
                                    )
                                }
                                is CollectionLoadResult.Failed -> DesktopLyricsState.Failed
                            }
                        }
                    }
                }
        }
    }

    private fun observeRendering() {
        scope.launch {
            combine(
                playbackController.state,
                lyricsState,
                preferencesRepository.preferences,
            ) { playback, lyrics, preferences ->
                DesktopLyricsRenderInput(playback, lyrics, preferences)
            }.collectLatest { (playback, lyrics, preferences) ->
                latestPreferences = preferences
                if (!preferences.desktopLyricsEnabled) {
                    stopSelf()
                    return@collectLatest
                }
                if (!Settings.canDrawOverlays(this@DesktopLyricsService)) {
                    preferencesRepository.setPreferences(preferences.copy(desktopLyricsEnabled = false))
                    stopSelf()
                    return@collectLatest
                }
                if (!desktopLyricsWindowShouldBeVisible(preferences.desktopLyricsEnabled, true)) {
                    window.hide()
                    return@collectLatest
                }
                if (!window.show(preferences)) {
                    preferencesRepository.setPreferences(preferences.copy(desktopLyricsEnabled = false))
                    stopSelf()
                    return@collectLatest
                }
                if (appliedPreferences != preferences) {
                    appliedPreferences = preferences
                    window.applyPreferences(preferences)
                    updateNotification(preferences.desktopLyricsLocked)
                }
                window.updatePlayback(playback.isPlaying, playback.mode)
                if (lyrics is DesktopLyricsState.Content && playback.isPlaying) {
                    val positionAnchor = playback.positionMillis
                    val timeAnchor = SystemClock.elapsedRealtime()
                    while (true) {
                        val elapsed = SystemClock.elapsedRealtime() - timeAnchor
                        val position = interpolatedDesktopLyricsPosition(
                            positionAnchorMillis = positionAnchor,
                            elapsedRealtimeMillis = elapsed,
                            durationMillis = playback.durationMillis,
                            visualLeadMillis = LYRICS_VISUAL_LEAD_MILLIS,
                        )
                        renderLyricsState(lyrics, position)
                        delay(LYRICS_FRAME_INTERVAL_MILLIS)
                    }
                }
                renderLyricsState(lyrics, playback.positionMillis, playback.currentItem == null)
            }
        }
    }

    private fun renderLyricsState(
        lyrics: DesktopLyricsState,
        positionMillis: Long,
        waitingForPlayback: Boolean = false,
    ) {
        when (lyrics) {
            DesktopLyricsState.Waiting -> window.renderMessage(
                getString(R.string.core_playback_service_desktop_lyrics_waiting),
            )
            DesktopLyricsState.Loading -> if (!hasRenderedLyricsContent) {
                window.renderMessage(getString(R.string.core_playback_service_desktop_lyrics_loading))
            }
            DesktopLyricsState.Empty -> window.renderMessage(
                getString(R.string.core_playback_service_desktop_lyrics_empty),
            )
            DesktopLyricsState.Unavailable -> window.renderMessage(
                if (waitingForPlayback) {
                    getString(R.string.core_playback_service_desktop_lyrics_waiting)
                } else {
                    getString(R.string.core_playback_service_desktop_lyrics_unavailable)
                },
            )
            DesktopLyricsState.Failed -> window.renderMessage(
                getString(R.string.core_playback_service_desktop_lyrics_failed),
            )
            is DesktopLyricsState.Content -> {
                val content = DesktopLyricsRenderer.render(
                    document = lyrics.document,
                    positionMillis = positionMillis,
                )
                if (content != null) {
                    hasRenderedLyricsContent = true
                    window.render(content)
                } else {
                    window.renderMessage(getString(R.string.core_playback_service_desktop_lyrics_empty))
                }
            }
        }
    }

    private fun persistPosition(position: com.resonote.core.model.DesktopLyricsPosition) {
        if (latestPreferences.desktopLyricsPosition == position) return
        val updated = latestPreferences.copy(desktopLyricsPosition = position)
        latestPreferences = updated
        scope.launch { preferencesRepository.setPreferences(updated) }
    }

    private fun toggleLock() {
        scope.launch {
            val current = preferencesRepository.preferences.first()
            persistLock(!current.desktopLyricsLocked)
        }
    }

    private fun persistLock(locked: Boolean) {
        val updated = latestPreferences.copy(desktopLyricsLocked = locked)
        latestPreferences = updated
        scope.launch { preferencesRepository.setPreferences(updated) }
    }

    private fun cyclePlaybackMode() {
        playbackController.setMode(playbackController.state.value.mode.nextDesktopLyricsMode())
    }

    private fun openSettings() {
        packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
            launchIntent.action = DesktopLyricsNavigation.ACTION_OPEN_SETTINGS
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(launchIntent)
        }
    }

    private fun disableAndStop() {
        scope.launch {
            val updated = preferencesRepository.preferences.first().copy(desktopLyricsEnabled = false)
            latestPreferences = updated
            preferencesRepository.setPreferences(updated)
            stopSelf()
        }
    }

    private fun startForegroundServiceNotification() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification(locked = false),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
    }

    private fun updateNotification(locked: Boolean) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(locked))
    }

    private fun notification(locked: Boolean): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(getString(R.string.core_playback_service_desktop_lyrics_notification_title))
            .setContentText(
                getString(
                    if (locked) {
                        R.string.core_playback_service_desktop_lyrics_notification_locked
                    } else {
                        R.string.core_playback_service_desktop_lyrics_notification_adjust
                    },
                ),
            )
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(
                0,
                getString(
                    if (locked) {
                        R.string.core_playback_service_desktop_lyrics_unlock
                    } else {
                        R.string.core_playback_service_desktop_lyrics_lock
                    },
                ),
                servicePendingIntent(ACTION_TOGGLE_LOCK, 1),
            )
            .addAction(
                0,
                getString(R.string.core_playback_service_desktop_lyrics_hide),
                servicePendingIntent(ACTION_HIDE, 2),
            )
            .build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int) = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, DesktopLyricsService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.core_playback_service_desktop_lyrics_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
        )
    }

    internal companion object {
        const val ACTION_SHOW = "com.resonote.desktoplyrics.SHOW"
        const val ACTION_HIDE = "com.resonote.desktoplyrics.HIDE"
        const val ACTION_REFRESH = "com.resonote.desktoplyrics.REFRESH"
        const val ACTION_TOGGLE_LOCK = "com.resonote.desktoplyrics.TOGGLE_LOCK"
        const val ACTION_RESET_POSITION = "com.resonote.desktoplyrics.RESET_POSITION"
        private const val NOTIFICATION_CHANNEL_ID = "desktop_lyrics"
        private const val NOTIFICATION_ID = 4702
        private const val LYRICS_FRAME_INTERVAL_MILLIS = 60L
        private const val LYRICS_VISUAL_LEAD_MILLIS = 260L
    }
}

private data class DesktopLyricsRenderInput(
    val playback: com.resonote.core.playback.PlaybackState,
    val lyrics: DesktopLyricsState,
    val preferences: LyricsPreferences,
)

internal fun desktopLyricsWindowShouldBeVisible(enabled: Boolean, overlayPermissionGranted: Boolean): Boolean =
    enabled && overlayPermissionGranted

internal fun interpolatedDesktopLyricsPosition(
    positionAnchorMillis: Long,
    elapsedRealtimeMillis: Long,
    durationMillis: Long,
    visualLeadMillis: Long = 0,
): Long = (positionAnchorMillis + elapsedRealtimeMillis.coerceAtLeast(0) + visualLeadMillis.coerceAtLeast(0))
    .coerceAtMost(
        durationMillis.takeIf { it > 0 } ?: Long.MAX_VALUE,
    )

internal fun PlaybackMode.nextDesktopLyricsMode(): PlaybackMode = when (this) {
    PlaybackMode.ListLoop -> PlaybackMode.Shuffle
    PlaybackMode.Shuffle -> PlaybackMode.SingleLoop
    PlaybackMode.SingleLoop -> PlaybackMode.Sequential
    PlaybackMode.Sequential -> PlaybackMode.ListLoop
}

private sealed interface DesktopLyricsState {
    data object Waiting : DesktopLyricsState

    data object Loading : DesktopLyricsState

    data object Empty : DesktopLyricsState

    data object Unavailable : DesktopLyricsState

    data object Failed : DesktopLyricsState

    data class Content(val document: LyricsDocument) : DesktopLyricsState
}

private data class DesktopLyricsTarget(val hash: String, val albumAudioId: String?)

private fun PlaybackItem.lyricsTarget(): DesktopLyricsTarget? = when (val origin = origin) {
    is PlaybackOrigin.Online -> DesktopLyricsTarget(origin.song.hash, origin.song.albumAudioId)
    is PlaybackOrigin.Cloud -> DesktopLyricsTarget(origin.track.hash, origin.track.albumAudioId)
    is PlaybackOrigin.Local -> null
}
