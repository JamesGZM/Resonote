package com.resonote.core.data

import com.resonote.core.datastore.PlaybackSessionSnapshotStorage
import com.resonote.core.model.AudioQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultPlaybackSessionRepository @Inject constructor(
    private val storage: PlaybackSessionSnapshotStorage,
) : PlaybackSessionRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun load(): PlaybackSessionSnapshot? {
        val stored = runCatching { storage.snapshotJson.first() }.getOrNull() ?: return null
        val decoded = withContext(Dispatchers.Default) {
            runCatching { json.decodeFromString<PlaybackSessionSnapshotJson>(stored) }.getOrNull()
        } ?: return null
        val entries = decoded.entries.mapNotNull(PlaybackSessionEntryJson::toDomain)
        if (entries.isEmpty() || entries.size != decoded.entries.size || decoded.currentIndex !in entries.indices) {
            return null
        }
        return PlaybackSessionSnapshot(
            entries = entries,
            currentIndex = decoded.currentIndex,
            positionMillis = decoded.positionMillis.coerceAtLeast(0),
            mode = decoded.mode,
        )
    }

    override suspend fun save(snapshot: PlaybackSessionSnapshot) {
        require(snapshot.entries.isNotEmpty()) { "playback snapshot must contain entries" }
        require(snapshot.currentIndex in snapshot.entries.indices) { "playback snapshot index is invalid" }
        val encoded = withContext(Dispatchers.Default) {
            json.encodeToString(PlaybackSessionSnapshotJson.from(snapshot))
        }
        storage.write(encoded)
    }

    override suspend fun clear() = storage.clear()
}

@Serializable
private data class PlaybackSessionSnapshotJson(
    val entries: List<PlaybackSessionEntryJson> = emptyList(),
    val currentIndex: Int = -1,
    val positionMillis: Long = 0,
    val mode: String = "ListLoop",
) {
    companion object {
        fun from(snapshot: PlaybackSessionSnapshot) = PlaybackSessionSnapshotJson(
            entries = snapshot.entries.map(PlaybackSessionEntryJson::from),
            currentIndex = snapshot.currentIndex,
            positionMillis = snapshot.positionMillis.coerceAtLeast(0),
            mode = snapshot.mode,
        )
    }
}

@Serializable
private data class PlaybackSessionEntryJson(
    val kind: String = "",
    val mediaId: String = "",
    val title: String = "",
    val artist: String? = null,
    val albumTitle: String? = null,
    val artworkUri: String? = null,
    val durationMillis: Long = 0,
    val isVip: Boolean = false,
    val audioQuality: String? = null,
    val albumId: String? = null,
    val albumAudioId: String? = null,
    val fileId: String? = null,
    val previewDurationMillis: Long? = null,
    val mimeType: String? = null,
    val extension: String? = null,
    val sampleRateHz: Int? = null,
    val bitDepth: Int? = null,
    val bitrateBitsPerSecond: Int? = null,
) {
    fun toDomain(): PlaybackSessionEntry? {
        val parsedKind = PlaybackSessionEntryKind.entries.firstOrNull { it.name == kind } ?: return null
        if (mediaId.isBlank() || title.isBlank() || durationMillis < 0) return null
        val quality = audioQuality?.let { stored -> AudioQuality.entries.firstOrNull { it.name == stored } }
        if (parsedKind == PlaybackSessionEntryKind.Online && quality == null) return null
        return PlaybackSessionEntry(
            kind = parsedKind,
            mediaId = mediaId,
            title = title,
            artist = artist,
            albumTitle = albumTitle,
            artworkUri = artworkUri,
            durationMillis = durationMillis,
            isVip = isVip,
            audioQuality = quality,
            albumId = albumId,
            albumAudioId = albumAudioId,
            fileId = fileId,
            previewDurationMillis = previewDurationMillis?.takeIf { it >= 0 },
            mimeType = mimeType,
            extension = extension,
            sampleRateHz = sampleRateHz?.takeIf { it > 0 },
            bitDepth = bitDepth?.takeIf { it > 0 },
            bitrateBitsPerSecond = bitrateBitsPerSecond?.takeIf { it > 0 },
        )
    }

    companion object {
        fun from(entry: PlaybackSessionEntry) = PlaybackSessionEntryJson(
            kind = entry.kind.name,
            mediaId = entry.mediaId,
            title = entry.title,
            artist = entry.artist,
            albumTitle = entry.albumTitle,
            artworkUri = entry.artworkUri,
            durationMillis = entry.durationMillis,
            isVip = entry.isVip,
            audioQuality = entry.audioQuality?.name,
            albumId = entry.albumId,
            albumAudioId = entry.albumAudioId,
            fileId = entry.fileId,
            previewDurationMillis = entry.previewDurationMillis,
            mimeType = entry.mimeType,
            extension = entry.extension,
            sampleRateHz = entry.sampleRateHz,
            bitDepth = entry.bitDepth,
            bitrateBitsPerSecond = entry.bitrateBitsPerSecond,
        )
    }
}
