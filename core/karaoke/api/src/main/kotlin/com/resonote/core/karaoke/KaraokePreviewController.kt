package com.resonote.core.karaoke

import com.resonote.core.model.KaraokeProjectId
import kotlinx.coroutines.flow.StateFlow

data class KaraokePreviewState(val projectId: KaraokeProjectId? = null, val isPlaying: Boolean = false)

interface KaraokePreviewController {
    val state: StateFlow<KaraokePreviewState>

    fun toggle(projectId: KaraokeProjectId)

    fun stop()
}
