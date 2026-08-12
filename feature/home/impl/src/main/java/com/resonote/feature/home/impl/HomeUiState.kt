package com.resonote.feature.home.impl

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.HomeContent
import com.resonote.core.model.HomeIssue
import com.resonote.core.model.PlaylistSummary
import com.resonote.core.model.HomeSection
import com.resonote.core.model.OnlineSong

@Immutable
data class HomeSongUiModel(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val qualityLabel: String? = null,
    val isVip: Boolean = false,
    val artworkColors: List<Color>,
    val artworkUrl: String? = null,
)

@Immutable
data class HomePlaylistUiModel(
    val id: String,
    val title: String,
    val playCount: String,
    val artworkColors: List<Color>,
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

data class HomePlaybackRequest(
    val songs: List<OnlineSong>,
    val startIndex: Int,
) {
    init {
        require(songs.isNotEmpty()) { "Playback request requires at least one song" }
        require(startIndex in songs.indices) { "startIndex must point to a song" }
    }
}

internal fun HomeContent.toUiState(radioSongs: List<OnlineSong>): HomeContentUiState =
    HomeContentUiState(
        radio = radioSongs.firstOrNull()?.toUiModel() ?: dailyRecommendations.firstOrNull()?.toUiModel(),
        dailySongs = dailyRecommendations.map(OnlineSong::toUiModel),
        recommendedPlaylists = recommendedPlaylists.map(PlaylistSummary::toUiModel),
        newSongs = newSongs.map(OnlineSong::toUiModel),
    )

internal fun List<HomeIssue>.failedSections(): Set<HomeSection> = mapTo(linkedSetOf()) { it.section }

private fun OnlineSong.toUiModel(): HomeSongUiModel =
    HomeSongUiModel(
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
        artworkColors = artworkGradient(hash),
        artworkUrl = coverUrl,
    )

private fun PlaylistSummary.toUiModel(): HomePlaylistUiModel =
    HomePlaylistUiModel(
        id = id,
        title = title,
        playCount = playCount.toPlayCountLabel(),
        artworkColors = artworkGradient(id),
        artworkUrl = coverUrl,
    )

private fun Long.toDurationLabel(): String {
    val totalSeconds = (coerceAtLeast(0) / 1_000).toInt()
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun Long?.toPlayCountLabel(): String =
    when {
        this == null -> ""
        this >= 100_000_000 -> "%.1f亿".format(this / 100_000_000.0)
        this >= 10_000 -> "%.1f万".format(this / 10_000.0)
        else -> toString()
    }

private fun artworkGradient(seed: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFF5A061B), Color(0xFFE31353), Color(0xFFFF8DA9)),
        listOf(Color(0xFF042E48), Color(0xFF0879BC), Color(0xFFBBD9F4)),
        listOf(Color(0xFF20164B), Color(0xFF786EDB), Color(0xFFF4A9BC)),
        listOf(Color(0xFF6F2E19), Color(0xFFE38A52), Color(0xFFFFD9A8)),
        listOf(Color(0xFF123D36), Color(0xFF3A8068), Color(0xFFC6D9A8)),
        listOf(Color(0xFF31495D), Color(0xFF8BAABD), Color(0xFFEAF0F2)),
    )
    return palettes[(seed.hashCode() and Int.MAX_VALUE) % palettes.size]
}
