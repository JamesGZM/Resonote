@file:androidx.media3.common.util.UnstableApi

package com.resonote.core.playback.service

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PlaybackMediaCache @Inject constructor(@ApplicationContext context: Context) {
    private val cache = SimpleCache(
        File(context.cacheDir, CACHE_DIRECTORY_NAME),
        LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
        StandaloneDatabaseProvider(context),
    )
    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()

    val cacheDataSourceFactory: CacheDataSource.Factory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(httpDataSourceFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    val playbackDataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(
        context,
        cacheDataSourceFactory,
    )

    private companion object {
        const val CACHE_DIRECTORY_NAME = "playback_media"
        const val MAX_CACHE_BYTES = 512L * 1024L * 1024L
    }
}
