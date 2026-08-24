@file:androidx.media3.common.util.UnstableApi

package com.resonote.core.playback.service

import com.resonote.core.playback.PlaybackCacheController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultPlaybackCacheController @Inject constructor(private val mediaCache: PlaybackMediaCache) :
    PlaybackCacheController {
    override suspend fun sizeBytes(): Long = withContext(Dispatchers.IO) { mediaCache.sizeBytes() }

    override suspend fun clear(): Long = withContext(Dispatchers.IO) {
        mediaCache.clear()
        mediaCache.sizeBytes()
    }
}
