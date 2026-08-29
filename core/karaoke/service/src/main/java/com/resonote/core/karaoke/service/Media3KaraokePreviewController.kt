package com.resonote.core.karaoke.service

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.CompositionPlayer
import com.resonote.core.data.KaraokeRepository
import com.resonote.core.karaoke.KaraokePreviewController
import com.resonote.core.karaoke.KaraokePreviewState
import com.resonote.core.model.KaraokeMixSettings
import com.resonote.core.model.KaraokeProjectId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(UnstableApi::class, ExperimentalApi::class)
internal class Media3KaraokePreviewController @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: KaraokeRepository,
) : KaraokePreviewController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(KaraokePreviewState())
    override val state = mutableState.asStateFlow()
    private var activeMixSettings: KaraokeMixSettings? = null
    private var progressJob: Job? = null
    private val player = CompositionPlayer.Builder(context).build().apply {
        addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    mutableState.value = mutableState.value.copy(isPlaying = isPlaying)
                    if (isPlaying) startProgressUpdates() else stopProgressUpdates(updateOnce = true)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) updateProgress()
                    if (playbackState == Player.STATE_ENDED) {
                        stopProgressUpdates()
                        mutableState.value = KaraokePreviewState()
                    }
                }
            },
        )
    }

    override fun toggle(projectId: KaraokeProjectId, mixSettings: KaraokeMixSettings?) {
        if (mutableState.value.projectId == projectId && activeMixSettings == mixSettings) {
            if (player.isPlaying) player.pause() else player.play()
            return
        }
        scope.launch {
            val storedInput = repository.renderInput(projectId) ?: return@launch
            val input = mixSettings?.let { storedInput.copy(project = storedInput.project.copy(mixSettings = it)) }
                ?: storedInput
            val composition = runCatching { KaraokeCompositionFactory.create(input) }.getOrNull() ?: return@launch
            val started = runCatching {
                player.setComposition(composition)
                activeMixSettings = mixSettings
                mutableState.value = KaraokePreviewState(projectId)
                player.prepare()
                player.play()
            }.isSuccess
            if (!started) {
                runCatching { player.stop() }
                activeMixSettings = null
                mutableState.value = KaraokePreviewState()
            }
        }
    }

    override fun seekTo(positionMillis: Long) {
        if (mutableState.value.projectId == null) return
        player.seekTo(positionMillis.coerceIn(0L, player.duration.coerceAtLeast(0L)))
        updateProgress()
    }

    override fun stop() {
        stopProgressUpdates()
        player.stop()
        activeMixSettings = null
        mutableState.value = KaraokePreviewState()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                updateProgress()
                delay(PREVIEW_PROGRESS_INTERVAL_MILLIS)
            }
        }
    }

    private fun stopProgressUpdates(updateOnce: Boolean = false) {
        progressJob?.cancel()
        progressJob = null
        if (updateOnce) updateProgress()
    }

    private fun updateProgress() {
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        mutableState.value = mutableState.value.copy(
            positionMillis = player.currentPosition.coerceIn(0L, duration.coerceAtLeast(0L)),
            durationMillis = duration,
        )
    }

    private companion object {
        const val PREVIEW_PROGRESS_INTERVAL_MILLIS = 100L
    }
}
