package com.resonote.core.model

data class ResolvedSongSource(
    val uri: String,
    val durationMillis: Long,
    val extension: String?,
    val isPreview: Boolean = false,
)

enum class OnlinePlaybackQuality {
    Standard,
    HighQuality,
    Lossless,
    HighResolution,
    ViperAtmos,
    ViperClear,
    ViperTape,
}

enum class PlaybackUnavailableReason {
    Copyright,
    Vip,
    Cloud,
    Local,
}

sealed interface ResolveSongSourceResult {
    data class Resolved(val source: ResolvedSongSource) : ResolveSongSourceResult

    data class Unavailable(val reason: PlaybackUnavailableReason) : ResolveSongSourceResult

    data class Failed(val failure: ContentFailure) : ResolveSongSourceResult
}
