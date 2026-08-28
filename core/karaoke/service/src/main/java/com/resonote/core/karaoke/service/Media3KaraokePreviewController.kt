package com.resonote.core.karaoke.service

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.transformer.CompositionPlayer
import com.resonote.core.data.KaraokeRepository
import com.resonote.core.karaoke.KaraokePreviewController
import com.resonote.core.karaoke.KaraokePreviewState
import com.resonote.core.model.KaraokeProjectId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class Media3KaraokePreviewController @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: KaraokeRepository,
) : KaraokePreviewController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(KaraokePreviewState())
    override val state = mutableState.asStateFlow()
    private val player = CompositionPlayer.Builder(context).build().apply {
        addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    mutableState.value = mutableState.value.copy(isPlaying = isPlaying)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) mutableState.value = KaraokePreviewState()
                }
            },
        )
    }

    override fun toggle(projectId: KaraokeProjectId) {
        if (mutableState.value.projectId == projectId) {
            if (player.isPlaying) player.pause() else player.play()
            return
        }
        scope.launch {
            val input = repository.renderInput(projectId) ?: return@launch
            player.setComposition(KaraokeCompositionFactory.create(input))
            mutableState.value = KaraokePreviewState(projectId)
            player.prepare()
            player.play()
        }
    }

    override fun stop() {
        player.stop()
        mutableState.value = KaraokePreviewState()
    }
}
