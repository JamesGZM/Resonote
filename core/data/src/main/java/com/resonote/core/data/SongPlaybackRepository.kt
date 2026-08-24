package com.resonote.core.data

import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.ResolveSongSourceResult

interface SongPlaybackRepository {
    suspend fun resolveSource(
        song: OnlineSong,
        qualityOverride: OnlinePlaybackQuality? = null,
    ): ResolveSongSourceResult
}
