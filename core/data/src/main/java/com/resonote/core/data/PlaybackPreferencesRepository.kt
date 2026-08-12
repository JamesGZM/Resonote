package com.resonote.core.data

import com.resonote.core.model.PlaybackSpeed
import kotlinx.coroutines.flow.Flow

interface PlaybackPreferencesRepository {
    val playbackSpeed: Flow<PlaybackSpeed>

    suspend fun setPlaybackSpeed(speed: PlaybackSpeed)
}
