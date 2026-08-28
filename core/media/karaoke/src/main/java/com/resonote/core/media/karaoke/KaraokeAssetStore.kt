package com.resonote.core.media.karaoke

data class KaraokeStoredAsset(val path: String, val sizeBytes: Long)

sealed interface KaraokeStoreResult<out T> {
    data class Success<T>(val value: T) : KaraokeStoreResult<T>

    data class Failure(val reason: KaraokeStoreFailure) : KaraokeStoreResult<Nothing>
}

enum class KaraokeStoreFailure {
    InvalidSource,
    SourceUnavailable,
    InsufficientStorage,
    StorageUnavailable,
}

interface KaraokeAssetStore {
    suspend fun persistSource(
        projectId: String,
        assetId: String,
        sourceUri: String,
        extension: String?,
    ): KaraokeStoreResult<KaraokeStoredAsset>

    suspend fun createRecordingFile(projectId: String, segmentId: String): KaraokeStoreResult<String>

    suspend fun hasRecordingCapacity(expectedDurationMillis: Long): Boolean

    suspend fun removeProject(projectId: String): KaraokeStoreResult<Unit>
}
