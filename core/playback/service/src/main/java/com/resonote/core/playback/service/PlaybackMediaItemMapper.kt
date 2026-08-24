@file:androidx.media3.common.util.UnstableApi

package com.resonote.core.playback.service

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.PlaybackItem

internal fun PlaybackItem.toMediaItem(source: ResolvedSongSource): MediaItem {
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
        .apply { source.cacheKey?.let(::setCustomCacheKey) }
        .setMediaMetadata(mediaMetadata)
        .build()
}
