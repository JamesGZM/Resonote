package com.resonote.core.playback.service

import com.resonote.core.model.DeviceHistoryRecord
import com.resonote.core.model.DeviceHistorySource
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackOrigin

internal data class PlaybackHistoryQualification(val sessionId: Long, val record: DeviceHistoryRecord)

internal class PlaybackHistoryEligibilityTracker(
    private val thresholdMillis: Long = HISTORY_THRESHOLD_MILLIS,
    private val shortCompletionToleranceMillis: Long = SHORT_COMPLETION_TOLERANCE_MILLIS,
) {
    private var sessionId = 0L
    private var record: DeviceHistoryRecord? = null
    private var durationMillis = 0L
    private var activePlaybackMillis = 0L
    private var lastSampleMillis: Long? = null
    private var wasPlaying = false
    private var eligible = false
    private var persistencePending = false
    private var persisted = false

    fun start(record: DeviceHistoryRecord?, durationMillis: Long, elapsedRealtimeMillis: Long) {
        reset()
        this.record = record
        this.durationMillis = durationMillis.coerceAtLeast(0)
        lastSampleMillis = elapsedRealtimeMillis
    }

    fun reset() {
        sessionId += 1
        record = null
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
            activePlaybackMillis >= thresholdMillis ||
            completedShortTrack(endedNaturally)
        val currentRecord = record
        if (!eligible || currentRecord == null || persistencePending || persisted) return null
        persistencePending = true
        return PlaybackHistoryQualification(sessionId, currentRecord)
    }

    fun onPersistenceResult(qualification: PlaybackHistoryQualification, success: Boolean) {
        if (qualification.sessionId != sessionId || qualification.record != record) return
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

internal fun PlaybackItem.toDeviceHistoryRecordOrNull(): DeviceHistoryRecord? = when (val value = origin) {
    is PlaybackOrigin.Local ->
        DeviceHistoryRecord(
            source = DeviceHistorySource.Local,
            mediaId = value.id.value,
            title = metadata.title,
            artist = metadata.artist,
            albumTitle = metadata.albumTitle,
            artworkUri = metadata.artworkUri,
            durationMillis = metadata.durationMillis,
            albumAudioId = null,
        )
    is PlaybackOrigin.Cloud ->
        DeviceHistoryRecord(
            source = DeviceHistorySource.Cloud,
            mediaId = value.track.hash,
            title = metadata.title,
            artist = metadata.artist,
            albumTitle = metadata.albumTitle,
            artworkUri = metadata.artworkUri,
            durationMillis = metadata.durationMillis,
            albumAudioId = value.track.albumAudioId,
        )
    is PlaybackOrigin.Online -> null
}
