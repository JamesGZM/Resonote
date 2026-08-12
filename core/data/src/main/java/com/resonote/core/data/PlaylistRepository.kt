package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.PlaylistPage

interface PlaylistRepository {
    suspend fun loadPlaylist(
        globalCollectionId: String,
        page: Int = 1,
        pageSize: Int = 50,
    ): CollectionLoadResult<PlaylistPage>
}
