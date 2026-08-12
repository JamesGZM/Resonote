package com.resonote.core.data

import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.network.model.NetworkSong

internal fun NetworkSong.toOnlineSong(): OnlineSong =
    OnlineSong(
        hash = hash,
        title = title,
        artist = artist,
        coverUrl = coverUrl?.replace("{size}", "480"),
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
    )
