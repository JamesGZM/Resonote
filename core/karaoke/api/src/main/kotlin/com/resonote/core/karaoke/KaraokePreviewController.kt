package com.resonote.core.karaoke

import com.resonote.core.model.KaraokeMixSettings
import com.resonote.core.model.KaraokeProjectId
import kotlinx.coroutines.flow.StateFlow

data class KaraokePreviewState(
    val projectId: KaraokeProjectId? = null,
    val isPlaying: Boolean = false,
    val positionMillis: Long = 0,
    val durationMillis: Long = 0,
)

interface KaraokePreviewController {
    val state: StateFlow<KaraokePreviewState>

    fun toggle(projectId: KaraokeProjectId, mixSettings: KaraokeMixSettings? = null)

    fun seekTo(positionMillis: Long)

    fun stop()
}
