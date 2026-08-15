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
import com.resonote.core.data.PlaybackSessionEntry
import com.resonote.core.data.PlaybackSessionEntryKind
import com.resonote.core.data.PlaybackSessionRepository
import com.resonote.core.data.PlaybackSessionSnapshot
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.LocalMediaId
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.PlaybackController
import com.resonote.core.playback.PlaybackFormat
import com.resonote.core.playback.PlaybackIssue
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackMetadata
import com.resonote.core.playback.PlaybackMode
import com.resonote.core.playback.PlaybackOrigin
import com.resonote.core.playback.PlaybackState
import com.resonote.core.playback.PlaybackStatus
import dagger.hilt.android.qualifiers.ApplicationContext
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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
internal class DefaultPlaybackController internal constructor(
    @param:ApplicationContext private val context: Context,
    private val sourceResolver: PlaybackSourceResolver,
    private val historyRepository: ListeningHistoryRepository,
    private val preferencesRepository: PlaybackPreferencesRepository,
    private val sessionRepository: PlaybackSessionRepository,
    private val elapsedRealtime: () -> Long,
) : PlaybackController,
    Player.Listener {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        sourceResolver: PlaybackSourceResolver,
        historyRepository: ListeningHistoryRepository,
        preferencesRepository: PlaybackPreferencesRepository,
        sessionRepository: PlaybackSessionRepository,
    ) : this(
        context,
        sourceResolver,
        historyRepository,
        preferencesRepository,
        sessionRepository,
        SystemClock::elapsedRealtime,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sessionPersister = PlaybackSessionPersister(sessionRepository, persistenceScope)
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
    private var pausedPreviewGeneration = -1L
    private var isResolving = false
    private var activeFailureBehavior = FailureBehavior.SkipQueueItem
    private var positionUpdates: Job? = null
    private var automaticSkipJob: Job? = null
    private var currentSourceRefreshJob: Job? = null
    private var isRefreshingCurrentSource = false
    private var hasPlaybackMutation = false
    private var lastPositionCheckpointAtMillis = 0L
    private val historyEligibility = PlaybackHistoryEligibilityTracker()
    private val failureRecovery = PlaybackFailureRecovery(MAX_CONSECUTIVE_AUTOMATIC_SKIPS)

    override val state: StateFlow<PlaybackState> = mutableState.asStateFlow()

    init {
        scope.launch { restoreSession() }
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
        hasPlaybackMutation = true
        scope.launch {
            failureRecovery.reset()
            mutableState.value = mutableState.value.copy(issue = null)
            resolveAndLoad(item, failureBehavior = FailureBehavior.RejectWithoutQueueMutation)
        }
    }

    override fun playAll(items: List<PlaybackItem>, startIndex: Int) {
        require(items.isNotEmpty()) { "Playback queue must not be empty" }
        require(startIndex in items.indices) { "startIndex must point to an item" }
        hasPlaybackMutation = true
        scope.launch {
            queue.replace(items, startIndex)
            failureRecovery.reset()
            publishQueue(status = PlaybackStatus.Resolving)
            requestPersistSession()
            resolveAndLoad(checkNotNull(queue.currentItem), failureBehavior = FailureBehavior.SkipQueueItem)
        }
    }

    override fun append(items: List<PlaybackItem>) {
        if (items.isEmpty()) return
        hasPlaybackMutation = true
        scope.launch {
            queue.append(items)
            publishQueue()
            requestPersistSession()
        }
    }

    override fun playNext(items: List<PlaybackItem>) {
        if (items.isEmpty()) return
        hasPlaybackMutation = true
        scope.launch {
            queue.playNext(items)
            publishQueue()
            requestPersistSession()
        }
    }

    override fun selectQueueItem(index: Int) {
        hasPlaybackMutation = true
        scope.launch {
            if (index == queue.currentIndex && mutableState.value.status != PlaybackStatus.Failed) return@launch
            val item = queue.select(index) ?: return@launch
            failureRecovery.reset()
            publishQueue(status = PlaybackStatus.Resolving)
            requestPersistSession()
            resolveAndLoad(item, failureBehavior = FailureBehavior.SkipQueueItem)
        }
    }

    override fun removeQueueItem(index: Int) {
        hasPlaybackMutation = true
        scope.launch {
            val removal = queue.removeAt(index) ?: return@launch
            if (!removal.removedCurrent) {
                publishQueue()
                requestPersistSession()
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
                requestClearSession()
            } else {
                publishQueue(status = PlaybackStatus.Resolving)
                requestPersistSession()
                resolveAndLoad(next, failureBehavior = FailureBehavior.SkipQueueItem)
            }
        }
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        hasPlaybackMutation = true
        scope.launch {
            if (queue.move(fromIndex, toIndex)) {
                publishQueue()
                requestPersistSession()
            }
        }
    }

    override fun togglePlayPause() {
        hasPlaybackMutation = true
        if ((controller?.mediaItemCount ?: 0) == 0) {
            queue.currentItem?.let { item ->
                scope.launch {
                    failureRecovery.reset()
                    val restoredPosition = mutableState.value.positionMillis
                    publishQueue(status = PlaybackStatus.Resolving)
                    resolveAndLoad(
                        item,
                        failureBehavior = FailureBehavior.SkipQueueItem,
                        startPositionMillis = restoredPosition,
                    )
                }
            }
            return
        }
        runWithController { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                if (
                    player.playbackState == Player.STATE_ENDED ||
                    pausedPreviewGeneration == loadGeneration
                ) {
                    pausedPreviewGeneration = -1L
                    player.seekTo(0)
                }
                player.play()
            }
        }
    }

    override fun pause() {
        hasPlaybackMutation = true
        sampleHistory(controller?.isPlaying == true, endedNaturally = false)
        loadGeneration++
        isResolving = false
        pendingControllerAction = null
        controller?.pause()
        if (mutableState.value.currentItem != null && mutableState.value.status != PlaybackStatus.Failed) {
            mutableState.value = mutableState.value.copy(status = PlaybackStatus.Paused)
        }
        requestPersistSession()
    }

    override fun next() {
        hasPlaybackMutation = true
        failureRecovery.reset()
        scope.launch { selectNext(automatic = false) }
    }

    override fun previous() {
        hasPlaybackMutation = true
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
            requestPersistSession()
            resolveAndLoad(item, failureBehavior = FailureBehavior.SkipQueueItem)
        }
    }

    override fun seekTo(positionMillis: Long) {
        hasPlaybackMutation = true
        val previewDurationMillis = queue.currentItem?.vipPreviewDurationMillisOrNull()
        val target = positionMillis.coerceIn(0, previewDurationMillis ?: Long.MAX_VALUE)
        runWithController {
            it.seekTo(target)
        }
        mutableState.value = mutableState.value.copy(positionMillis = target)
        requestPersistSession(positionMillis = target)
    }

    override fun setMode(mode: PlaybackMode) {
        hasPlaybackMutation = true
        mutableState.value = mutableState.value.copy(mode = mode)
        requestPersistSession()
    }

    override fun setPlaybackSpeed(speed: PlaybackSpeed) {
        if (mutableState.value.playbackSpeed == speed) return
        mutableState.value = mutableState.value.copy(playbackSpeed = speed)
        runWithController { it.setPlaybackSpeed(speed.factor) }
        scope.launch { preferencesRepository.setPlaybackSpeed(speed) }
    }

    override fun refreshCurrentOnlineSource(force: Boolean) {
        val item = queue.currentItem ?: return
        val player = controller
        if (
            !item.shouldRefreshOnlineSource(force) ||
            isResolving ||
            currentSourceRefreshJob?.isActive == true
        ) {
            return
        }
        val hasLoadedMedia = (player?.mediaItemCount ?: 0) > 0
        val startPositionMillis = if (hasLoadedMedia) {
            player?.currentPosition?.coerceAtLeast(0) ?: 0
        } else {
            mutableState.value.positionMillis
        }
        val playWhenReady = hasLoadedMedia && player?.isPlaying == true
        isRefreshingCurrentSource = true
        currentSourceRefreshJob = scope.launch {
            val refreshed = try {
                resolveAndLoad(
                    item = item.copy(resolvedSource = null),
                    failureBehavior = FailureBehavior.RefreshCurrentSource,
                    startPositionMillis = startPositionMillis,
                    playWhenReady = playWhenReady,
                )
            } finally {
                isRefreshingCurrentSource = false
            }
            if (!refreshed) controller?.let(::handleVipPreviewBoundary)
        }
    }

    override fun clear() {
        hasPlaybackMutation = true
        scope.launch {
            sampleHistory(controller?.isPlaying == true, endedNaturally = false)
            historyEligibility.reset()
            loadGeneration++
            isResolving = false
            pendingControllerAction = null
            automaticSkipJob?.cancel()
            automaticSkipJob = null
            currentSourceRefreshJob?.cancel()
            currentSourceRefreshJob = null
            isRefreshingCurrentSource = false
            failureRecovery.reset()
            queue.clear()
            controller?.stop()
            controller?.clearMediaItems()
            mutableState.value = PlaybackState(
                mode = mutableState.value.mode,
                playbackSpeed = mutableState.value.playbackSpeed,
            )
            requestClearSession()
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        syncPlayerState(player)
        if (handleVipPreviewBoundary(player)) return
        if (player.playbackState == Player.STATE_ENDED && handledEndedGeneration != loadGeneration) {
            handledEndedGeneration = loadGeneration
            handleCompletionAction(playbackEndedCompletionAction(mutableState.value.mode), player)
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

    private suspend fun resolveAndLoad(
        item: PlaybackItem,
        failureBehavior: FailureBehavior,
        startPositionMillis: Long = 0,
        playWhenReady: Boolean = true,
    ): Boolean {
        automaticSkipJob?.cancel()
        automaticSkipJob = null
        val preservesCurrentPlayback =
            failureBehavior != FailureBehavior.SkipQueueItem &&
                ((controller?.mediaItemCount ?: 0) > 0 || failureBehavior == FailureBehavior.RefreshCurrentSource)
        val generation = ++loadGeneration
        if (!preservesCurrentPlayback) {
            sampleHistory(controller?.isPlaying == true, endedNaturally = false)
            historyEligibility.reset()
            activeFailureBehavior = failureBehavior
            handledEndedGeneration = -1L
            pausedPreviewGeneration = -1L
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
            return false
        }
        if (generation != loadGeneration) return false

        return when (result) {
            is ResolveSongSourceResult.Resolved -> {
                loadResolvedItem(
                    item = item,
                    source = result.source,
                    generation = generation,
                    preservesCurrentPlayback = preservesCurrentPlayback,
                    startPositionMillis = startPositionMillis,
                    playWhenReady = playWhenReady,
                )
                true
            }
            is ResolveSongSourceResult.Unavailable -> {
                failResolution(
                    generation,
                    PlaybackIssue.Unavailable(result.reason),
                    failureBehavior,
                    preservesCurrentPlayback,
                )
                false
            }
            is ResolveSongSourceResult.Failed -> {
                failResolution(
                    generation,
                    PlaybackIssue.SourceFailure(result.failure),
                    failureBehavior,
                    preservesCurrentPlayback,
                )
                false
            }
        }
    }

    private fun loadResolvedItem(
        item: PlaybackItem,
        source: ResolvedSongSource,
        generation: Long,
        preservesCurrentPlayback: Boolean,
        startPositionMillis: Long,
        playWhenReady: Boolean,
    ) {
        if (preservesCurrentPlayback) {
            sampleHistory(controller?.isPlaying == true, endedNaturally = false)
            historyEligibility.reset()
            activeFailureBehavior = FailureBehavior.RejectWithoutQueueMutation
            handledEndedGeneration = -1L
            pausedPreviewGeneration = -1L
            isResolving = true
            controller?.stop()
            controller?.clearMediaItems()
        }
        queue.selectOrInsert(item.withResolvedSource(source))
        val resolvedDuration = source.durationMillis.takeIf { it > 0 } ?: item.metadata.durationMillis
        val boundedStartPositionMillis = checkNotNull(queue.currentItem).coercePlaybackPosition(
            positionMillis = startPositionMillis,
            fallbackDurationMillis = resolvedDuration,
        )
        publishQueue(status = PlaybackStatus.Buffering)
        mutableState.value = mutableState.value.copy(positionMillis = boundedStartPositionMillis)
        requestPersistSession(positionMillis = boundedStartPositionMillis)
        runWithController { player ->
            if (generation != loadGeneration) return@runWithController
            isResolving = false
            historyEligibility.start(
                record = item.toDeviceHistoryRecordOrNull()?.copy(durationMillis = resolvedDuration),
                durationMillis = resolvedDuration,
                elapsedRealtimeMillis = elapsedRealtime(),
            )
            player.setMediaItem(item.toMediaItem(source))
            player.prepare()
            if (boundedStartPositionMillis > 0) player.seekTo(boundedStartPositionMillis)
            if (playWhenReady) player.play() else player.pause()
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
            if (failureBehavior == FailureBehavior.RefreshCurrentSource) return
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
            replayCurrentItem()
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
        requestPersistSession()
        resolveAndLoad(item, failureBehavior = FailureBehavior.SkipQueueItem)
    }

    private fun replayCurrentItem() {
        pausedPreviewGeneration = -1L
        controller?.let { player ->
            player.seekTo(0)
            player.play()
        }
    }

    private fun handleVipPreviewBoundary(player: Player): Boolean {
        if (handledEndedGeneration == loadGeneration) return false
        val item = queue.currentItem ?: return false
        if (isRefreshingCurrentSource && item.vipPreviewDurationMillisOrNull() != null) {
            player.pause()
            mutableState.value = mutableState.value.copy(status = PlaybackStatus.Paused)
            return true
        }
        val action = vipPreviewCompletionAction(
            item = item,
            mode = mutableState.value.mode,
            queueSize = queue.items.size,
            positionMillis = player.currentPosition.coerceAtLeast(0),
        ) ?: return false
        handledEndedGeneration = loadGeneration
        handleCompletionAction(action, player)
        return true
    }

    private fun handleCompletionAction(action: PlaybackCompletionAction, player: Player) {
        when (action) {
            PlaybackCompletionAction.Pause -> {
                pausedPreviewGeneration = loadGeneration
                player.pause()
                mutableState.value = mutableState.value.copy(status = PlaybackStatus.Paused)
            }
            PlaybackCompletionAction.Advance -> scope.launch { selectNext(automatic = true) }
            PlaybackCompletionAction.Replay -> replayCurrentItem()
        }
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
        requestPersistSession()
        resolveAndLoad(item, failureBehavior = FailureBehavior.SkipQueueItem)
    }

    private fun publishQueue(status: PlaybackStatus = mutableState.value.status, issue: PlaybackIssue? = null) {
        val currentDuration = queue.currentItem?.resolvedSource?.durationMillis
            ?: queue.currentItem?.metadata?.durationMillis
            ?: 0L
        mutableState.value = mutableState.value.copy(
            queue = queue.items,
            currentIndex = queue.currentIndex,
            status = status,
            positionMillis = if (status == PlaybackStatus.Resolving) 0 else mutableState.value.positionMillis,
            durationMillis = currentDuration,
            bufferedPositionMillis = if (status ==
                PlaybackStatus.Resolving
            ) {
                0
            } else {
                mutableState.value.bufferedPositionMillis
            },
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
        val hasLoadedMedia = player.mediaItemCount > 0
        val duration = queue.currentItem?.vipPreviewDurationMillisOrNull()
            ?: player.duration.takeUnless { it == C.TIME_UNSET || it < 0 }
            ?: queue.currentItem?.resolvedSource?.durationMillis
            ?: queue.currentItem?.metadata?.durationMillis
            ?: 0L
        val status = when {
            player.playerError != null -> PlaybackStatus.Failed
            player.playbackState == Player.STATE_BUFFERING -> PlaybackStatus.Buffering
            player.playbackState == Player.STATE_ENDED &&
                pausedPreviewGeneration == loadGeneration -> PlaybackStatus.Paused
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
            positionMillis = if (hasLoadedMedia) {
                player.currentPosition.coerceAtLeast(0)
            } else {
                mutableState.value.positionMillis
            },
            durationMillis = duration,
            bufferedPositionMillis = if (hasLoadedMedia) {
                player.bufferedPosition.coerceAtLeast(0)
            } else {
                mutableState.value.bufferedPositionMillis
            },
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
                controller?.let { player ->
                    syncPlayerState(player)
                    handleVipPreviewBoundary(player)
                    val now = elapsedRealtime()
                    if (
                        mutableState.value.status == PlaybackStatus.Playing &&
                        now - lastPositionCheckpointAtMillis >= POSITION_CHECKPOINT_INTERVAL_MILLIS
                    ) {
                        lastPositionCheckpointAtMillis = now
                        requestPersistSession()
                    }
                }
            }
        }
    }

    private suspend fun restoreSession() {
        val snapshot = runCatching { sessionRepository.load() }.getOrNull() ?: return
        if (hasPlaybackMutation || queue.currentItem != null) return
        val restoredState = snapshot.toPlaybackState(mutableState.value.playbackSpeed) ?: return
        queue.replace(restoredState.queue, restoredState.currentIndex)
        mutableState.value = restoredState
        lastPositionCheckpointAtMillis = elapsedRealtime()
    }

    private fun requestPersistSession(positionMillis: Long = mutableState.value.positionMillis) {
        val items = queue.items
        val currentIndex = queue.currentIndex
        if (items.isEmpty() || currentIndex !in items.indices) {
            requestClearSession()
            return
        }
        sessionPersister.save(
            items = items,
            currentIndex = currentIndex,
            positionMillis = positionMillis,
            mode = mutableState.value.mode.name,
        )
    }

    private fun requestClearSession() {
        sessionPersister.clear()
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
        const val POSITION_CHECKPOINT_INTERVAL_MILLIS = 5_000L
        const val PREVIOUS_RESTART_THRESHOLD_MILLIS = 5_000L
        const val AUTOMATIC_SKIP_DELAY_MILLIS = 3_000L
        const val MAX_CONSECUTIVE_AUTOMATIC_SKIPS = 5
    }

    private enum class FailureBehavior {
        RejectWithoutQueueMutation,
        RefreshCurrentSource,
        SkipQueueItem,
    }
}

internal fun PlaybackItem.toSessionEntry(): PlaybackSessionEntry {
    val playbackMetadata = metadata
    val localFormat = playbackMetadata.format as? PlaybackFormat.Local
    return PlaybackSessionEntry(
        kind = when (origin) {
            is PlaybackOrigin.Online -> PlaybackSessionEntryKind.Online
            is PlaybackOrigin.Cloud -> PlaybackSessionEntryKind.Cloud
            is PlaybackOrigin.Local -> PlaybackSessionEntryKind.Local
        },
        mediaId = playbackMetadata.mediaId,
        title = playbackMetadata.title,
        artist = playbackMetadata.artist,
        albumTitle = playbackMetadata.albumTitle,
        artworkUri = playbackMetadata.artworkUri,
        durationMillis = playbackMetadata.durationMillis,
        isVip = playbackMetadata.isVip,
        audioQuality = (origin as? PlaybackOrigin.Online)?.song?.quality,
        albumId = (origin as? PlaybackOrigin.Online)?.song?.albumId,
        albumAudioId = when (val value = origin) {
            is PlaybackOrigin.Online -> value.song.albumAudioId
            is PlaybackOrigin.Cloud -> value.track.albumAudioId
            is PlaybackOrigin.Local -> null
        },
        fileId = (origin as? PlaybackOrigin.Online)?.song?.fileId,
        previewDurationMillis = (origin as? PlaybackOrigin.Online)?.song?.previewDurationMillis,
        mimeType = localFormat?.mimeType,
        extension = localFormat?.extension,
        sampleRateHz = localFormat?.sampleRateHz,
        bitDepth = localFormat?.bitDepth,
        bitrateBitsPerSecond = localFormat?.bitrateBitsPerSecond,
    )
}

internal fun PlaybackSessionEntry.toPlaybackItem(): PlaybackItem? {
    if (mediaId.isBlank() || title.isBlank() || durationMillis < 0) return null
    val metadata = PlaybackMetadata(
        mediaId = mediaId,
        title = title,
        artist = artist,
        albumTitle = albumTitle,
        artworkUri = artworkUri,
        durationMillis = durationMillis,
        format = when (kind) {
            PlaybackSessionEntryKind.Online -> PlaybackFormat.Online(audioQuality ?: AudioQuality.Standard)
            PlaybackSessionEntryKind.Cloud -> PlaybackFormat.Cloud(extension = null)
            PlaybackSessionEntryKind.Local -> PlaybackFormat.Local(
                mimeType = mimeType,
                extension = extension,
                sampleRateHz = sampleRateHz,
                bitDepth = bitDepth,
                bitrateBitsPerSecond = bitrateBitsPerSecond,
            )
        },
        isVip = isVip,
    )
    val origin = when (kind) {
        PlaybackSessionEntryKind.Online -> PlaybackOrigin.Online(
            OnlineSong(
                hash = mediaId,
                title = title,
                artist = artist,
                coverUrl = artworkUri,
                albumId = albumId,
                albumAudioId = albumAudioId,
                durationMillis = durationMillis,
                quality = audioQuality ?: AudioQuality.Standard,
                vip = isVip,
                albumTitle = albumTitle,
                fileId = fileId,
                previewDurationMillis = previewDurationMillis,
            ),
        )
        PlaybackSessionEntryKind.Cloud -> PlaybackOrigin.Cloud(
            CloudTrack(
                hash = mediaId,
                title = title,
                artist = artist,
                album = albumTitle,
                coverUrl = artworkUri,
                durationMillis = durationMillis,
                albumAudioId = albumAudioId,
            ),
        )
        PlaybackSessionEntryKind.Local -> PlaybackOrigin.Local(LocalMediaId(mediaId))
    }
    return PlaybackItem(metadata = metadata, origin = origin)
}

internal fun PlaybackSessionSnapshot.toPlaybackState(playbackSpeed: PlaybackSpeed): PlaybackState? {
    val restoredItems = entries.mapNotNull(PlaybackSessionEntry::toPlaybackItem)
    if (restoredItems.isEmpty() || restoredItems.size != entries.size || currentIndex !in restoredItems.indices) {
        return null
    }
    val snapshotIndex = currentIndex
    val restoredQueue = PlaybackQueue().apply { replace(restoredItems, snapshotIndex) }
    val currentItem = restoredQueue.currentItem ?: return null
    val duration = currentItem.metadata.durationMillis
    return PlaybackState(
        queue = restoredQueue.items,
        currentIndex = restoredQueue.currentIndex,
        status = PlaybackStatus.Paused,
        positionMillis = positionMillis.coerceIn(0, duration.takeIf { it > 0 } ?: Long.MAX_VALUE),
        durationMillis = duration,
        bufferedPositionMillis = 0,
        mode = PlaybackMode.entries.firstOrNull { it.name == mode } ?: PlaybackMode.ListLoop,
        playbackSpeed = playbackSpeed,
        issue = null,
    )
}
