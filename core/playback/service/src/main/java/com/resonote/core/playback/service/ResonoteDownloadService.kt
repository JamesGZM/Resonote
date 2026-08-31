@file:androidx.media3.common.util.UnstableApi

package com.resonote.core.playback.service

import android.app.Notification
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ResonoteDownloadService :
    DownloadService(
        DOWNLOAD_NOTIFICATION_ID,
        DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
        DOWNLOAD_NOTIFICATION_CHANNEL_ID,
        R.string.core_playback_service_download_channel,
        R.string.core_playback_service_download_channel_description,
    ) {
    @Inject
    internal lateinit var store: PlaybackDownloadStore

    private val notificationHelper by lazy { DownloadNotificationHelper(this, DOWNLOAD_NOTIFICATION_CHANNEL_ID) }

    override fun getDownloadManager(): DownloadManager = store.manager

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(downloads: List<Download>, notMetRequirements: Int): Notification =
        notificationHelper.buildProgressNotification(
            this,
            android.R.drawable.stat_sys_download,
            null,
            getString(R.string.core_playback_service_download_notification_message),
            downloads,
            notMetRequirements,
        )

    private companion object {
        const val DOWNLOAD_NOTIFICATION_ID = 4102
        const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "music_downloads"
    }
}
