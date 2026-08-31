@file:androidx.media3.common.util.UnstableApi

package com.resonote.core.playback.service

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.scheduler.Requirements
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PlaybackDownloadStore @Inject constructor(@ApplicationContext context: Context) {
    private val databaseProvider = StandaloneDatabaseProvider(context)

    val cache = SimpleCache(
        File(context.filesDir, DOWNLOAD_DIRECTORY_NAME),
        NoOpCacheEvictor(),
        databaseProvider,
    )

    val cacheOnlyDataSourceFactory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(null)

    val manager = DownloadManager(
        context,
        databaseProvider,
        cache,
        DefaultHttpDataSource.Factory(),
        Executors.newFixedThreadPool(MAX_PARALLEL_DOWNLOADS),
    ).apply {
        maxParallelDownloads = MAX_PARALLEL_DOWNLOADS
        requirements = Requirements(Requirements.NETWORK)
        resumeDownloads()
    }

    private companion object {
        const val DOWNLOAD_DIRECTORY_NAME = "music-downloads"
        const val MAX_PARALLEL_DOWNLOADS = 2
    }
}
