package com.resonote.core.playback.service

import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class CommunicationPlaybackGuard(
    private val audioManager: AudioManager,
    private val player: Player,
    private val scope: CoroutineScope,
    private val mainExecutor: java.util.concurrent.Executor,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
    private val legacyPollIntervalMillis: Long = LEGACY_POLL_INTERVAL_MILLIS,
) {
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) {
                legacyPollingJob?.cancel()
                legacyPollingJob = null
                return
            }

            pauseForAudioMode(audioManager.mode)
            startLegacyPollingIfNeeded()
        }
    }
    private var legacyPollingJob: Job? = null
    private var unregisterModeChangedListener: (() -> Unit)? = null
    private var started = false

    fun start() {
        if (started) return
        started = true

        player.addListener(playerListener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && sdkInt >= Build.VERSION_CODES.S) {
            registerModeChangedListener()
        }
        if (player.isPlaying) {
            pauseForAudioMode(audioManager.mode)
            startLegacyPollingIfNeeded()
        }
    }

    fun release() {
        if (!started) return
        started = false

        player.removeListener(playerListener)
        legacyPollingJob?.cancel()
        legacyPollingJob = null
        unregisterModeChangedListener?.invoke()
        unregisterModeChangedListener = null
    }

    internal fun pauseForAudioMode(mode: Int) {
        if (player.isPlaying && isCommunicationAudioMode(mode, sdkInt)) {
            player.pause()
        }
    }

    private fun startLegacyPollingIfNeeded() {
        if (sdkInt >= Build.VERSION_CODES.S || !player.isPlaying || legacyPollingJob?.isActive == true) return

        legacyPollingJob = scope.launch {
            while (isActive && player.isPlaying) {
                pauseForAudioMode(audioManager.mode)
                delay(legacyPollIntervalMillis)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerModeChangedListener() {
        val listener = AudioManager.OnModeChangedListener(::pauseForAudioMode)
        audioManager.addOnModeChangedListener(mainExecutor, listener)
        unregisterModeChangedListener = { audioManager.removeOnModeChangedListener(listener) }
    }

    private companion object {
        const val LEGACY_POLL_INTERVAL_MILLIS = 250L
    }
}

internal fun isCommunicationAudioMode(mode: Int, sdkInt: Int): Boolean = mode == AudioManager.MODE_RINGTONE ||
    mode == AudioManager.MODE_IN_CALL ||
    mode == AudioManager.MODE_IN_COMMUNICATION ||
    (sdkInt >= Build.VERSION_CODES.R && mode == AudioManager.MODE_CALL_SCREENING) ||
    (sdkInt >= Build.VERSION_CODES.S && mode == AudioManager.MODE_CALL_REDIRECT) ||
    (sdkInt >= Build.VERSION_CODES.TIRAMISU && mode == AudioManager.MODE_COMMUNICATION_REDIRECT)
