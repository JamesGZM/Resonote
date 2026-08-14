package com.resonote.core.data

import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.network.model.NetworkSong

internal fun NetworkSong.toOnlineSong(): OnlineSong =
    OnlineSong(
        hash = hash,
        title = title,
        artist = artist,
        coverUrl = coverUrl.toRemoteImageUrl(480),
        albumId = albumId,
        albumAudioId = albumAudioId,
        durationMillis = durationMillis,
        quality =
            when {
                losslessAvailable || !losslessHash.isNullOrBlank() -> AudioQuality.Lossless
                highQualityAvailable || !highQualityHash.isNullOrBlank() -> AudioQuality.HighQuality
                else -> AudioQuality.Standard
            },
        vip = vip,
        albumTitle = albumTitle,
        fileId = fileId,
        previewDurationMillis = previewDurationMillis,
    )

internal fun String?.toRemoteImageUrl(size: Int): String? {
    val value = this?.trim()?.takeIf(String::isNotEmpty)?.replace("{size}", size.toString()) ?: return null
    return when {
        value.startsWith("//") -> "https:$value"
        value.startsWith("http://") -> "https://${value.removePrefix("http://")}"
        else -> value
    }
}
