package com.resonote.core.model

enum class AudioQuality {
    Standard,
    HighQuality,
    HighResolution,
    Lossless,
}

data class OnlineSong(
    val hash: String,
    val title: String,
    val artist: String?,
    val coverUrl: String?,
    val albumId: String?,
    val albumAudioId: String?,
    val durationMillis: Long,
    val quality: AudioQuality,
    val vip: Boolean,
    val albumTitle: String? = null,
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

sealed interface ContentFailure {
    data object Network : ContentFailure

    data object ServiceRejected : ContentFailure

    data class RiskVerificationRequired(val challenge: RiskChallengeHandle) : ContentFailure

    data object Protocol : ContentFailure
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

    data object Superseded : HomeRefreshResult
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

data class Ranking(val id: String, val title: String, val coverUrl: String?)

data class SongPage(val songs: List<OnlineSong>, val page: Int, val total: Int?, val hasMore: Boolean)

data class PlaylistDetails(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String?,
    val songCount: Int,
)

data class PlaylistPage(
    val details: PlaylistDetails?,
    val songs: List<OnlineSong>,
    val page: Int,
    val hasMore: Boolean,
)

sealed interface CollectionLoadResult<out T> {
    data class Available<T>(val value: T) : CollectionLoadResult<T>

    data class Failed(val failure: ContentFailure) : CollectionLoadResult<Nothing>
}
