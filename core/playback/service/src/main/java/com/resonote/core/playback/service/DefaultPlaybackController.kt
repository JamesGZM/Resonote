package com.resonote.core.playback.service

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.PlaybackController
import com.resonote.core.playback.PlaybackIssue
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackMode
import com.resonote.core.playback.PlaybackState
import com.resonote.core.playback.PlaybackStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
internal class DefaultPlaybackController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sourceResolver: PlaybackSourceResolver,
) : PlaybackController, Player.Listener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val queue = PlaybackQueue()
    private val mutableState = MutableStateFlow(PlaybackState())
    private val controllerFuture = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, ResonotePlaybackService::class.java)),
    ).buildAsync()

    private var controller: MediaController? = null
    private var pendingControllerAction: ((MediaController) -> Unit)? = null
    private var loadGeneration = 0L
    private var handledEndedGeneration = -1L
    private var isResolving = false
    private var positionUpdates: Job? = null

    override val state: StateFlow<PlaybackState> = mutableState.asStateFlow()

    init {
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }
                    .onSuccess { connectedController ->
                        controller = connectedController
                        connectedController.addListener(this)
                        pendingControllerAction?.also { action ->
                            pendingControllerAction = null
                            action(connectedController)
                        }
                        syncPlayerState(connectedController)
                        startPositionUpdates()
                    }
                    .onFailure { failure ->
                        mutableState.value = mutableState.value.copy(
                            status = PlaybackStatus.Failed,
                            issue = PlaybackIssue.PlayerFailure(failure.message),
                        )
                    }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    override fun play(item: PlaybackItem) {
        scope.launch {
            queue.selectOrInsert(item)
            publishQueue(status = PlaybackStatus.Resolving)
            resolveAndLoad(item)
        }
    }

    override fun playAll(items: List<PlaybackItem>, startIndex: Int) {
        require(items.isNotEmpty()) { "Playback queue must not be empty" }
        require(startIndex in items.indices) { "startIndex must point to an item" }
        scope.launch {
            queue.replace(items, startIndex)
            publishQueue(status = PlaybackStatus.Resolving)
            resolveAndLoad(checkNotNull(queue.currentItem))
        }
    }

    override fun append(items: List<PlaybackItem>) {
        if (items.isEmpty()) return
        scope.launch {
            queue.append(items)
            publishQueue()
        }
    }

    override fun selectQueueItem(index: Int) {
        scope.launch {
            if (index == queue.currentIndex) return@launch
            val item = queue.select(index) ?: return@launch
            publishQueue(status = PlaybackStatus.Resolving)
            resolveAndLoad(item)
        }
    }

    override fun removeQueueItem(index: Int) {
        scope.launch {
            val removal = queue.removeAt(index) ?: return@launch
            if (!removal.removedCurrent) {
                publishQueue()
                return@launch
            }

            loadGeneration++
            isResolving = false
            pendingControllerAction = null
            controller?.stop()
            controller?.clearMediaItems()
            val next = removal.nextCurrentItem
            if (next == null) {
                mutableState.value = PlaybackState(mode = mutableState.value.mode)
            } else {
                publishQueue(status = PlaybackStatus.Resolving)
                resolveAndLoad(next)
            }
        }
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        scope.launch {
            if (queue.move(fromIndex, toIndex)) publishQueue()
        }
    }

    override fun togglePlayPause() {
        if (controller?.mediaItemCount == 0) {
            queue.currentItem?.let { item ->
                scope.launch {
                    publishQueue(status = PlaybackStatus.Resolving)
                    resolveAndLoad(item)
                }
            }
            return
        }
        runWithController { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                player.play()
            }
        }
    }

    override fun pause() {
        loadGeneration++
        isResolving = false
        pendingControllerAction = null
        controller?.pause()
        if (mutableState.value.currentItem != null && mutableState.value.status != PlaybackStatus.Failed) {
            mutableState.value = mutableState.value.copy(status = PlaybackStatus.Paused)
        }
    }

    override fun next() {
        scope.launch { selectNext(automatic = false) }
    }

    override fun previous() {
        scope.launch {
            val player = controller
            if (player != null && player.currentPosition > PREVIOUS_RESTART_THRESHOLD_MILLIS) {
                player.seekTo(0)
                return@launch
            }
            val item = when (mutableState.value.mode) {
                PlaybackMode.Shuffle -> queue.selectRandom(Random::nextInt)
                PlaybackMode.Sequential -> queue.previous(wrap = false)
                PlaybackMode.ListLoop, PlaybackMode.SingleLoop -> queue.previous(wrap = true)
            } ?: return@launch
            publishQueue(status = PlaybackStatus.Resolving)
            resolveAndLoad(item)
        }
    }

    override fun seekTo(positionMillis: Long) {
        runWithController { it.seekTo(positionMillis.coerceAtLeast(0)) }
    }

    override fun setMode(mode: PlaybackMode) {
        mutableState.value = mutableState.value.copy(mode = mode)
    }

    override fun clear() {
        scope.launch {
            loadGeneration++
            isResolving = false
            pendingControllerAction = null
            queue.clear()
            controller?.stop()
            controller?.clearMediaItems()
            mutableState.value = PlaybackState(mode = mutableState.value.mode)
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        syncPlayerState(player)
        if (player.playbackState == Player.STATE_ENDED && handledEndedGeneration != loadGeneration) {
            handledEndedGeneration = loadGeneration
            scope.launch { selectNext(automatic = true) }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        isResolving = false
        mutableState.value = mutableState.value.copy(
            status = PlaybackStatus.Failed,
            issue = PlaybackIssue.PlayerFailure(error.message),
        )
    }

    private suspend fun resolveAndLoad(item: PlaybackItem) {
        val generation = ++loadGeneration
        handledEndedGeneration = -1L
        isResolving = true
        controller?.stop()
        controller?.clearMediaItems()

        val result = sourceResolver.resolve(item)
        if (generation != loadGeneration) return

        when (result) {
            is ResolveSongSourceResult.Resolved -> loadResolvedItem(item, result.source, generation)
            is ResolveSongSourceResult.Unavailable -> failResolution(
                generation,
                PlaybackIssue.Unavailable(result.reason),
            )
            is ResolveSongSourceResult.Failed -> failResolution(
                generation,
                PlaybackIssue.SourceFailure(result.failure),
            )
        }
    }

    private fun loadResolvedItem(item: PlaybackItem, source: ResolvedSongSource, generation: Long) {
        queue.selectOrInsert(item.copy(resolvedSource = source))
        publishQueue(status = PlaybackStatus.Buffering)
        runWithController { player ->
            if (generation != loadGeneration) return@runWithController
            isResolving = false
            player.setMediaItem(item.toMediaItem(source))
            player.prepare()
            player.play()
            syncPlayerState(player)
        }
    }

    private fun failResolution(generation: Long, issue: PlaybackIssue) {
        if (generation != loadGeneration) return
        isResolving = false
        pendingControllerAction = null
        controller?.stop()
        controller?.clearMediaItems()
        publishQueue(status = PlaybackStatus.Failed, issue = issue)
    }

    private suspend fun selectNext(automatic: Boolean) {
        if (queue.currentItem == null) return
        val mode = mutableState.value.mode
        if (automatic && mode == PlaybackMode.SingleLoop) {
            controller?.let { player ->
                player.seekTo(0)
                player.play()
            }
            return
        }

        val item = when (mode) {
            PlaybackMode.Shuffle -> queue.selectRandom(Random::nextInt)
            PlaybackMode.Sequential -> queue.next(wrap = false)
            PlaybackMode.ListLoop, PlaybackMode.SingleLoop -> queue.next(wrap = true)
        }
        if (item == null) {
            mutableState.value = mutableState.value.copy(status = PlaybackStatus.Ended)
            return
        }
        publishQueue(status = PlaybackStatus.Resolving)
        resolveAndLoad(item)
    }

    private fun publishQueue(
        status: PlaybackStatus = mutableState.value.status,
        issue: PlaybackIssue? = null,
    ) {
        val currentDuration = queue.currentItem?.resolvedSource?.durationMillis
            ?: queue.currentItem?.song?.durationMillis
            ?: 0L
        mutableState.value = mutableState.value.copy(
            queue = queue.items,
            currentIndex = queue.currentIndex,
            status = status,
            positionMillis = if (status == PlaybackStatus.Resolving) 0 else mutableState.value.positionMillis,
            durationMillis = currentDuration,
            bufferedPositionMillis = if (status == PlaybackStatus.Resolving) 0 else mutableState.value.bufferedPositionMillis,
            issue = issue,
        )
    }

    private fun runWithController(action: (MediaController) -> Unit) {
        val connectedController = controller
        if (connectedController == null) {
            pendingControllerAction = action
        } else {
            action(connectedController)
        }
    }

    private fun syncPlayerState(player: Player) {
        if (isResolving) return
        val duration = player.duration.takeUnless { it == C.TIME_UNSET || it < 0 }
            ?: queue.currentItem?.resolvedSource?.durationMillis
            ?: queue.currentItem?.song?.durationMillis
            ?: 0L
        val status = when {
            player.playerError != null -> PlaybackStatus.Failed
            player.playbackState == Player.STATE_BUFFERING -> PlaybackStatus.Buffering
            player.playbackState == Player.STATE_ENDED -> PlaybackStatus.Ended
            player.isPlaying -> PlaybackStatus.Playing
            queue.currentItem != null -> PlaybackStatus.Paused
            else -> PlaybackStatus.Idle
        }
        mutableState.value = mutableState.value.copy(
            queue = queue.items,
            currentIndex = queue.currentIndex,
            status = status,
            positionMillis = player.currentPosition.coerceAtLeast(0),
            durationMillis = duration,
            bufferedPositionMillis = player.bufferedPosition.coerceAtLeast(0),
            issue = player.playerError?.let { PlaybackIssue.PlayerFailure(it.message) },
        )
    }

    private fun startPositionUpdates() {
        positionUpdates?.cancel()
        positionUpdates = scope.launch {
            while (isActive) {
                delay(POSITION_UPDATE_INTERVAL_MILLIS)
                controller?.let(::syncPlayerState)
            }
        }
    }

    private fun PlaybackItem.toMediaItem(source: ResolvedSongSource): MediaItem {
        val song = song
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.albumTitle)
            .setArtworkUri(song.coverUrl?.toUri())
            .setDurationMs(source.durationMillis.takeIf { it > 0 } ?: song.durationMillis)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setIsPlayable(true)
            .build()
        return MediaItem.Builder()
            .setMediaId(song.hash)
            .setUri(source.uri)
            .setMediaMetadata(metadata)
            .build()
    }

    private companion object {
        const val POSITION_UPDATE_INTERVAL_MILLIS = 500L
        const val PREVIOUS_RESTART_THRESHOLD_MILLIS = 5_000L
    }
}
