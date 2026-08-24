package com.resonote.core.data

import com.resonote.core.model.AppRelease
import com.resonote.core.model.CollectionLoadResult

interface AppUpdateRepository {
    suspend fun latestRelease(): CollectionLoadResult<AppRelease>
}
