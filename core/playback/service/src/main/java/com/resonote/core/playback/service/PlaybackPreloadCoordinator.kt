package com.resonote.core.playback.service

import androidx.media3.common.Player
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.PlaybackState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class PlaybackPreloadCoordinator(
    private val sourceResolver: PlaybackSourceResolver,
    private val audioPreloader: PlaybackAudioPreloader,
    private val scope: CoroutineScope,
    private val elapsedRealtime: () -> Long,
) {
    private var preloadJob: Job? = null
    private var attemptedGeneration = -1L
    private var prefetchedSource: PrefetchedSource? = null

    fun maybePreload(player: Player, state: PlaybackState, queue: PlaybackQueue, generation: Long) {
        if (
            !shouldStartPlaybackPreload(
                status = state.status,
                positionMillis = player.currentPosition.coerceAtLeast(0),
                durationMillis = state.durationMillis,
                alreadyAttempted = attemptedGeneration == generation,
            )
        ) {
            return
        }
        attemptedGeneration = generation
        val item = playbackPreloadCandidate(queue, state.mode) ?: return
        preloadJob = scope.launch {
            val result = try {
                sourceResolver.resolve(item.copy(resolvedSource = null))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                return@launch
            }
            if (generation != attemptedGeneration || result !is ResolveSongSourceResult.Resolved) return@launch
            prefetchedSource = PrefetchedSource(
                queueKey = item.queueKey,
                source = result.source,
                resolvedAtElapsedRealtimeMillis = elapsedRealtime(),
            )
            try {
                audioPreloader.preload(result.source)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Preloading is opportunistic. Normal source loading remains the fallback.
            }
        }
    }

    fun consume(queueKey: String): ResolvedSongSource? {
        val prefetched = prefetchedSource
        prefetchedSource = null
        return prefetched?.source?.takeIf {
            prefetched.queueKey == queueKey &&
                isPrefetchedSourceFresh(
                    resolvedAtElapsedRealtimeMillis = prefetched.resolvedAtElapsedRealtimeMillis,
                    nowElapsedRealtimeMillis = elapsedRealtime(),
                )
        }
    }

    fun cancelAttempt() {
        preloadJob?.cancel()
        preloadJob = null
        attemptedGeneration = -1L
    }

    fun invalidate() {
        cancelAttempt()
        prefetchedSource = null
    }

    private data class PrefetchedSource(
        val queueKey: String,
        val source: ResolvedSongSource,
        val resolvedAtElapsedRealtimeMillis: Long,
    )
}
