@file:androidx.media3.common.util.UnstableApi

package com.resonote.core.playback.service

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ResonotePlaybackService : MediaSessionService() {
    @Inject
    internal lateinit var mediaCache: PlaybackMediaCache

    @Inject
    internal lateinit var queueCommandRouter: PlaybackQueueCommandRouter

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val stopPlayback = ::pauseAllPlayersAndStopSelf
        setListener(
            object : Listener {
                override fun onForegroundServiceStartNotAllowedException() = stopPlayback()
            },
        )
        setMediaNotificationProvider(
            ForegroundSafeMediaNotificationProvider(
                delegate = DefaultMediaNotificationProvider(this),
                onForegroundServiceStartNotAllowed = stopPlayback,
            ),
        )
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(mediaCache.playbackDataSourceFactory))
            .build()
            .apply {
                setAudioAttributes(audioAttributes, true)
                setHandleAudioBecomingNoisy(true)
            }
        val player = QueueAwarePlayer(exoPlayer, queueCommandRouter)
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        clearListener()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
