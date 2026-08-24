package com.resonote.core.playback

interface PlaybackCacheController {
    suspend fun sizeBytes(): Long

    suspend fun clear(): Long
}
