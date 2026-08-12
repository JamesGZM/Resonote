package com.resonote.core.datastore

import kotlinx.coroutines.flow.Flow

interface PlaybackPreferencesStorage {
    val playbackSpeedPercent: Flow<Int>

    suspend fun setPlaybackSpeedPercent(percent: Int)
}
