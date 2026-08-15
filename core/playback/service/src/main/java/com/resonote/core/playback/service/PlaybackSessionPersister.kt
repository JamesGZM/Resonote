package com.resonote.core.playback.service

import com.resonote.core.data.PlaybackSessionRepository
import com.resonote.core.data.PlaybackSessionSnapshot
import com.resonote.core.playback.PlaybackItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

internal class PlaybackSessionPersister(private val repository: PlaybackSessionRepository, scope: CoroutineScope) {
    private val requests = Channel<Request>(Channel.CONFLATED)

    init {
        scope.launch { processRequests() }
    }

    fun save(items: List<PlaybackItem>, currentIndex: Int, positionMillis: Long, mode: String) {
        requests.trySend(Request.Save(items, currentIndex, positionMillis.coerceAtLeast(0), mode))
    }

    fun clear() {
        requests.trySend(Request.Clear)
    }

    private suspend fun processRequests() {
        for (request in requests) {
            try {
                when (request) {
                    Request.Clear -> repository.clear()
                    is Request.Save -> repository.save(
                        PlaybackSessionSnapshot(
                            entries = request.items.map(PlaybackItem::toSessionEntry),
                            currentIndex = request.currentIndex,
                            positionMillis = request.positionMillis,
                            mode = request.mode,
                        ),
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Persistence must never interrupt playback; a future request retries the latest state.
            }
        }
    }

    private sealed interface Request {
        data class Save(
            val items: List<PlaybackItem>,
            val currentIndex: Int,
            val positionMillis: Long,
            val mode: String,
        ) : Request

        data object Clear : Request
    }
}
