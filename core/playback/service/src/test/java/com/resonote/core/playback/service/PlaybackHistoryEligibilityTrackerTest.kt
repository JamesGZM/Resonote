package com.resonote.core.playback.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.DeviceHistoryRecord
import com.resonote.core.model.DeviceHistorySource
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaId
import com.resonote.core.model.OnlineSong
import com.resonote.core.playback.PlaybackItem
import org.junit.Test

class PlaybackHistoryEligibilityTrackerTest {
    @Test
    fun continuousPlaybackQualifiesAtTenSecondsOnlyOnce() {
        val tracker = PlaybackHistoryEligibilityTracker()
        tracker.start(localRecord(), durationMillis = 60_000, elapsedRealtimeMillis = 0)

        assertThat(tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 0)).isNull()
        val qualification = tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 10_000)

        assertThat(qualification?.record).isEqualTo(localRecord())
        tracker.onPersistenceResult(requireNotNull(qualification), success = true)
        assertThat(tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 20_000)).isNull()
    }

    @Test
    fun pausedAndBufferedTimeDoesNotCountTowardThreshold() {
        val tracker = PlaybackHistoryEligibilityTracker()
        tracker.start(localRecord(), durationMillis = 60_000, elapsedRealtimeMillis = 0)

        tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 0)
        tracker.sample(isPlaying = false, endedNaturally = false, elapsedRealtimeMillis = 6_000)
        assertThat(tracker.sample(isPlaying = false, endedNaturally = false, elapsedRealtimeMillis = 20_000)).isNull()
        tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 20_000)

        assertThat(tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 24_000)).isNotNull()
    }

    @Test
    fun seekToEndDoesNotQualifyShortTrack() {
        val tracker = PlaybackHistoryEligibilityTracker()
        tracker.start(localRecord(), durationMillis = 8_000, elapsedRealtimeMillis = 0)
        tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 0)

        val qualification =
            tracker.sample(isPlaying = false, endedNaturally = true, elapsedRealtimeMillis = 500)

        assertThat(qualification).isNull()
    }

    @Test
    fun naturallyCompletedShortTrackQualifiesWithCallbackTolerance() {
        val tracker = PlaybackHistoryEligibilityTracker()
        tracker.start(localRecord(), durationMillis = 8_000, elapsedRealtimeMillis = 0)
        tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 0)

        val qualification =
            tracker.sample(isPlaying = false, endedNaturally = true, elapsedRealtimeMillis = 7_500)

        assertThat(qualification?.record).isEqualTo(localRecord())
    }

    @Test
    fun failedPersistenceCanRetryButLateResultCannotAffectNewSession() {
        val tracker = PlaybackHistoryEligibilityTracker()
        tracker.start(localRecord("first"), durationMillis = 60_000, elapsedRealtimeMillis = 0)
        tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 0)
        val first = requireNotNull(
            tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 10_000),
        )
        tracker.onPersistenceResult(first, success = false)
        assertThat(tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 10_001)).isNotNull()

        tracker.start(localRecord("second"), durationMillis = 60_000, elapsedRealtimeMillis = 20_000)
        tracker.onPersistenceResult(first, success = true)
        tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 20_000)

        assertThat(tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 30_000)?.record)
            .isEqualTo(localRecord("second"))
    }

    @Test
    fun onlyLocalAndCloudPlaybackProduceDeviceHistoryRecords() {
        val local = PlaybackItem(localMedia()).toDeviceHistoryRecordOrNull()
        val cloud = PlaybackItem(cloudTrack()).toDeviceHistoryRecordOrNull()
        val online = PlaybackItem(onlineSong()).toDeviceHistoryRecordOrNull()

        assertThat(local?.source).isEqualTo(DeviceHistorySource.Local)
        assertThat(local?.mediaId).isEqualTo("local-id")
        assertThat(cloud?.source).isEqualTo(DeviceHistorySource.Cloud)
        assertThat(cloud?.mediaId).isEqualTo("cloud-hash")
        assertThat(cloud?.albumAudioId).isEqualTo("cloud-audio")
        assertThat(online).isNull()
    }

    private companion object {
        fun localRecord(id: String = "local-id") = DeviceHistoryRecord(
            source = DeviceHistorySource.Local,
            mediaId = id,
            title = id,
            artist = "Artist",
            albumTitle = null,
            artworkUri = null,
            durationMillis = 60_000,
            albumAudioId = null,
        )

        fun localMedia() = LocalMedia(
            id = LocalMediaId("local-id"),
            displayName = "local.flac",
            title = "Local song",
            artist = "Artist",
            albumTitle = null,
            artworkUri = null,
            durationMillis = 60_000,
            mimeType = "audio/flac",
            fileExtension = "flac",
            sizeBytes = 4_096,
            sampleRateHz = 96_000,
            bitDepth = 24,
            bitrateBitsPerSecond = 2_304_000,
            importedAtEpochMillis = 1_000,
        )

        fun cloudTrack() = CloudTrack(
            hash = "cloud-hash",
            title = "Cloud song",
            artist = "Artist",
            album = "Album",
            coverUrl = null,
            durationMillis = 60_000,
            albumAudioId = "cloud-audio",
        )

        fun onlineSong() = OnlineSong(
            hash = "online-hash",
            title = "Online song",
            artist = "Artist",
            coverUrl = null,
            albumId = null,
            albumAudioId = null,
            durationMillis = 60_000,
            quality = AudioQuality.Standard,
            vip = false,
        )
    }
}
