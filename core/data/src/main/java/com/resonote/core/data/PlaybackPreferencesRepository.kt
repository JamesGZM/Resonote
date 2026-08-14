package com.resonote.core.data

import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PlaybackSpeed
import kotlinx.coroutines.flow.Flow

interface PlaybackPreferencesRepository {
    val playbackSpeed: Flow<PlaybackSpeed>
    val onlinePlaybackQuality: Flow<OnlinePlaybackQuality>

    suspend fun setPlaybackSpeed(speed: PlaybackSpeed)
    suspend fun setOnlinePlaybackQuality(quality: OnlinePlaybackQuality)
}
