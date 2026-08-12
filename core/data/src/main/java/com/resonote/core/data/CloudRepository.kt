package com.resonote.core.data

import com.resonote.core.model.CloudPage
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ResolveSongSourceResult

interface CloudRepository {
    suspend fun loadTracks(
        page: Int = 1,
        pageSize: Int = 50,
    ): CollectionLoadResult<CloudPage>

    suspend fun resolveSource(track: CloudTrack): ResolveSongSourceResult
}
