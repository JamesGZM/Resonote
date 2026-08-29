@file:androidx.media3.common.util.UnstableApi

package com.resonote.core.playback.service

import android.content.Context
import android.media.AudioManager
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.resonote.core.data.PlaybackPreferencesRepository
import com.resonote.core.model.AudioFocusPolicy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ResonotePlaybackService : MediaSessionService() {
    @Inject
    internal lateinit var mediaCache: PlaybackMediaCache

    @Inject
    internal lateinit var queueCommandRouter: PlaybackQueueCommandRouter

    @Inject
    internal lateinit var playbackPreferencesRepository: PlaybackPreferencesRepository

    private var mediaSession: MediaSession? = null
    private var communicationPlaybackGuard: CommunicationPlaybackGuard? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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
        val equalizer = ResonoteEqualizerAudioProcessor()
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioOutputPlaybackParameters: Boolean,
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParameters)
                .setAudioProcessors(arrayOf(equalizer))
                .build()
        }
        val exoPlayer = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(mediaCache.playbackDataSourceFactory))
            .build()
            .apply {
                setAudioAttributes(audioAttributes, true)
                setHandleAudioBecomingNoisy(true)
            }
        val player = QueueAwarePlayer(exoPlayer, queueCommandRouter)
        mediaSession = MediaSession.Builder(this, player).build()
        communicationPlaybackGuard = CommunicationPlaybackGuard(
            audioManager = getSystemService(AudioManager::class.java),
            player = exoPlayer,
            scope = serviceScope,
            mainExecutor = ContextCompat.getMainExecutor(this),
        ).also(CommunicationPlaybackGuard::start)
        serviceScope.launch {
            playbackPreferencesRepository.preferences.collect { preferences ->
                equalizer.update(
                    enabled = preferences.equalizerEnabled,
                    lowDb = preferences.equalizerLowDb,
                    midDb = preferences.equalizerMidDb,
                    highDb = preferences.equalizerHighDb,
                )
                exoPlayer.setAudioAttributes(
                    audioAttributes,
                    preferences.audioFocusPolicy != AudioFocusPolicy.AllowAll,
                )
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        communicationPlaybackGuard?.release()
        communicationPlaybackGuard = null
        serviceScope.cancel()
        clearListener()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
