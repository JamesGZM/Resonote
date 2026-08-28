package com.resonote.core.karaoke.service

import com.resonote.core.data.CloudRepository
import com.resonote.core.data.KaraokePreparationFailure
import com.resonote.core.data.KaraokePreparationRequest
import com.resonote.core.data.KaraokeRecordingFileResult
import com.resonote.core.data.KaraokeRepository
import com.resonote.core.data.LocalMediaRepository
import com.resonote.core.data.PrepareKaraokeResult
import com.resonote.core.data.PreparedKaraokeProject
import com.resonote.core.data.SongPlaybackRepository
import com.resonote.core.karaoke.KaraokeSessionFailure
import com.resonote.core.karaoke.KaraokeSessionState
import com.resonote.core.karaoke.KaraokeSessionStatus
import com.resonote.core.model.KaraokeProjectId
import com.resonote.core.model.KaraokeSourceMode
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.PlaybackController
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackOrigin
import com.resonote.core.playback.PlaybackStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class KaraokeSessionRuntime @Inject constructor(
    private val repository: KaraokeRepository,
    private val playback: PlaybackController,
    private val songPlaybackRepository: SongPlaybackRepository,
    private val cloudRepository: CloudRepository,
    private val localMediaRepository: LocalMediaRepository,
    private val recordingEngine: KaraokeRecordingEngine,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val commandMutex = Mutex()
    private val mutableState = MutableStateFlow(KaraokeSessionState())
    val state: StateFlow<KaraokeSessionState> = mutableState.asStateFlow()

    private val originalItems = linkedMapOf<String, PlaybackItem>()
    private var preparedProject: PreparedKaraokeProject? = null
    private var activeProjectId: KaraokeProjectId? = null
    private var activeItemKey: String? = null
    private var activeSegment: ActiveSegment? = null
    private var elapsedJob: Job? = null
    private var preparingJob: Job? = null

    init {
        scope.launch {
            playback.state
                .map { it.currentItem?.queueKey to it.status }
                .distinctUntilChanged()
                .collectLatest { (queueKey, status) ->
                    if (!mutableState.value.enabled) return@collectLatest
                    if (queueKey != null && activeItemKey != null && queueKey != activeItemKey) {
                        val autoStart = mutableState.value.continuousRecordingArmed
                        playback.state.value.currentItem?.let { prepareSong(it, autoStart) }
                    } else if (queueKey == activeItemKey) {
                        if (mutableState.value.sourceChangeInProgress) return@collectLatest
                        when {
                            status == PlaybackStatus.Paused &&
                                mutableState.value.status is KaraokeSessionStatus.Recording -> pauseFromPlayback()
                            status == PlaybackStatus.Playing &&
                                mutableState.value.status is KaraokeSessionStatus.Paused -> resumeFromPlayback()
                        }
                    }
                }
        }
    }

    fun enable(item: PlaybackItem) {
        if (mutableState.value.enabled) return
        originalItems.putIfAbsent(item.queueKey, item)
        mutableState.value = KaraokeSessionState(enabled = true, status = KaraokeSessionStatus.Preparing)
        prepareSong(item, autoStart = false)
    }

    fun disable() {
        scope.launch {
            commandMutex.withLock {
                mutableState.value = mutableState.value.copy(savingInProgress = true)
                preparingJob?.cancel()
                stopCapture()
                cleanupEmptyProject()
                restoreOriginalQueue()
                originalItems.clear()
                reset(enabled = false)
            }
        }
    }

    fun start() {
        if (mutableState.value.status is KaraokeSessionStatus.Preparing) return
        if (preparedProject == null) {
            playback.state.value.currentItem?.let { prepareSong(it, autoStart = false) }
            return
        }
        mutableState.value = mutableState.value.copy(continuousRecordingArmed = true, failure = null)
        scope.launch { commandMutex.withLock { countdownAndStart() } }
    }

    fun selectSource(sourceMode: KaraokeSourceMode) {
        if (sourceMode == KaraokeSourceMode.Mixed || sourceMode !in mutableState.value.availableSourceModes) return
        if (sourceMode == mutableState.value.selectedSourceMode || mutableState.value.sourceChangeInProgress) return
        scope.launch { commandMutex.withLock { switchSource(sourceMode) } }
    }

    fun pause() = playback.pause()

    fun resume() {
        if (playback.state.value.status == PlaybackStatus.Paused) playback.togglePlayPause()
    }

    fun previous() = changeSong { playback.previous() }

    fun next() = changeSong { playback.next() }

    fun stopAndSave() {
        scope.launch {
            commandMutex.withLock {
                mutableState.value = mutableState.value.copy(savingInProgress = true)
                preparingJob?.cancel()
                stopCapture()
                cleanupEmptyProject()
                mutableState.value = mutableState.value.copy(
                    continuousRecordingArmed = false,
                    savingInProgress = false,
                    status = KaraokeSessionStatus.Off,
                )
                playback.state.value.currentItem?.let { current ->
                    prepareSong(originalItems[current.queueKey] ?: current, autoStart = false)
                }
            }
        }
    }

    fun acknowledgeFailure() {
        mutableState.value = mutableState.value.copy(
            failure = null,
            status = if (mutableState.value.status is KaraokeSessionStatus.Failed) {
                KaraokeSessionStatus.Off
            } else {
                mutableState.value.status
            },
        )
    }

    private fun changeSong(changePlaybackItem: () -> Unit) {
        mutableState.value = mutableState.value.copy(savingInProgress = true)
        scope.launch {
            commandMutex.withLock {
                preparingJob?.cancel()
                stopCapture()
                cleanupEmptyProject()
                preparedProject = null
                mutableState.value = mutableState.value.copy(savingInProgress = false)
            }
            changePlaybackItem()
            delay(150)
            if (preparedProject == null && preparingJob?.isActive != true) {
                playback.state.value.currentItem?.let { current ->
                    prepareSong(current, autoStart = mutableState.value.continuousRecordingArmed)
                }
            }
        }
    }

    private fun prepareSong(item: PlaybackItem, autoStart: Boolean) {
        preparingJob?.cancel()
        preparingJob = scope.launch {
            commandMutex.withLock {
                stopCapture()
                cleanupEmptyProject()
                val originalItem = originalItems.getOrPut(item.queueKey) { item }
                val originalSource = resolveOriginalSource(originalItem)
                    ?: return@withLock fail(KaraokeSessionFailure.SourceUnavailable)
                activeItemKey = item.queueKey
                mutableState.value = mutableState.value.copy(
                    status = KaraokeSessionStatus.Preparing,
                    availableSourceModes = emptySet(),
                    sourceChangeInProgress = false,
                    failure = null,
                )
                val metadata = originalItem.metadata
                val identity = when (val origin = originalItem.origin) {
                    is PlaybackOrigin.Online -> Triple(origin.song.hash, origin.song.albumAudioId, true)
                    is PlaybackOrigin.Cloud -> Triple(origin.track.hash, origin.track.albumAudioId, true)
                    is PlaybackOrigin.Local -> Triple(origin.id.value, null, false)
                }
                when (
                    val result = repository.prepareProject(
                        KaraokePreparationRequest(
                            songHash = identity.first,
                            songTitle = metadata.title,
                            artist = metadata.artist,
                            artworkUri = metadata.artworkUri,
                            durationMillis = metadata.durationMillis,
                            albumAudioId = identity.second,
                            originalSource = originalSource,
                            accompanimentLookupEnabled = identity.third,
                            timelineStartMillis = playback.state.value.positionMillis,
                        ),
                    )
                ) {
                    is PrepareKaraokeResult.Failed -> fail(result.reason.toSessionFailure())
                    is PrepareKaraokeResult.Ready -> {
                        preparedProject = result.value
                        activeProjectId = result.value.project.id
                        val selected = if (KaraokeSourceMode.Accompaniment in result.value.sources) {
                            KaraokeSourceMode.Accompaniment
                        } else {
                            KaraokeSourceMode.Original
                        }
                        val position = playback.state.value.positionMillis
                        val wasPlaying = playback.state.value.status == PlaybackStatus.Playing
                        val applied = applyPlaybackSource(originalItem, selected, position, wasPlaying)
                        if (!applied) return@withLock fail(KaraokeSessionFailure.SourceUnavailable)
                        mutableState.value = mutableState.value.copy(
                            status = KaraokeSessionStatus.Off,
                            availableSourceModes = result.value.sources.keys,
                            selectedSourceMode = selected,
                            sourceChangeInProgress = false,
                        )
                        if (autoStart) countdownAndStart()
                    }
                }
            }
        }
    }

    private suspend fun countdownAndStart() {
        val project = preparedProject ?: return
        val projectId = activeProjectId ?: return
        val startPosition = playback.state.value.positionMillis
        repository.setTrimStart(projectId, startPosition)
        playback.pause()
        for (remaining in COUNTDOWN_SECONDS downTo 1) {
            mutableState.value = mutableState.value.copy(status = KaraokeSessionStatus.Countdown(remaining))
            delay(1_000)
        }
        startCapture(
            sourceMode = mutableState.value.selectedSourceMode,
            expectedDurationMillis = (project.project.durationMillis - startPosition).coerceAtLeast(0L),
        )
        if (playback.state.value.status == PlaybackStatus.Paused) playback.togglePlayPause()
    }

    private suspend fun switchSource(target: KaraokeSourceMode) {
        val project = preparedProject ?: return
        val projectId = activeProjectId ?: return
        val originalItem = originalItems[activeItemKey] ?: return
        val previous = mutableState.value.selectedSourceMode
        val previousStatus = mutableState.value.status
        val wasRecording = previousStatus is KaraokeSessionStatus.Recording
        val wasPlaying = playback.state.value.status == PlaybackStatus.Playing
        val position = playback.state.value.positionMillis
        mutableState.value = mutableState.value.copy(sourceChangeInProgress = true, failure = null)
        playback.pause()
        if (wasRecording) stopCapture()
        val applied = applyPlaybackSource(originalItem, target, position, playWhenReady = false)
        val persisted = applied && repository.selectBackingSource(projectId, target, position)
        if (!persisted) {
            applyPlaybackSource(originalItem, previous, position, playWhenReady = false)
            mutableState.value = mutableState.value.copy(
                sourceChangeInProgress = false,
                failure = KaraokeSessionFailure.SourceUnavailable,
                status = previousStatus,
            )
        } else {
            mutableState.value = mutableState.value.copy(
                selectedSourceMode = target,
                sourceChangeInProgress = false,
                status = previousStatus,
            )
        }
        if (wasRecording) {
            startCapture(target.takeIf { persisted } ?: previous, project.project.durationMillis - position)
        }
        if (wasPlaying && playback.state.value.status == PlaybackStatus.Paused) playback.togglePlayPause()
    }

    private suspend fun applyPlaybackSource(
        originalItem: PlaybackItem,
        sourceMode: KaraokeSourceMode,
        positionMillis: Long,
        playWhenReady: Boolean,
    ): Boolean {
        val source = preparedProject?.sources?.get(sourceMode) ?: return false
        val resolved = ResolvedSongSource(
            uri = source.uri,
            durationMillis = source.durationMillis,
            extension = source.extension,
            cacheKey = "karaoke:${activeProjectId?.value}:${sourceMode.name}",
        )
        playback.replaceCurrentItem(originalItem.copy(resolvedSource = resolved), positionMillis, playWhenReady)
        return withTimeoutOrNull(SOURCE_SWITCH_TIMEOUT_MILLIS) {
            playback.state.first { state ->
                state.currentItem?.resolvedSource?.uri == source.uri &&
                    state.status !in setOf(PlaybackStatus.Resolving, PlaybackStatus.Buffering)
            }
        }?.status != PlaybackStatus.Failed
    }

    private suspend fun resolveOriginalSource(item: PlaybackItem): ResolvedSongSource? {
        item.resolvedSource?.let { return it }
        return when (val origin = item.origin) {
            is PlaybackOrigin.Online ->
                (
                    songPlaybackRepository.resolveSource(
                        origin.song,
                        item.onlineQualityOverride,
                    ) as? ResolveSongSourceResult.Resolved
                    )
                    ?.source
            is PlaybackOrigin.Cloud ->
                (cloudRepository.resolveSource(origin.track) as? ResolveSongSourceResult.Resolved)?.source
            is PlaybackOrigin.Local -> localMediaRepository.resolvePlaybackSource(origin.id)?.let { source ->
                ResolvedSongSource(
                    uri = source.uri,
                    durationMillis = source.media.durationMillis,
                    extension = source.media.fileExtension,
                )
            }
        }
    }

    private suspend fun startCapture(sourceMode: KaraokeSourceMode, expectedDurationMillis: Long) {
        val projectId = activeProjectId ?: return
        when (val result = repository.createRecordingFile(projectId, expectedDurationMillis)) {
            KaraokeRecordingFileResult.Failed -> fail(KaraokeSessionFailure.StorageUnavailable)
            KaraokeRecordingFileResult.InsufficientStorage -> fail(KaraokeSessionFailure.InsufficientStorage)
            is KaraokeRecordingFileResult.Ready -> {
                val timelineStart = playback.state.value.positionMillis
                val completion = CompletableDeferred<KaraokeCaptureSummary?>()
                val captureJob = scope.launch(Dispatchers.IO) {
                    completion.complete(recordingEngine.record(result.path))
                }
                activeSegment = ActiveSegment(
                    projectId = projectId,
                    segmentId = result.segmentId,
                    path = result.path,
                    timelineStartMillis = timelineStart,
                    captureJob = captureJob,
                    completion = completion,
                    sourceMode = sourceMode,
                )
                mutableState.value = mutableState.value.copy(
                    status = KaraokeSessionStatus.Recording(
                        projectId,
                        timelineStart,
                        sourceMode == KaraokeSourceMode.Accompaniment,
                    ),
                )
                elapsedJob?.cancel()
                elapsedJob = scope.launch {
                    while (true) {
                        delay(250)
                        val active = activeSegment ?: break
                        mutableState.value = mutableState.value.copy(
                            status = KaraokeSessionStatus.Recording(
                                active.projectId,
                                playback.state.value.positionMillis,
                                active.sourceMode == KaraokeSourceMode.Accompaniment,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun pauseFromPlayback() {
        scope.launch {
            commandMutex.withLock {
                val projectId = activeProjectId ?: return@withLock
                stopCapture()
                mutableState.value = mutableState.value.copy(status = KaraokeSessionStatus.Paused(projectId))
            }
        }
    }

    private fun resumeFromPlayback() {
        scope.launch {
            commandMutex.withLock {
                val project = preparedProject?.project ?: return@withLock
                startCapture(
                    mutableState.value.selectedSourceMode,
                    (project.durationMillis - playback.state.value.positionMillis).coerceAtLeast(0L),
                )
            }
        }
    }

    private suspend fun stopCapture() {
        val segment = activeSegment ?: return
        activeSegment = null
        elapsedJob?.cancel()
        recordingEngine.stop()
        segment.captureJob.join()
        val summary = segment.completion.await()
        val saved = if (summary == null) {
            File(segment.path).delete()
            true
        } else {
            repository.commitRecordingSegment(
                projectId = segment.projectId,
                segmentId = segment.segmentId,
                path = segment.path,
                timelineStartMillis = segment.timelineStartMillis,
                durationMillis = summary.durationMillis,
                peakAmplitude = summary.peakAmplitude,
            )
        }
        if (!saved) mutableState.value = mutableState.value.copy(failure = KaraokeSessionFailure.StorageUnavailable)
    }

    private suspend fun cleanupEmptyProject() {
        val projectId = activeProjectId ?: return
        val input = repository.renderInput(projectId)
        if (input == null || input.segments.isEmpty()) repository.deleteProjects(setOf(projectId))
        activeProjectId = null
        preparedProject = null
    }

    private fun restoreOriginalQueue() {
        val current = playback.state.value
        if (current.queue.isEmpty()) return
        val restored = current.queue.map { originalItems[it.queueKey] ?: it }
        val index = current.currentIndex.coerceIn(restored.indices)
        playback.replaceQueue(
            items = restored,
            startIndex = index,
            positionMillis = current.positionMillis,
            playWhenReady = current.status == PlaybackStatus.Playing,
        )
    }

    private fun fail(reason: KaraokeSessionFailure) {
        mutableState.value = mutableState.value.copy(
            continuousRecordingArmed = false,
            status = KaraokeSessionStatus.Failed(reason),
            sourceChangeInProgress = false,
            savingInProgress = false,
            failure = reason,
        )
    }

    private fun reset(enabled: Boolean) {
        preparedProject = null
        activeProjectId = null
        activeItemKey = null
        activeSegment = null
        mutableState.value = KaraokeSessionState(enabled = enabled)
    }

    private fun KaraokePreparationFailure.toSessionFailure() = when (this) {
        KaraokePreparationFailure.SourceUnavailable -> KaraokeSessionFailure.SourceUnavailable
        KaraokePreparationFailure.InsufficientStorage -> KaraokeSessionFailure.InsufficientStorage
        KaraokePreparationFailure.StorageUnavailable -> KaraokeSessionFailure.StorageUnavailable
    }

    private data class ActiveSegment(
        val projectId: KaraokeProjectId,
        val segmentId: String,
        val path: String,
        val timelineStartMillis: Long,
        val captureJob: Job,
        val completion: CompletableDeferred<KaraokeCaptureSummary?>,
        val sourceMode: KaraokeSourceMode,
    )

    private companion object {
        const val COUNTDOWN_SECONDS = 3
        const val SOURCE_SWITCH_TIMEOUT_MILLIS = 8_000L
    }
}
