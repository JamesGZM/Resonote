package com.resonote.core.playback.service

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.resonote.core.data.ListeningHistoryRepository
import com.resonote.core.data.PlaybackPreferencesRepository
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.model.PlaybackSpeed
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
import kotlinx.coroutines.CancellationException
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
internal class DefaultPlaybackController internal constructor(
    @param:ApplicationContext private val context: Context,
    private val sourceResolver: PlaybackSourceResolver,
    private val historyRepository: ListeningHistoryRepository,
    private val preferencesRepository: PlaybackPreferencesRepository,
    private val elapsedRealtime: () -> Long,
) : PlaybackController, Player.Listener {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        sourceResolver: PlaybackSourceResolver,
        historyRepository: ListeningHistoryRepository,
        preferencesRepository: PlaybackPreferencesRepository,
    ) : this(context, sourceResolver, historyRepository, preferencesRepository, SystemClock::elapsedRealtime)

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
    private var activeFailureBehavior = FailureBehavior.SkipQueueItem
    private var positionUpdates: Job? = null
    private var automaticSkipJob: Job? = null
    private val historyEligibility = PlaybackHistoryEligibilityTracker()
    private val failureRecovery = PlaybackFailureRecovery(MAX_CONSECUTIVE_AUTOMATIC_SKIPS)

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
                        connectedController.setPlaybackSpeed(mutableState.value.playbackSpeed.factor)
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
        scope.launch {
            preferencesRepository.playbackSpeed.collect { speed ->
                if (mutableState.value.playbackSpeed != speed) {
                    mutableState.value = mutableState.value.copy(playbackSpeed = speed)
                }
                runWithController { it.setPlaybackSpeed(speed.factor) }
            }
        }
    }

    override fun play(item: PlaybackItem) {
        scope.launch {
            failureRecovery.reset()
            mutableState.value = mutableState.value.copy(issue = null)
            resolveAndLoad(item, failureBehavior = FailureBehavior.RejectWithoutQueueMutation)
        }
    }

    override fun playAll(items: List<PlaybackItem>, startIndex: Int) {
        require(items.isNotEmpty()) { "Playback queue must not be empty" }
        require(startIndex in items.indices) { "startIndex must point to an item" }
        scope.launch {
            queue.replace(items, startIndex)
            failureRecovery.reset()
            publishQueue(status = PlaybackStatus.Resolving)
            resolveAndLoad(checkNotNull(queue.currentItem), failureBehavior = FailureBehavior.SkipQueueItem)
        }
    }

    override fun append(items: List<PlaybackItem>) {
        if (items.isEmpty()) return
        scope.launch {
            queue.append(items)
            publishQueue()
        }
    }

    override fun playNext(items: List<PlaybackItem>) {
        if (items.isEmpty()) return
        scope.launch {
            queue.playNext(items)
            publishQueue()
        }
    }

    override fun selectQueueItem(index: Int) {
        scope.launch {
            if (index == queue.currentIndex && mutableState.value.status != PlaybackStatus.Failed) return@launch
            val item = queue.select(index) ?: return@launch
            failureRecovery.reset()
            publishQueue(status = PlaybackStatus.Resolving)
            resolveAndLoad(item, failureBehavior = FailureBehavior.SkipQueueItem)
        }
    }

    override fun removeQueueItem(index: Int) {
        scope.launch {
            val removal = queue.removeAt(index) ?: return@launch
            if (!removal.removedCurrent) {
                publishQueue()
                return@launch
            }

            sampleHistory(controller?.isPlaying == true, endedNaturally = false)
            historyEligibility.reset()
            loadGeneration++
            isResolving = false
            pendingControllerAction = null
            controller?.stop()
            controller?.clearMediaItems()
            val next = removal.nextCurrentItem
            if (next == null) {
                mutableState.value = PlaybackState(
                    mode = mutableState.value.mode,
                    playbackSpeed = mutableState.value.playbackSpeed,
                )
            } else {
                publishQueue(status = PlaybackStatus.Resolving)
                resolveAndLoad(next, failureBehavior = FailureBehavior.SkipQueueItem)
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
                    failureRecovery.reset()
                    publishQueue(status = PlaybackStatus.Resolving)
                    resolveAndLoad(item, failureBehavior = FailureBehavior.SkipQueueItem)
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
        sampleHistory(controller?.isPlaying == true, endedNaturally = false)
        loadGeneration++
        isResolving = false
        pendingControllerAction = null
        controller?.pause()
        if (mutableState.value.currentItem != null && mutableState.value.status != PlaybackStatus.Failed) {
            mutableState.value = mutableState.value.copy(status = PlaybackStatus.Paused)
        }
    }

    override fun next() {
        failureRecovery.reset()
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
            failureRecovery.reset()
            publishQueue(status = PlaybackStatus.Resolving)
            resolveAndLoad(item, failureBehavior = FailureBehavior.SkipQueueItem)
        }
    }

    override fun seekTo(positionMillis: Long) {
        runWithController { it.seekTo(positionMillis.coerceAtLeast(0)) }
    }

    override fun setMode(mode: PlaybackMode) {
        mutableState.value = mutableState.value.copy(mode = mode)
    }

    override fun setPlaybackSpeed(speed: PlaybackSpeed) {
        if (mutableState.value.playbackSpeed == speed) return
        mutableState.value = mutableState.value.copy(playbackSpeed = speed)
        runWithController { it.setPlaybackSpeed(speed.factor) }
        scope.launch { preferencesRepository.setPlaybackSpeed(speed) }
    }

    override fun clear() {
        scope.launch {
            sampleHistory(controller?.isPlaying == true, endedNaturally = false)
            historyEligibility.reset()
            loadGeneration++
            isResolving = false
            pendingControllerAction = null
            automaticSkipJob?.cancel()
            automaticSkipJob = null
            failureRecovery.reset()
            queue.clear()
            controller?.stop()
            controller?.clearMediaItems()
            mutableState.value = PlaybackState(
                mode = mutableState.value.mode,
                playbackSpeed = mutableState.value.playbackSpeed,
            )
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
        sampleHistory(isPlaying = false, endedNaturally = false)
        historyEligibility.reset()
        isResolving = false
        mutableState.value = mutableState.value.copy(
            status = PlaybackStatus.Failed,
            issue = PlaybackIssue.PlayerFailure(error.message),
        )
        if (activeFailureBehavior == FailureBehavior.SkipQueueItem) {
            scheduleAutomaticSkip(loadGeneration)
        }
    }

    private suspend fun resolveAndLoad(item: PlaybackItem, failureBehavior: FailureBehavior) {
        automaticSkipJob?.cancel()
        automaticSkipJob = null
        val preservesCurrentPlayback =
            failureBehavior == FailureBehavior.RejectWithoutQueueMutation &&
                (controller?.mediaItemCount ?: 0) > 0
        val generation = ++loadGeneration
        if (!preservesCurrentPlayback) {
            sampleHistory(controller?.isPlaying == true, endedNaturally = false)
            historyEligibility.reset()
            activeFailureBehavior = failureBehavior
            handledEndedGeneration = -1L
            isResolving = true
            controller?.stop()
            controller?.clearMediaItems()
        }

        val result = try {
            sourceResolver.resolve(item)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            failResolution(
                generation = generation,
                issue = PlaybackIssue.PlayerFailure(failure.message),
                failureBehavior = failureBehavior,
                preservesCurrentPlayback = preservesCurrentPlayback,
            )
            return
        }
        if (generation != loadGeneration) return

        when (result) {
            is ResolveSongSourceResult.Resolved -> loadResolvedItem(
                item = item,
                source = result.source,
                generation = generation,
                preservesCurrentPlayback = preservesCurrentPlayback,
            )
            is ResolveSongSourceResult.Unavailable -> failResolution(
                generation,
                PlaybackIssue.Unavailable(result.reason),
                failureBehavior,
                preservesCurrentPlayback,
            )
            is ResolveSongSourceResult.Failed -> failResolution(
                generation,
                PlaybackIssue.SourceFailure(result.failure),
                failureBehavior,
                preservesCurrentPlayback,
            )
        }
    }

    private fun loadResolvedItem(
        item: PlaybackItem,
        source: ResolvedSongSource,
        generation: Long,
        preservesCurrentPlayback: Boolean,
    ) {
        if (preservesCurrentPlayback) {
            sampleHistory(controller?.isPlaying == true, endedNaturally = false)
            historyEligibility.reset()
            activeFailureBehavior = FailureBehavior.RejectWithoutQueueMutation
            handledEndedGeneration = -1L
            isResolving = true
            controller?.stop()
            controller?.clearMediaItems()
        }
        queue.selectOrInsert(item.withResolvedSource(source))
        publishQueue(status = PlaybackStatus.Buffering)
        runWithController { player ->
            if (generation != loadGeneration) return@runWithController
            isResolving = false
            val resolvedDuration = source.durationMillis.takeIf { it > 0 } ?: item.metadata.durationMillis
            historyEligibility.start(
                record = item.toDeviceHistoryRecordOrNull()?.copy(durationMillis = resolvedDuration),
                durationMillis = resolvedDuration,
                elapsedRealtimeMillis = elapsedRealtime(),
            )
            player.setMediaItem(item.toMediaItem(source))
            player.prepare()
            player.play()
            syncPlayerState(player)
        }
    }

    private fun failResolution(
        generation: Long,
        issue: PlaybackIssue,
        failureBehavior: FailureBehavior,
        preservesCurrentPlayback: Boolean,
    ) {
        if (generation != loadGeneration) return
        if (preservesCurrentPlayback) {
            mutableState.value = mutableState.value.withNonInterruptingIssue(issue)
            return
        }
        historyEligibility.reset()
        isResolving = false
        pendingControllerAction = null
        controller?.stop()
        controller?.clearMediaItems()
        publishQueue(status = PlaybackStatus.Failed, issue = issue)
        if (failureBehavior == FailureBehavior.SkipQueueItem && issue.allowsAutomaticSkip()) {
            scheduleAutomaticSkip(generation)
        }
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
        resolveAndLoad(item, failureBehavior = FailureBehavior.SkipQueueItem)
    }

    private fun scheduleAutomaticSkip(generation: Long) {
        automaticSkipJob?.cancel()
        if (!failureRecovery.onFailure()) return
        automaticSkipJob = scope.launch {
            delay(AUTOMATIC_SKIP_DELAY_MILLIS)
            if (generation != loadGeneration || mutableState.value.status != PlaybackStatus.Failed) return@launch
            selectNextAfterFailure()
        }
    }

    private suspend fun selectNextAfterFailure() {
        val item = when (mutableState.value.mode) {
            PlaybackMode.Shuffle -> queue.selectRandom(Random::nextInt)
            PlaybackMode.Sequential -> queue.next(wrap = false)
            PlaybackMode.ListLoop, PlaybackMode.SingleLoop -> queue.next(wrap = true)
        }
        if (item == null) return
        publishQueue(status = PlaybackStatus.Resolving)
        resolveAndLoad(item, failureBehavior = FailureBehavior.SkipQueueItem)
    }

    private fun publishQueue(
        status: PlaybackStatus = mutableState.value.status,
        issue: PlaybackIssue? = null,
    ) {
        val currentDuration = queue.currentItem?.resolvedSource?.durationMillis
            ?: queue.currentItem?.metadata?.durationMillis
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
            ?: queue.currentItem?.metadata?.durationMillis
            ?: 0L
        val status = when {
            player.playerError != null -> PlaybackStatus.Failed
            player.playbackState == Player.STATE_BUFFERING -> PlaybackStatus.Buffering
            player.playbackState == Player.STATE_ENDED -> PlaybackStatus.Ended
            player.isPlaying -> PlaybackStatus.Playing
            queue.currentItem != null -> PlaybackStatus.Paused
            else -> PlaybackStatus.Idle
        }
        if (status == PlaybackStatus.Playing) failureRecovery.onPlaybackStarted()
        mutableState.value = mutableState.value.copy(
            queue = queue.items,
            currentIndex = queue.currentIndex,
            status = status,
            positionMillis = player.currentPosition.coerceAtLeast(0),
            durationMillis = duration,
            bufferedPositionMillis = player.bufferedPosition.coerceAtLeast(0),
            issue = player.playerError?.let { PlaybackIssue.PlayerFailure(it.message) },
        )
        sampleHistory(
            isPlaying = status == PlaybackStatus.Playing,
            endedNaturally = status == PlaybackStatus.Ended,
        )
    }

    private fun sampleHistory(isPlaying: Boolean, endedNaturally: Boolean) {
        val qualification =
            historyEligibility.sample(
                isPlaying = isPlaying,
                endedNaturally = endedNaturally,
                elapsedRealtimeMillis = elapsedRealtime(),
            ) ?: return
        scope.launch {
            val persisted = historyRepository.recordDevicePlayback(qualification.record)
            historyEligibility.onPersistenceResult(qualification, persisted)
        }
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
        val playbackMetadata = metadata
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(playbackMetadata.title)
            .setArtist(playbackMetadata.artist)
            .setAlbumTitle(playbackMetadata.albumTitle)
            .setArtworkUri(playbackMetadata.artworkUri?.toUri())
            .setDurationMs(source.durationMillis.takeIf { it > 0 } ?: playbackMetadata.durationMillis)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setIsPlayable(true)
            .build()
        return MediaItem.Builder()
            .setMediaId(queueKey)
            .setUri(source.uri)
            .setMediaMetadata(mediaMetadata)
            .build()
    }

    private companion object {
        const val POSITION_UPDATE_INTERVAL_MILLIS = 500L
        const val PREVIOUS_RESTART_THRESHOLD_MILLIS = 5_000L
        const val AUTOMATIC_SKIP_DELAY_MILLIS = 3_000L
        const val MAX_CONSECUTIVE_AUTOMATIC_SKIPS = 5
    }

    private enum class FailureBehavior {
        RejectWithoutQueueMutation,
        SkipQueueItem,
    }
}
