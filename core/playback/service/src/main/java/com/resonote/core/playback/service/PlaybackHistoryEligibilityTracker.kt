package com.resonote.core.playback.service

import com.resonote.core.data.ListeningHistoryRepository
import com.resonote.core.model.DeviceHistoryRecord
import com.resonote.core.model.DeviceHistorySource
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackOrigin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal sealed interface PlaybackHistoryTarget {
    data class Device(val record: DeviceHistoryRecord) : PlaybackHistoryTarget
    data class Account(val albumAudioId: String) : PlaybackHistoryTarget
}

internal data class PlaybackHistoryQualification(val sessionId: Long, val target: PlaybackHistoryTarget)

internal class PlaybackHistoryRecorder(
    private val repository: ListeningHistoryRepository,
    private val scope: CoroutineScope,
    private val elapsedRealtime: () -> Long,
    private val eligibility: PlaybackHistoryEligibilityTracker = PlaybackHistoryEligibilityTracker(),
) {
    fun start(target: PlaybackHistoryTarget?, durationMillis: Long) {
        eligibility.start(target, durationMillis, elapsedRealtime())
    }

    fun reset() {
        eligibility.reset()
    }

    fun sample(isPlaying: Boolean, endedNaturally: Boolean) {
        val qualification = eligibility.sample(
            isPlaying = isPlaying,
            endedNaturally = endedNaturally,
            elapsedRealtimeMillis = elapsedRealtime(),
        ) ?: return
        scope.launch {
            val persisted = when (val target = qualification.target) {
                is PlaybackHistoryTarget.Device -> repository.recordDevicePlayback(target.record)
                is PlaybackHistoryTarget.Account -> repository.recordAccountPlayback(target.albumAudioId)
            }
            eligibility.onPersistenceResult(qualification, persisted)
        }
    }
}

internal class PlaybackHistoryEligibilityTracker(
    private val thresholdMillis: Long = HISTORY_THRESHOLD_MILLIS,
    private val shortCompletionToleranceMillis: Long = SHORT_COMPLETION_TOLERANCE_MILLIS,
) {
    private var sessionId = 0L
    private var target: PlaybackHistoryTarget? = null
    private var durationMillis = 0L
    private var activePlaybackMillis = 0L
    private var lastSampleMillis: Long? = null
    private var wasPlaying = false
    private var eligible = false
    private var persistencePending = false
    private var persisted = false

    fun start(target: PlaybackHistoryTarget?, durationMillis: Long, elapsedRealtimeMillis: Long) {
        reset()
        this.target = target
        this.durationMillis = durationMillis.coerceAtLeast(0)
        lastSampleMillis = elapsedRealtimeMillis
    }

    fun reset() {
        sessionId += 1
        target = null
        durationMillis = 0
        activePlaybackMillis = 0
        lastSampleMillis = null
        wasPlaying = false
        eligible = false
        persistencePending = false
        persisted = false
    }

    fun sample(
        isPlaying: Boolean,
        endedNaturally: Boolean,
        elapsedRealtimeMillis: Long,
    ): PlaybackHistoryQualification? {
        val previousSample = lastSampleMillis
        if (wasPlaying && previousSample != null) {
            activePlaybackMillis += (elapsedRealtimeMillis - previousSample).coerceAtLeast(0)
        }
        lastSampleMillis = elapsedRealtimeMillis
        wasPlaying = isPlaying
        eligible =
            eligible ||
            (target is PlaybackHistoryTarget.Account && isPlaying) ||
            activePlaybackMillis >= thresholdMillis ||
            completedShortTrack(endedNaturally)
        val currentTarget = target
        if (!eligible || currentTarget == null || persistencePending || persisted) return null
        persistencePending = true
        return PlaybackHistoryQualification(sessionId, currentTarget)
    }

    fun onPersistenceResult(qualification: PlaybackHistoryQualification, success: Boolean) {
        if (qualification.sessionId != sessionId || qualification.target != target) return
        persistencePending = false
        persisted = success
    }

    private fun completedShortTrack(endedNaturally: Boolean): Boolean = endedNaturally &&
        durationMillis in 1 until thresholdMillis &&
        activePlaybackMillis + shortCompletionToleranceMillis >= durationMillis

    private companion object {
        const val HISTORY_THRESHOLD_MILLIS = 10_000L
        const val SHORT_COMPLETION_TOLERANCE_MILLIS = 750L
    }
}

internal fun PlaybackItem.toHistoryTargetOrNull(): PlaybackHistoryTarget? = when (val value = origin) {
    is PlaybackOrigin.Local ->
        PlaybackHistoryTarget.Device(
            DeviceHistoryRecord(
                source = DeviceHistorySource.Local,
                mediaId = value.id.value,
                title = metadata.title,
                artist = metadata.artist,
                albumTitle = metadata.albumTitle,
                artworkUri = metadata.artworkUri,
                durationMillis = metadata.durationMillis,
                albumAudioId = null,
            ),
        )
    is PlaybackOrigin.Cloud ->
        PlaybackHistoryTarget.Device(
            DeviceHistoryRecord(
                source = DeviceHistorySource.Cloud,
                mediaId = value.track.hash,
                title = metadata.title,
                artist = metadata.artist,
                albumTitle = metadata.albumTitle,
                artworkUri = metadata.artworkUri,
                durationMillis = metadata.durationMillis,
                albumAudioId = value.track.albumAudioId,
            ),
        )
    is PlaybackOrigin.Online ->
        value.song.albumAudioId
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let(PlaybackHistoryTarget::Account)
}
