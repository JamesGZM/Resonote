package com.resonote.core.data

import com.resonote.core.model.AudioQuality
import com.resonote.core.model.HomeContent
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistSummary
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal class HomeSnapshotCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(content: HomeContent): String = json.encodeToString(HomeSnapshotJson.from(content))

    fun decode(value: String): HomeContent? = runCatching {
        val snapshot = json.decodeFromString<HomeSnapshotJson>(value)
        HomeContent(
            dailyRecommendations = snapshot.dailyRecommendations.mapNotNull(SongSnapshotJson::toDomain),
            recommendedPlaylists = snapshot.recommendedPlaylists.mapNotNull(PlaylistSnapshotJson::toDomain),
            newSongs = snapshot.newSongs.mapNotNull(SongSnapshotJson::toDomain),
        )
    }.getOrNull()
}

@Serializable
private data class HomeSnapshotJson(
    val dailyRecommendations: List<SongSnapshotJson> = emptyList(),
    val recommendedPlaylists: List<PlaylistSnapshotJson> = emptyList(),
    val newSongs: List<SongSnapshotJson> = emptyList(),
) {
    companion object {
        fun from(content: HomeContent) = HomeSnapshotJson(
            dailyRecommendations = content.dailyRecommendations.map(SongSnapshotJson::from),
            recommendedPlaylists = content.recommendedPlaylists.map(PlaylistSnapshotJson::from),
            newSongs = content.newSongs.map(SongSnapshotJson::from),
        )
    }
}

@Serializable
private data class SongSnapshotJson(
    val hash: String = "",
    val title: String = "",
    val artist: String? = null,
    val coverUrl: String? = null,
    val albumId: String? = null,
    val albumAudioId: String? = null,
    val durationMillis: Long = 0,
    val quality: String = AudioQuality.Standard.name,
    val vip: Boolean = false,
    val albumTitle: String? = null,
    val fileId: String? = null,
    val previewDurationMillis: Long? = null,
) {
    fun toDomain(): OnlineSong? {
        if (hash.isBlank() || title.isBlank() || durationMillis < 0) return null
        return OnlineSong(
            hash = hash,
            title = title,
            artist = artist,
            coverUrl = coverUrl,
            albumId = albumId,
            albumAudioId = albumAudioId,
            durationMillis = durationMillis,
            quality = AudioQuality.entries.firstOrNull { it.name == quality } ?: AudioQuality.Standard,
            vip = vip,
            albumTitle = albumTitle,
            fileId = fileId,
            previewDurationMillis = previewDurationMillis?.takeIf { it >= 0 },
        )
    }

    companion object {
        fun from(song: OnlineSong) = SongSnapshotJson(
            hash = song.hash,
            title = song.title,
            artist = song.artist,
            coverUrl = song.coverUrl,
            albumId = song.albumId,
            albumAudioId = song.albumAudioId,
            durationMillis = song.durationMillis,
            quality = song.quality.name,
            vip = song.vip,
            albumTitle = song.albumTitle,
            fileId = song.fileId,
            previewDurationMillis = song.previewDurationMillis,
        )
    }
}

@Serializable
private data class PlaylistSnapshotJson(
    val id: String = "",
    val title: String = "",
    val coverUrl: String? = null,
    val playCount: Long? = null,
) {
    fun toDomain(): PlaylistSummary? = if (id.isBlank() || title.isBlank() || playCount?.let { it < 0 } == true) {
        null
    } else {
        PlaylistSummary(id, title, coverUrl, playCount)
    }

    companion object {
        fun from(playlist: PlaylistSummary) = PlaylistSnapshotJson(
            id = playlist.id,
            title = playlist.title,
            coverUrl = playlist.coverUrl,
            playCount = playlist.playCount,
        )
    }
}
