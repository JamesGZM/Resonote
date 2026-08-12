package com.resonote.core.media.local

interface LocalMediaStore {
    suspend fun inspect(sourceUri: String): LocalMediaStoreResult<LocalMediaSourceInspection>

    suspend fun calculateDigest(
        sourceUri: String,
        expectedSizeBytes: Long?,
    ): LocalMediaStoreResult<LocalMediaDigest>

    suspend fun persist(request: LocalMediaPersistRequest): LocalMediaStoreResult<StoredLocalMedia>

    suspend fun remove(files: LocalMediaFiles): LocalMediaStoreResult<Unit>
}

sealed interface LocalMediaStoreResult<out T> {
    data class Success<T>(val value: T) : LocalMediaStoreResult<T>

    data class Failure(val error: LocalMediaStoreError) : LocalMediaStoreResult<Nothing>
}

enum class LocalMediaStoreError {
    InvalidSource,
    PermissionDenied,
    SourceUnavailable,
    EmptyFile,
    UnsupportedFormat,
    MetadataUnavailable,
    InsufficientStorage,
    HashFailed,
    SourceChanged,
    StorageUnavailable,
}

data class LocalMediaSourceInspection(
    val displayName: String,
    val reportedSizeBytes: Long?,
    val declaredMimeType: String?,
    val fileExtension: String?,
    val metadata: LocalMediaMetadata,
)

data class LocalMediaMetadata(
    val title: String,
    val artist: String?,
    val albumTitle: String?,
    val durationMillis: Long,
    val detectedMimeType: String,
    val sampleRateHz: Int?,
    val bitDepth: Int?,
    val bitrateBitsPerSecond: Int?,
)

data class LocalMediaDigest(
    val sizeBytes: Long,
    val sha256: String,
)

data class LocalMediaPersistRequest(
    val sourceUri: String,
    val storageKey: String,
    val inspection: LocalMediaSourceInspection,
    val expectedDigest: LocalMediaDigest,
)

data class LocalMediaFiles(
    val audioPath: String,
    val artworkPath: String?,
)

data class StoredLocalMedia(
    val files: LocalMediaFiles,
    val displayName: String,
    val fileExtension: String?,
    val digest: LocalMediaDigest,
    val metadata: LocalMediaMetadata,
)
