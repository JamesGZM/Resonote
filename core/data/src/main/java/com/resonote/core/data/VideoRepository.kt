package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult

interface VideoRepository {
    suspend fun resolveVideoUrl(hash: String): CollectionLoadResult<String?>
}
