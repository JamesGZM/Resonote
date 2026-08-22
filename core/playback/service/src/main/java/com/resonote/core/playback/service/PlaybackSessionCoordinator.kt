package com.resonote.core.playback.service

import com.resonote.core.data.PlaybackSessionRepository
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.playback.PlaybackState
import kotlinx.coroutines.CoroutineScope

internal class PlaybackSessionCoordinator(repository: PlaybackSessionRepository, persistenceScope: CoroutineScope) {
    private val persister = PlaybackSessionPersister(repository, persistenceScope)
    private val repository = repository

    suspend fun load(playbackSpeed: PlaybackSpeed): PlaybackState? =
        runCatching { repository.load() }.getOrNull()?.toPlaybackState(playbackSpeed)

    fun persist(queue: PlaybackQueue, state: PlaybackState, positionMillis: Long = state.positionMillis) {
        val items = queue.items
        val currentIndex = queue.currentIndex
        if (items.isEmpty() || currentIndex !in items.indices) {
            clear()
            return
        }
        persister.save(
            items = items,
            currentIndex = currentIndex,
            positionMillis = positionMillis,
            mode = state.mode.name,
        )
    }

    fun clear() {
        persister.clear()
    }
}
