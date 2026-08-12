package com.resonote.core.data

import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaDeleteResult
import com.resonote.core.model.LocalMediaDuplicateAction
import com.resonote.core.model.LocalMediaId
import com.resonote.core.model.LocalMediaImportResult
import com.resonote.core.model.LocalMediaPlaybackSource
import kotlinx.coroutines.flow.Flow

interface LocalMediaRepository {
    suspend fun recoverStorage(): Boolean

    fun observeAll(): Flow<List<LocalMedia>>

    suspend fun importFromUri(
        sourceUri: String,
        duplicateAction: LocalMediaDuplicateAction = LocalMediaDuplicateAction.RequireConfirmation,
    ): LocalMediaImportResult

    suspend fun delete(id: LocalMediaId): LocalMediaDeleteResult

    suspend fun resolvePlaybackSource(id: LocalMediaId): LocalMediaPlaybackSource?
}
