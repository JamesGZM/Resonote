package com.resonote.core.datastore

import kotlinx.coroutines.flow.Flow

interface PlaybackPreferencesStorage {
    val playbackSpeedPercent: Flow<Int>
    val onlinePlaybackQuality: Flow<String>

    suspend fun setPlaybackSpeedPercent(percent: Int)
    suspend fun setOnlinePlaybackQuality(quality: String)
}
