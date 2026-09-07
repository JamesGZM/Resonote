package com.resonote.core.playback.service

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.resonote.core.data.ListeningHistoryRepository
import com.resonote.core.data.PlaybackPreferencesRepository
import com.resonote.core.data.PlaybackSessionRepository
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.PlaybackController
import com.resonote.core.playback.PlaybackIssue
import com.resonote.core.playback.PlaybackItem
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
@OptIn(UnstableApi::class)
internal class DefaultPlaybackController internal constructor(
    @param:ApplicationContext private val context: Context,
    private val sourceResolver: PlaybackSourceResolver,
    private val historyRepository: ListeningHistoryRepository,
    private val preferencesRepository: PlaybackPreferencesRepository,
    sessionRepository: PlaybackSessionRepository,
    private val audioPreloader: PlaybackAudioPreloader,
    private val queueCommandRouter: PlaybackQueueCommandRouter,
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
        audioPreloader: PlaybackAudioPreloader,
        queueCommandRouter: PlaybackQueueCommandRouter,
    ) : this(
        context,
        sourceResolver,
        historyRepository,
        preferencesRepository,
        sessionRepository,
        audioPreloader,
        queueCommandRouter,
        SystemClock::elapsedRealtime,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sessionCoordinator = PlaybackSessionCoordinator(sessionRepository, persistenceScope)
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
    private val preloadCoordinator = PlaybackPreloadCoordinator(
        sourceResolver = sourceResolver,
        audioPreloader = audioPreloader,
        scope = scope,
        elapsedRealtime = elapsedRealtime,
    )
    private var isRefreshingCurrentSource = false
    private var hasPlaybackMutation = false
    private var pendingSeekPositionMillis: Long? = null
    private var pendingSeekRequestedAtMillis = 0L
    private var lastPositionCheckpointAtMillis = 0L
    private val historyRecorder = PlaybackHistoryRecorder(historyRepository, scope, elapsedRealtime)
    private val failureRecovery = PlaybackFailureRecovery(MAX_CONSECUTIVE_AUTOMATIC_SKIPS)

    override val state: StateFlow<PlaybackState> = mutableState.asStateFlow()

    init {
        queueCommandRouter.bind(onNext = ::next, onPrevious = ::previous)
        scope.launch { restoreSession() }
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }
                    .onSuccess { connectedController ->
                        controller = connectedController
                        connectedController.addListener(this)
                        publishAudioSessionId(connectedController.audioSessionId)
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
            preferencesRepository.preferences.collect { preferences ->
                val state = mutableState.value
                if (state.playbackSpeed != preferences.playbackSpeed || state.mode != preferences.playbackMode) {
                    mutableState.value = state.copy(
                        playbackSpeed = preferences.playbackSpeed,
                        mode = preferences.playbackMode,
                    )
                    if (queue.currentItem != null) requestPersistSession()
                }
                runWithController { it.setPlaybackSpeed(preferences.playbackSpeed.factor) }
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

    override fun replaceQueue(
        items: List<PlaybackItem>,
        startIndex: Int,
        positionMillis: Long,
        playWhenReady: Boolean,
    ) {
        require(items.isNotEmpty()) { "Playback queue must not be empty" }
        require(startIndex in items.indices) { "startIndex must point to an item" }
        hasPlaybackMutation = true
        scope.launch {
            queue.replace(items, startIndex)
            failureRecovery.reset()
            publishQueue(status = PlaybackStatus.Resolving)
            requestPersistSession(positionMillis = positionMillis)
            resolveAndLoad(
                item = checkNotNull(queue.currentItem),
                failureBehavior = FailureBehavior.SkipQueueItem,
                startPositionMillis = positionMillis,
                playWhenReady = playWhenReady,
            )
        }
    }

    override fun append(items: List<PlaybackItem>) {
        if (items.isEmpty()) return
        hasPlaybackMutation = true
        scope.launch {
            preloadCoordinator.invalidate()
            queue.append(items)
            publishQueue()
            requestPersistSession()
        }
    }

    override fun playNext(items: List<PlaybackItem>) {
        if (items.isEmpty()) return
        hasPlaybackMutation = true
        scope.launch {
            preloadCoordinator.invalidate()
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
            preloadCoordinator.invalidate()
            val removal = queue.removeAt(index) ?: return@launch
            if (!removal.removedCurrent) {
                publishQueue()
                requestPersistSession()
                return@launch
            }

            historyRecorder.sample(controller?.isPlaying == true, endedNaturally = false)
            historyRecorder.reset()
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
                    audioSessionId = mutableState.value.audioSessionId,
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
            preloadCoordinator.invalidate()
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
        historyRecorder.sample(controller?.isPlaying == true, endedNaturally = false)
        loadGeneration++
        isResolving = false
        pendingControllerAction = null
        preloadCoordinator.cancelAttempt()
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
            if (
                player != null && shouldRestartCurrentOnPrevious(
                    loadedQueueKey = player.currentMediaItem?.mediaId,
                    currentQueueKey = queue.currentItem?.queueKey,
                    positionMillis = player.currentPosition,
                )
            ) {
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
        pendingSeekPositionMillis = target
        pendingSeekRequestedAtMillis = elapsedRealtime()
        runWithController {
            it.seekTo(target)
        }
        mutableState.value = mutableState.value.copy(positionMillis = target)
        requestPersistSession(positionMillis = target)
    }

    override fun replaceCurrentItem(item: PlaybackItem, positionMillis: Long, playWhenReady: Boolean) {
        if (item.queueKey != queue.currentItem?.queueKey) return
        hasPlaybackMutation = true
        scope.launch {
            failureRecovery.reset()
            mutableState.value = mutableState.value.copy(issue = null)
            resolveAndLoad(
                item = item,
                failureBehavior = FailureBehavior.RejectWithoutQueueMutation,
                startPositionMillis = positionMillis,
                playWhenReady = playWhenReady,
            )
        }
    }

    override fun setMode(mode: PlaybackMode) {
        hasPlaybackMutation = true
        preloadCoordinator.invalidate()
        mutableState.value = mutableState.value.copy(mode = mode)
        requestPersistSession()
        scope.launch { preferencesRepository.setPlaybackMode(mode) }
    }

    override fun setPlaybackSpeed(speed: PlaybackSpeed) {
        if (mutableState.value.playbackSpeed == speed) return
        mutableState.value = mutableState.value.copy(playbackSpeed = speed)
        runWithController { it.setPlaybackSpeed(speed.factor) }
        scope.launch { preferencesRepository.setPlaybackSpeed(speed) }
    }

    override fun setCurrentOnlineQuality(quality: OnlinePlaybackQuality) {
        val item = queue.currentItem ?: return
        if (item.origin !is PlaybackOrigin.Online || item.onlineQualityOverride == quality) {
            return
        }
        val player = controller
        if (isResolving || currentSourceRefreshJob?.isActive == true) return
        val startPositionMillis = item.sourceRefreshPositionMillis(
            loadedPlayerPositionMillis = player?.takeIf { it.mediaItemCount > 0 }?.currentPosition,
            restoredPositionMillis = mutableState.value.positionMillis,
        )
        val playWhenReady = player?.isPlaying == true
        preloadCoordinator.invalidate()
        isRefreshingCurrentSource = true
        currentSourceRefreshJob = scope.launch {
            try {
                resolveAndLoad(
                    item = item.copy(
                        resolvedSource = null,
                        onlineQualityOverride = quality,
                    ),
                    failureBehavior = FailureBehavior.RejectWithoutQueueMutation,
                    startPositionMillis = startPositionMillis,
                    playWhenReady = playWhenReady,
                )
            } finally {
                isRefreshingCurrentSource = false
            }
        }
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
        val loadedPlayerPositionMillis = player
            ?.takeIf { it.mediaItemCount > 0 }
            ?.currentPosition
        val startPositionMillis = item.sourceRefreshPositionMillis(
            loadedPlayerPositionMillis = loadedPlayerPositionMillis,
            restoredPositionMillis = mutableState.value.positionMillis,
        )
        val playWhenReady = item.resolvedSource != null && player?.isPlaying == true
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
            historyRecorder.sample(controller?.isPlaying == true, endedNaturally = false)
            historyRecorder.reset()
            loadGeneration++
            isResolving = false
            pendingControllerAction = null
            automaticSkipJob?.cancel()
            automaticSkipJob = null
            currentSourceRefreshJob?.cancel()
            currentSourceRefreshJob = null
            preloadCoordinator.invalidate()
            isRefreshingCurrentSource = false
            failureRecovery.reset()
            queue.clear()
            controller?.stop()
            controller?.clearMediaItems()
            mutableState.value = PlaybackState(
                mode = mutableState.value.mode,
                playbackSpeed = mutableState.value.playbackSpeed,
                audioSessionId = mutableState.value.audioSessionId,
            )
            requestClearSession()
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (!shouldProcessPlaybackEvents(isResolving, mutableState.value.status)) return
        syncPlayerState(player)
        if (handleVipPreviewBoundary(player)) return
        if (player.playbackState == Player.STATE_ENDED && handledEndedGeneration != loadGeneration) {
            handledEndedGeneration = loadGeneration
            handleCompletionAction(playbackEndedCompletionAction(mutableState.value.mode), player)
        }
    }

    override fun onAudioSessionIdChanged(audioSessionId: Int) = publishAudioSessionId(audioSessionId)

    override fun onPlayerError(error: PlaybackException) {
        preloadCoordinator.invalidate()
        historyRecorder.sample(isPlaying = false, endedNaturally = false)
        historyRecorder.reset()
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
        retainLoadedMediaWhileResolving: Boolean = false,
    ): Boolean {
        automaticSkipJob?.cancel()
        automaticSkipJob = null
        val prefetched = preloadCoordinator.consume(item.queueKey)
        preloadCoordinator.cancelAttempt()
        val preservesCurrentPlayback =
            failureBehavior != FailureBehavior.SkipQueueItem &&
                ((controller?.mediaItemCount ?: 0) > 0 || failureBehavior == FailureBehavior.RefreshCurrentSource)
        val generation = ++loadGeneration
        if (!preservesCurrentPlayback) {
            historyRecorder.sample(controller?.isPlaying == true, endedNaturally = false)
            historyRecorder.reset()
            activeFailureBehavior = failureBehavior
            handledEndedGeneration = -1L
            pausedPreviewGeneration = -1L
            isResolving = true
            if (!retainLoadedMediaWhileResolving) {
                controller?.stop()
                controller?.clearMediaItems()
            }
        }

        val result = prefetched?.let(ResolveSongSourceResult::Resolved) ?: try {
            sourceResolver.resolve(item)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            failResolution(
                generation = generation,
                issue = PlaybackIssue.PlayerFailure(failure.message),
                failureBehavior = failureBehavior,
                preservesCurrentPlayback = preservesCurrentPlayback,
                retainLoadedMediaWhileResolving = retainLoadedMediaWhileResolving,
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
                    retainLoadedMediaWhileResolving,
                )
                false
            }
            is ResolveSongSourceResult.Failed -> {
                failResolution(
                    generation,
                    PlaybackIssue.SourceFailure(result.failure),
                    failureBehavior,
                    preservesCurrentPlayback,
                    retainLoadedMediaWhileResolving,
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
            historyRecorder.sample(controller?.isPlaying == true, endedNaturally = false)
            historyRecorder.reset()
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
            historyRecorder.start(
                target = item.toHistoryTargetOrNull()?.let { target ->
                    if (target is PlaybackHistoryTarget.Device) {
                        target.copy(record = target.record.copy(durationMillis = resolvedDuration))
                    } else {
                        target
                    }
                },
                durationMillis = resolvedDuration,
            )
            player.setMediaItem(item.toMediaItem(source), boundedStartPositionMillis)
            player.prepare()
            if (playWhenReady) player.play() else player.pause()
            syncPlayerState(player)
        }
    }

    private fun failResolution(
        generation: Long,
        issue: PlaybackIssue,
        failureBehavior: FailureBehavior,
        preservesCurrentPlayback: Boolean,
        retainLoadedMediaWhileResolving: Boolean,
    ) {
        if (generation != loadGeneration) return
        if (preservesCurrentPlayback) {
            if (failureBehavior == FailureBehavior.RefreshCurrentSource) return
            mutableState.value = mutableState.value.withNonInterruptingIssue(issue)
            return
        }
        historyRecorder.reset()
        isResolving = false
        pendingControllerAction = null
        if (!retainLoadedMediaWhileResolving) {
            controller?.stop()
            controller?.clearMediaItems()
        }
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
        resolveAndLoad(
            item = item,
            failureBehavior = FailureBehavior.SkipQueueItem,
            retainLoadedMediaWhileResolving = shouldRetainLoadedMediaWhileResolvingNext(
                automatic = automatic,
                loadedMediaItemCount = controller?.mediaItemCount ?: 0,
            ),
        )
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
        resolveAndLoad(
            item = item,
            failureBehavior = FailureBehavior.SkipQueueItem,
            retainLoadedMediaWhileResolving = shouldRetainLoadedMediaWhileResolvingNext(
                automatic = true,
                loadedMediaItemCount = controller?.mediaItemCount ?: 0,
            ),
        )
    }

    private fun publishQueue(status: PlaybackStatus = mutableState.value.status, issue: PlaybackIssue? = null) {
        if (status == PlaybackStatus.Resolving) pendingSeekPositionMillis = null
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

    private fun publishAudioSessionId(audioSessionId: Int) {
        mutableState.value = mutableState.value.copy(audioSessionId = audioSessionId.takeIf { it > 0 })
    }

    private fun syncPlayerState(player: Player) {
        if (isResolving) return
        val snapshot = player.snapshotPlaybackState(
            queue = queue,
            previousState = mutableState.value,
            pausedPreviewGeneration = pausedPreviewGeneration,
            loadGeneration = loadGeneration,
        )
        val pendingSeekPosition = pendingSeekPositionMillis
        val retainsPendingSeek = pendingSeekPosition != null &&
            shouldRetainPendingSeekPosition(
                targetPositionMillis = pendingSeekPosition,
                reportedPositionMillis = snapshot.positionMillis,
                elapsedSinceRequestMillis = elapsedRealtime() - pendingSeekRequestedAtMillis,
            )
        if (pendingSeekPosition != null && !retainsPendingSeek) pendingSeekPositionMillis = null
        val updatedState = if (retainsPendingSeek) {
            snapshot.copy(positionMillis = pendingSeekPosition)
        } else {
            snapshot
        }
        if (updatedState.status == PlaybackStatus.Playing) failureRecovery.onPlaybackStarted()
        mutableState.value = updatedState
        historyRecorder.sample(
            isPlaying = updatedState.status == PlaybackStatus.Playing,
            endedNaturally = updatedState.status == PlaybackStatus.Ended,
        )
    }

    private fun startPositionUpdates() {
        positionUpdates?.cancel()
        positionUpdates = scope.launch {
            while (isActive) {
                delay(POSITION_UPDATE_INTERVAL_MILLIS)
                controller?.let { player ->
                    syncPlayerState(player)
                    handleVipPreviewBoundary(player)
                    preloadCoordinator.maybePreload(player, mutableState.value, queue, loadGeneration)
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
        val restoredState = sessionCoordinator.load(mutableState.value.playbackSpeed) ?: return
        if (hasPlaybackMutation || queue.currentItem != null) return
        queue.replace(restoredState.queue, restoredState.currentIndex)
        mutableState.value = restoredState
        lastPositionCheckpointAtMillis = elapsedRealtime()
    }

    private fun requestPersistSession(positionMillis: Long = mutableState.value.positionMillis) {
        sessionCoordinator.persist(queue, mutableState.value, positionMillis)
    }

    private fun requestClearSession() {
        sessionCoordinator.clear()
    }

    private companion object {
        const val POSITION_UPDATE_INTERVAL_MILLIS = 500L
        const val POSITION_CHECKPOINT_INTERVAL_MILLIS = 5_000L
        const val AUTOMATIC_SKIP_DELAY_MILLIS = 3_000L
        const val MAX_CONSECUTIVE_AUTOMATIC_SKIPS = 5
    }

    private enum class FailureBehavior {
        RejectWithoutQueueMutation,
        RefreshCurrentSource,
        SkipQueueItem,
    }
}
