package com.resonote.core.model

enum class AudioQuality {
    Standard,
    HighResolution,
    Lossless,
}

data class OnlineSong(
    val hash: String,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val albumId: String?,
    val albumAudioId: String?,
    val durationMillis: Long,
    val quality: AudioQuality,
    val vip: Boolean,
)

data class HomePlaylist(val id: String, val title: String, val coverUrl: String?, val playCount: Long?)

enum class RecommendationMode {
    Personal,
    Nostalgia,
    Popular,
    HiddenGems,
    Vip,
}

data class HomeContent(
    val dailyRecommendations: List<OnlineSong>,
    val recommendedPlaylists: List<HomePlaylist>,
    val newSongs: List<OnlineSong>,
)

enum class ContentFailure {
    Network,
    ServiceRejected,
    RiskVerificationUnavailable,
    Protocol,
}

enum class HomeSection {
    DailyRecommendations,
    RecommendedPlaylists,
    NewSongs,
}

data class HomeIssue(val section: HomeSection, val failure: ContentFailure)

sealed interface HomeRefreshResult {
    data class Updated(val content: HomeContent, val issues: List<HomeIssue>) : HomeRefreshResult

    data class Failed(val issues: List<HomeIssue>) : HomeRefreshResult
}

sealed interface RadioRecommendationResult {
    data class Available(val songs: List<OnlineSong>) : RadioRecommendationResult

    data class Failed(val failure: ContentFailure) : RadioRecommendationResult
}

data class ResolvedSongSource(val uri: String, val durationMillis: Long, val extension: String?)

enum class PlaybackUnavailableReason {
    Copyright,
    Vip,
}

sealed interface ResolveSongSourceResult {
    data class Resolved(val source: ResolvedSongSource) : ResolveSongSourceResult

    data class Unavailable(val reason: PlaybackUnavailableReason) : ResolveSongSourceResult

    data class Failed(val failure: ContentFailure) : ResolveSongSourceResult
}
