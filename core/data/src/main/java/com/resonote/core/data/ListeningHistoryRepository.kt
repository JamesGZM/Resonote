package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.OnlineSong

interface ListeningHistoryRepository {
    suspend fun loadAccountHistory(): CollectionLoadResult<List<OnlineSong>>
}
