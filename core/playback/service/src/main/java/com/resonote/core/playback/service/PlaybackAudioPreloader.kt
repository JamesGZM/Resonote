@file:androidx.media3.common.util.UnstableApi

package com.resonote.core.playback.service

import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import com.resonote.core.model.ResolvedSongSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

internal interface PlaybackAudioPreloader {
    suspend fun preload(source: ResolvedSongSource)
}

@Singleton
internal class DefaultPlaybackAudioPreloader internal constructor(
    private val mediaCache: PlaybackMediaCache,
    private val ioDispatcher: CoroutineDispatcher,
) : PlaybackAudioPreloader {
    @Inject
    constructor(mediaCache: PlaybackMediaCache) : this(mediaCache, Dispatchers.IO)

    override suspend fun preload(source: ResolvedSongSource) = withContext(ioDispatcher) {
        val cacheKey = source.cacheKey ?: return@withContext
        val job = currentCoroutineContext()
        CacheWriter(
            mediaCache.cacheDataSourceFactory.createDataSource(),
            buildPlaybackPreloadDataSpec(source.uri, cacheKey),
            null,
        ) { _, _, _ ->
            job.ensureActive()
        }.cache()
    }
}

internal fun buildPlaybackPreloadDataSpec(uri: String, cacheKey: String): DataSpec = DataSpec.Builder()
    .setUri(uri)
    .setKey(cacheKey)
    .setPosition(0)
    .setLength(PLAYBACK_PRELOAD_BYTES)
    .build()

internal const val PLAYBACK_PRELOAD_BYTES = 4L * 1024L * 1024L
