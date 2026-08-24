package com.resonote.core.playback.service

import com.resonote.core.data.PlaybackSessionEntry
import com.resonote.core.data.PlaybackSessionEntryKind
import com.resonote.core.data.PlaybackSessionSnapshot
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.LocalMediaId
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.playback.PlaybackFormat
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackMetadata
import com.resonote.core.playback.PlaybackOrigin
import com.resonote.core.playback.PlaybackState
import com.resonote.core.playback.PlaybackStatus
import javax.inject.Singleton

@Singleton
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
