package com.resonote.core.playback

import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.DeviceHistoryRecord
import com.resonote.core.model.DeviceHistorySource
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaId
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.model.PlaybackUnavailableReason
import com.resonote.core.model.ResolvedSongSource
import kotlinx.coroutines.flow.StateFlow

sealed interface PlaybackOrigin {
    data class Online(val song: OnlineSong) : PlaybackOrigin

    data class Cloud(val track: CloudTrack) : PlaybackOrigin

    data class Local(val id: LocalMediaId) : PlaybackOrigin
}

sealed interface PlaybackFormat {
    data class Online(val quality: AudioQuality) : PlaybackFormat

    data class Cloud(val extension: String?) : PlaybackFormat

    data class Local(
        val mimeType: String?,
        val extension: String?,
        val sampleRateHz: Int?,
        val bitDepth: Int?,
        val bitrateBitsPerSecond: Int?,
    ) : PlaybackFormat
}

data class PlaybackMetadata(
    val mediaId: String,
    val title: String,
    val artist: String?,
    val albumTitle: String?,
    val artworkUri: String?,
    val durationMillis: Long,
    val format: PlaybackFormat,
    val isVip: Boolean,
) {
    init {
        require(mediaId.isNotBlank()) { "mediaId must not be blank" }
        require(title.isNotBlank()) { "title must not be blank" }
        require(durationMillis >= 0) { "durationMillis must not be negative" }
    }
}

data class PlaybackItem(
    val metadata: PlaybackMetadata,
    val origin: PlaybackOrigin,
    val resolvedSource: ResolvedSongSource? = null,
) {
    constructor(
        song: OnlineSong,
        resolvedSource: ResolvedSongSource? = null,
    ) : this(
        metadata = song.toPlaybackMetadata(),
        origin = PlaybackOrigin.Online(song),
        resolvedSource = resolvedSource,
    )

    constructor(
        track: CloudTrack,
        resolvedSource: ResolvedSongSource? = null,
    ) : this(
        metadata = track.toPlaybackMetadata(resolvedSource?.extension),
        origin = PlaybackOrigin.Cloud(track),
        resolvedSource = resolvedSource,
    )

    constructor(media: LocalMedia) : this(
        metadata = media.toPlaybackMetadata(),
        origin = PlaybackOrigin.Local(media.id),
    )

    constructor(record: DeviceHistoryRecord) : this(
        metadata = record.toPlaybackMetadata(),
        origin = when (record.source) {
            DeviceHistorySource.Local -> PlaybackOrigin.Local(LocalMediaId(record.mediaId))
            DeviceHistorySource.Cloud -> PlaybackOrigin.Cloud(record.toCloudTrack())
        },
    )

    val queueKey: String
        get() = when (val value = origin) {
            is PlaybackOrigin.Online -> "online:${value.song.hash}"
            is PlaybackOrigin.Cloud -> "cloud:${value.track.hash}"
            is PlaybackOrigin.Local -> "local:${value.id.value}"
        }

    fun withResolvedSource(source: ResolvedSongSource): PlaybackItem = copy(
        metadata = when (metadata.format) {
            is PlaybackFormat.Cloud -> metadata.copy(format = PlaybackFormat.Cloud(source.extension))
            else -> metadata
        },
        resolvedSource = source,
    )
}

enum class PlaybackStatus {
    Idle,
    Resolving,
    Buffering,
    Playing,
    Paused,
    Ended,
    Failed,
}

enum class PlaybackMode {
    ListLoop,
    Shuffle,
    SingleLoop,
    Sequential,
}

sealed interface PlaybackIssue {
    data class Unavailable(val reason: PlaybackUnavailableReason) : PlaybackIssue

    data class SourceFailure(val failure: ContentFailure) : PlaybackIssue

    data class PlayerFailure(val message: String?) : PlaybackIssue
}

data class PlaybackState(
    val queue: List<PlaybackItem> = emptyList(),
    val currentIndex: Int = -1,
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val positionMillis: Long = 0,
    val durationMillis: Long = 0,
    val bufferedPositionMillis: Long = 0,
    val mode: PlaybackMode = PlaybackMode.ListLoop,
    val playbackSpeed: PlaybackSpeed = PlaybackSpeed.Normal,
    val issue: PlaybackIssue? = null,
) {
    val currentItem: PlaybackItem?
        get() = queue.getOrNull(currentIndex)

    val currentMetadata: PlaybackMetadata?
        get() = currentItem?.metadata

    val isPlaying: Boolean
        get() = status == PlaybackStatus.Playing

    val progress: Float
        get() = if (durationMillis > 0) {
            (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
        } else {
            0f
        }
}

interface PlaybackController {
    val state: StateFlow<PlaybackState>

    fun play(item: PlaybackItem)

    fun playAll(items: List<PlaybackItem>, startIndex: Int = 0)

    fun playNext(items: List<PlaybackItem>)

    fun append(items: List<PlaybackItem>)

    fun selectQueueItem(index: Int)

    fun removeQueueItem(index: Int)

    fun moveQueueItem(fromIndex: Int, toIndex: Int)

    fun togglePlayPause()

    fun pause()

    fun next()

    fun previous()

    fun seekTo(positionMillis: Long)

    fun setMode(mode: PlaybackMode)

    fun setPlaybackSpeed(speed: PlaybackSpeed)

    fun refreshCurrentOnlineSource(force: Boolean = false)

    fun clear()
}

private fun OnlineSong.toPlaybackMetadata() = PlaybackMetadata(
    mediaId = hash,
    title = title,
    artist = artist,
    albumTitle = albumTitle,
    artworkUri = coverUrl,
    durationMillis = durationMillis,
    format = PlaybackFormat.Online(quality),
    isVip = vip,
)

private fun CloudTrack.toPlaybackMetadata(extension: String?) = PlaybackMetadata(
    mediaId = hash,
    title = title,
    artist = artist,
    albumTitle = album,
    artworkUri = coverUrl,
    durationMillis = durationMillis,
    format = PlaybackFormat.Cloud(extension),
    isVip = false,
)

private fun LocalMedia.toPlaybackMetadata() = PlaybackMetadata(
    mediaId = id.value,
    title = title,
    artist = artist,
    albumTitle = albumTitle,
    artworkUri = artworkUri,
    durationMillis = durationMillis,
    format = PlaybackFormat.Local(
        mimeType = mimeType,
        extension = fileExtension,
        sampleRateHz = sampleRateHz,
        bitDepth = bitDepth,
        bitrateBitsPerSecond = bitrateBitsPerSecond,
    ),
    isVip = false,
)

private fun DeviceHistoryRecord.toPlaybackMetadata() = PlaybackMetadata(
    mediaId = mediaId,
    title = title,
    artist = artist,
    albumTitle = albumTitle,
    artworkUri = artworkUri,
    durationMillis = durationMillis,
    format = when (source) {
        DeviceHistorySource.Local -> PlaybackFormat.Local(
            mimeType = null,
            extension = null,
            sampleRateHz = null,
            bitDepth = null,
            bitrateBitsPerSecond = null,
        )
        DeviceHistorySource.Cloud -> PlaybackFormat.Cloud(extension = null)
    },
    isVip = false,
)

private fun DeviceHistoryRecord.toCloudTrack() = CloudTrack(
    hash = mediaId,
    title = title,
    artist = artist,
    album = albumTitle,
    coverUrl = artworkUri,
    durationMillis = durationMillis,
    albumAudioId = albumAudioId,
)
