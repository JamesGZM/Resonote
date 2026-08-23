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
        tracker.start(deviceTarget(), durationMillis = 60_000, elapsedRealtimeMillis = 0)

        assertThat(tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 0)).isNull()
        val qualification = tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 10_000)

        assertThat(qualification?.target).isEqualTo(deviceTarget())
        tracker.onPersistenceResult(requireNotNull(qualification), success = true)
        assertThat(tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 20_000)).isNull()
    }

    @Test
    fun onlinePlaybackQualifiesAsSoonAsPlaybackStarts() {
        val tracker = PlaybackHistoryEligibilityTracker()
        val target = PlaybackHistoryTarget.Account("32155307")
        tracker.start(target, durationMillis = 60_000, elapsedRealtimeMillis = 0)

        val qualification = tracker.sample(
            isPlaying = true,
            endedNaturally = false,
            elapsedRealtimeMillis = 0,
        )

        assertThat(qualification?.target).isEqualTo(target)
    }

    @Test
    fun pausedAndBufferedTimeDoesNotCountTowardThreshold() {
        val tracker = PlaybackHistoryEligibilityTracker()
        tracker.start(deviceTarget(), durationMillis = 60_000, elapsedRealtimeMillis = 0)

        tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 0)
        tracker.sample(isPlaying = false, endedNaturally = false, elapsedRealtimeMillis = 6_000)
        assertThat(tracker.sample(isPlaying = false, endedNaturally = false, elapsedRealtimeMillis = 20_000)).isNull()
        tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 20_000)

        assertThat(tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 24_000)).isNotNull()
    }

    @Test
    fun seekToEndDoesNotQualifyShortTrack() {
        val tracker = PlaybackHistoryEligibilityTracker()
        tracker.start(deviceTarget(), durationMillis = 8_000, elapsedRealtimeMillis = 0)
        tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 0)

        val qualification =
            tracker.sample(isPlaying = false, endedNaturally = true, elapsedRealtimeMillis = 500)

        assertThat(qualification).isNull()
    }

    @Test
    fun naturallyCompletedShortTrackQualifiesWithCallbackTolerance() {
        val tracker = PlaybackHistoryEligibilityTracker()
        tracker.start(deviceTarget(), durationMillis = 8_000, elapsedRealtimeMillis = 0)
        tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 0)

        val qualification =
            tracker.sample(isPlaying = false, endedNaturally = true, elapsedRealtimeMillis = 7_500)

        assertThat(qualification?.target).isEqualTo(deviceTarget())
    }

    @Test
    fun failedPersistenceCanRetryButLateResultCannotAffectNewSession() {
        val tracker = PlaybackHistoryEligibilityTracker()
        tracker.start(deviceTarget("first"), durationMillis = 60_000, elapsedRealtimeMillis = 0)
        tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 0)
        val first = requireNotNull(
            tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 10_000),
        )
        tracker.onPersistenceResult(first, success = false)
        assertThat(tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 10_001)).isNotNull()

        tracker.start(deviceTarget("second"), durationMillis = 60_000, elapsedRealtimeMillis = 20_000)
        tracker.onPersistenceResult(first, success = true)
        tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 20_000)

        assertThat(tracker.sample(isPlaying = true, endedNaturally = false, elapsedRealtimeMillis = 30_000)?.target)
            .isEqualTo(deviceTarget("second"))
    }

    @Test
    fun playbackMapsLocalAndCloudToDeviceHistoryAndOnlineToAccountHistory() {
        val local = PlaybackItem(localMedia()).toHistoryTargetOrNull() as PlaybackHistoryTarget.Device
        val cloud = PlaybackItem(cloudTrack()).toHistoryTargetOrNull() as PlaybackHistoryTarget.Device
        val online = PlaybackItem(onlineSong()).toHistoryTargetOrNull()

        assertThat(local.record.source).isEqualTo(DeviceHistorySource.Local)
        assertThat(local.record.mediaId).isEqualTo("local-id")
        assertThat(cloud.record.source).isEqualTo(DeviceHistorySource.Cloud)
        assertThat(cloud.record.mediaId).isEqualTo("cloud-hash")
        assertThat(cloud.record.albumAudioId).isEqualTo("cloud-audio")
        assertThat(online).isEqualTo(PlaybackHistoryTarget.Account("32155307"))
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

        fun deviceTarget(id: String = "local-id") = PlaybackHistoryTarget.Device(localRecord(id))

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
            albumAudioId = "32155307",
            durationMillis = 60_000,
            quality = AudioQuality.Standard,
            vip = false,
        )
    }
}
