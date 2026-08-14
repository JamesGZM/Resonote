package com.resonote.core.model

@JvmInline
value class LocalMediaId(val value: String) {
    init {
        require(value.isNotBlank()) { "LocalMediaId must not be blank" }
    }
}

data class LocalMedia(
    val id: LocalMediaId,
    val displayName: String,
    val title: String,
    val artist: String?,
    val albumTitle: String?,
    val artworkUri: String?,
    val durationMillis: Long,
    val mimeType: String?,
    val fileExtension: String?,
    val sizeBytes: Long,
    val sampleRateHz: Int?,
    val bitDepth: Int?,
    val bitrateBitsPerSecond: Int?,
    val importedAtEpochMillis: Long,
) {
    init {
        require(displayName.isNotBlank()) { "displayName must not be blank" }
        require(title.isNotBlank()) { "title must not be blank" }
        require(durationMillis >= 0) { "durationMillis must not be negative" }
        require(sizeBytes >= 0) { "sizeBytes must not be negative" }
        require(sampleRateHz == null || sampleRateHz > 0) { "sampleRateHz must be positive" }
        require(bitDepth == null || bitDepth > 0) { "bitDepth must be positive" }
        require(bitrateBitsPerSecond == null || bitrateBitsPerSecond > 0) {
            "bitrateBitsPerSecond must be positive"
        }
        require(importedAtEpochMillis >= 0) { "importedAtEpochMillis must not be negative" }
    }
}

enum class LocalMediaDuplicateAction {
    RequireConfirmation,
    ImportCopy,
}

data class LocalMediaImportCandidate(
    val displayName: String,
    val title: String,
    val artist: String?,
    val sizeBytes: Long,
    val mimeType: String?,
)

sealed interface LocalMediaImportResult {
    data class Imported(val media: LocalMedia) : LocalMediaImportResult

    data class DuplicateConfirmationRequired(
        val candidate: LocalMediaImportCandidate,
        val existing: List<LocalMedia>,
    ) : LocalMediaImportResult

    data class Failed(val reason: LocalMediaImportFailure) : LocalMediaImportResult
}

enum class LocalMediaImportFailure {
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
    IndexUnavailable,
}

sealed interface LocalMediaDeleteResult {
    data object Deleted : LocalMediaDeleteResult

    data object NotFound : LocalMediaDeleteResult

    data object Failed : LocalMediaDeleteResult
}

data class LocalMediaPlaybackSource(val uri: String, val media: LocalMedia)
