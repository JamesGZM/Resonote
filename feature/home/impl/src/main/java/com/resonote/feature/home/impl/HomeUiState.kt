package com.resonote.feature.home.impl

import androidx.compose.runtime.Immutable
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.HomeContent
import com.resonote.core.model.HomeIssue
import com.resonote.core.model.HomeSection
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistSummary
import java.util.Locale

@Immutable
data class HomeSongUiModel(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val qualityLabel: String? = null,
    val isVip: Boolean = false,
    val artworkUrl: String? = null,
)

@Immutable
data class HomePlaylistUiModel(
    val id: String,
    val title: String,
    val playCount: String,
    val artworkUrl: String? = null,
)

@Immutable
data class HomeContentUiState(
    val radio: HomeSongUiModel?,
    val dailySongs: List<HomeSongUiModel>,
    val recommendedPlaylists: List<HomePlaylistUiModel>,
    val newSongs: List<HomeSongUiModel>,
)

@Immutable
sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(
        val content: HomeContentUiState,
        val isRefreshing: Boolean = false,
        val issues: Set<HomeSection> = emptySet(),
    ) : HomeUiState

    data class Error(val issues: Set<HomeSection>) : HomeUiState
}

enum class HomeSongCollection {
    RADIO,
    DAILY_RECOMMENDATIONS,
    NEW_SONGS,
}

data class HomePlaybackRequest(val songs: List<OnlineSong>, val startIndex: Int) {
    init {
        require(songs.isNotEmpty()) { "Playback request requires at least one song" }
        require(startIndex in songs.indices) { "startIndex must point to a song" }
    }
}

internal fun HomeContent.toUiState(radioSongs: List<OnlineSong>): HomeContentUiState = HomeContentUiState(
    radio = radioSongs.firstOrNull()?.toUiModel() ?: dailyRecommendations.firstOrNull()?.toUiModel(),
    dailySongs = dailyRecommendations.map(OnlineSong::toUiModel),
    recommendedPlaylists = recommendedPlaylists.map(PlaylistSummary::toUiModel),
    newSongs = newSongs.map(OnlineSong::toUiModel),
)

internal fun List<HomeIssue>.failedSections(): Set<HomeSection> = mapTo(linkedSetOf()) { it.section }

private fun OnlineSong.toUiModel(): HomeSongUiModel = HomeSongUiModel(
    id = hash,
    title = title,
    artist = artist.orEmpty(),
    duration = durationMillis.toDurationLabel(),
    qualityLabel =
    when (quality) {
        AudioQuality.Standard -> null
        AudioQuality.HighQuality -> "HQ"
        AudioQuality.HighResolution -> "HI-RES"
        AudioQuality.Lossless -> "LOSSLESS"
    },
    isVip = vip,
    artworkUrl = coverUrl,
)

private fun PlaylistSummary.toUiModel(): HomePlaylistUiModel = HomePlaylistUiModel(
    id = id,
    title = title,
    playCount = playCount.toPlayCountLabel(),
    artworkUrl = coverUrl,
)

private fun Long.toDurationLabel(): String {
    val totalSeconds = (coerceAtLeast(0) / 1_000).toInt()
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun Long?.toPlayCountLabel(): String = when {
    this == null -> ""
    this >= 100_000_000 && Locale.getDefault().language == Locale.CHINESE.language ->
        compactLabel(this / 100_000_000.0, "亿")
    this >= 10_000 && Locale.getDefault().language == Locale.CHINESE.language ->
        compactLabel(this / 10_000.0, "万")
    this >= 1_000_000_000 -> compactLabel(this / 1_000_000_000.0, "B")
    this >= 1_000_000 -> compactLabel(this / 1_000_000.0, "M")
    this >= 1_000 -> compactLabel(this / 1_000.0, "K")
    else -> toString()
}

private fun compactLabel(value: Double, suffix: String): String {
    val formatted = if (value >= 100 || value % 1.0 == 0.0) "%.0f" else "%.1f"
    return formatted.format(Locale.ROOT, value) + suffix
}
