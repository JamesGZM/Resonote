package com.resonote.core.karaoke.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.resonote.core.karaoke.KaraokeSessionStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class KaraokeRecordingService : Service() {
    @Inject internal lateinit var runtime: KaraokeSessionRuntime

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var notificationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification(KaraokeSessionStatus.Preparing))
        notificationJob = scope.launch {
            runtime.state.collect { state ->
                notificationManager.notify(NOTIFICATION_ID, notification(state.status))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> runtime.pause()
            ACTION_RESUME -> runtime.resume()
            ACTION_PREVIOUS -> runtime.previous()
            ACTION_NEXT -> runtime.next()
            ACTION_STOP -> {
                runtime.stopAndSave()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        notificationJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(status: KaraokeSessionStatus): Notification {
        val isPaused = status is KaraokeSessionStatus.Paused
        val content = when (status) {
            KaraokeSessionStatus.Off -> getString(R.string.core_karaoke_service_preparing)
            KaraokeSessionStatus.Preparing -> getString(R.string.core_karaoke_service_preparing)
            is KaraokeSessionStatus.Countdown -> getString(
                R.string.core_karaoke_service_countdown,
                status.secondsRemaining,
            )
            is KaraokeSessionStatus.Recording -> getString(
                R.string.core_karaoke_service_recording,
                formatDuration(status.elapsedMillis),
            )
            is KaraokeSessionStatus.Paused -> getString(R.string.core_karaoke_service_paused)
            is KaraokeSessionStatus.Failed -> getString(R.string.core_karaoke_service_failed)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.core_karaoke_service_title))
            .setContentText(content)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                android.R.drawable.ic_media_previous,
                getString(R.string.core_karaoke_service_previous),
                actionIntent(ACTION_PREVIOUS, REQUEST_PREVIOUS),
            )
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                getString(if (isPaused) R.string.core_karaoke_service_resume else R.string.core_karaoke_service_pause),
                actionIntent(if (isPaused) ACTION_RESUME else ACTION_PAUSE, REQUEST_PLAY_PAUSE),
            )
            .addAction(
                android.R.drawable.ic_media_next,
                getString(R.string.core_karaoke_service_next),
                actionIntent(ACTION_NEXT, REQUEST_NEXT),
            )
            .addAction(
                android.R.drawable.ic_menu_save,
                getString(R.string.core_karaoke_service_stop),
                actionIntent(ACTION_STOP, REQUEST_STOP),
            )
            .build()
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, KaraokeRecordingService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.core_karaoke_service_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private val notificationManager: NotificationManager
        get() = getSystemService(NotificationManager::class.java)

    private fun formatDuration(millis: Long): String {
        val seconds = millis.coerceAtLeast(0L) / 1_000L
        return "%d:%02d".format(seconds / 60L, seconds % 60L)
    }

    companion object {
        const val ACTION_START = "com.resonote.karaoke.START"
        private const val ACTION_PAUSE = "com.resonote.karaoke.PAUSE"
        private const val ACTION_RESUME = "com.resonote.karaoke.RESUME"
        private const val ACTION_PREVIOUS = "com.resonote.karaoke.PREVIOUS"
        private const val ACTION_NEXT = "com.resonote.karaoke.NEXT"
        private const val ACTION_STOP = "com.resonote.karaoke.STOP"
        private const val CHANNEL_ID = "karaoke_recording"
        private const val NOTIFICATION_ID = 4_201
        private const val REQUEST_PREVIOUS = 1
        private const val REQUEST_PLAY_PAUSE = 2
        private const val REQUEST_NEXT = 3
        private const val REQUEST_STOP = 4
    }
}
