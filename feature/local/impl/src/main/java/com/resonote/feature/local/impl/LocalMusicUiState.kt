package com.resonote.feature.local.impl

import com.resonote.core.karaoke.KaraokePreviewState
import com.resonote.core.model.KaraokeProject
import com.resonote.core.model.KaraokeProjectId
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaImportCandidate
import com.resonote.core.model.LocalMediaImportFailure
import com.resonote.core.playback.MusicDownload
import com.resonote.core.playback.MusicDownloadState
import com.resonote.core.playback.PlaybackItem

sealed interface LocalLibraryItem {
    val stableId: String
    val title: String
    val artist: String?
    val albumTitle: String?
    val durationMillis: Long
    val sizeBytes: Long
    val sortTimeMillis: Long

    fun toPlaybackItem(): PlaybackItem

    data class Imported(val media: LocalMedia) : LocalLibraryItem {
        override val stableId = "imported:${media.id.value}"
        override val title = media.title
        override val artist = media.artist
        override val albumTitle = media.albumTitle
        override val durationMillis = media.durationMillis
        override val sizeBytes = media.sizeBytes
        override val sortTimeMillis = media.importedAtEpochMillis
        override fun toPlaybackItem() = PlaybackItem(media)
    }

    data class Downloaded(val download: MusicDownload) : LocalLibraryItem {
        override val stableId = "downloaded:${download.id}"
        override val title = download.song.title
        override val artist = download.song.artist
        override val albumTitle = download.song.albumTitle
        override val durationMillis = download.song.durationMillis
        override val sizeBytes = download.totalBytes ?: download.bytesDownloaded
        override val sortTimeMillis = download.updatedAtEpochMillis
        override fun toPlaybackItem() = PlaybackItem(
            song = download.song,
            resolvedSource = checkNotNull(download.completedPlaybackSource()),
        )
    }
}

data class LocalMusicUiState(
    val media: List<LocalMedia> = emptyList(),
    val downloads: List<MusicDownload> = emptyList(),
    val isLoading: Boolean = true,
    val query: String = "",
    val sort: LocalMusicSort = LocalMusicSort.ImportedNewest,
    val importState: LocalImportUiState = LocalImportUiState.Idle,
    val pendingDelete: LocalMedia? = null,
    val deletingMediaId: String? = null,
    val deleteFailed: Boolean = false,
    val pendingDownloadDelete: MusicDownload? = null,
    val selectedTab: LocalMusicTab = LocalMusicTab.Songs,
    val karaokeProjects: List<KaraokeProject> = emptyList(),
    val karaokeProjectsLoading: Boolean = true,
    val karaokeProjectsLoadFailed: Boolean = false,
    val selectedProjectIds: Set<KaraokeProjectId> = emptySet(),
    val editingProject: KaraokeProject? = null,
    val preview: KaraokePreviewState = KaraokePreviewState(),
) {
    val completedDownloads: List<MusicDownload>
        get() = downloads.filter { it.state == MusicDownloadState.Completed }

    val activeDownloadCount: Int
        get() = downloads.count {
            it.state == MusicDownloadState.Preparing ||
                it.state == MusicDownloadState.Queued ||
                it.state == MusicDownloadState.Downloading
        }

    val failedDownloadCount: Int
        get() = downloads.count { it.state == MusicDownloadState.Failed }

    val libraryItems: List<LocalLibraryItem>
        get() = media.map(LocalLibraryItem::Imported) + completedDownloads.map(LocalLibraryItem::Downloaded)

    val visibleItems: List<LocalLibraryItem>
        get() {
            val normalizedQuery = query.trim()
            val filtered = if (normalizedQuery.isEmpty()) {
                libraryItems
            } else {
                libraryItems.filter { item ->
                    item.title.contains(normalizedQuery, ignoreCase = true) ||
                        item.artist.orEmpty().contains(normalizedQuery, ignoreCase = true) ||
                        item.albumTitle.orEmpty().contains(normalizedQuery, ignoreCase = true)
                }
            }
            return when (sort) {
                LocalMusicSort.ImportedNewest -> filtered.sortedByDescending(LocalLibraryItem::sortTimeMillis)
                LocalMusicSort.Title -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                LocalMusicSort.Artist -> filtered.sortedWith { first, second ->
                    val artistOrder = String.CASE_INSENSITIVE_ORDER.compare(
                        first.artist.orEmpty(),
                        second.artist.orEmpty(),
                    )
                    if (artistOrder != 0) {
                        artistOrder
                    } else {
                        String.CASE_INSENSITIVE_ORDER.compare(first.title, second.title)
                    }
                }
                LocalMusicSort.Duration -> filtered.sortedByDescending(LocalLibraryItem::durationMillis)
            }
        }

    val visibleMedia: List<LocalMedia>
        get() {
            val normalizedQuery = query.trim()
            val filtered = if (normalizedQuery.isEmpty()) {
                media
            } else {
                media.filter { item ->
                    item.title.contains(normalizedQuery, ignoreCase = true) ||
                        item.artist.orEmpty().contains(normalizedQuery, ignoreCase = true) ||
                        item.albumTitle.orEmpty().contains(normalizedQuery, ignoreCase = true) ||
                        item.displayName.contains(normalizedQuery, ignoreCase = true)
                }
            }
            return when (sort) {
                LocalMusicSort.ImportedNewest -> filtered.sortedByDescending(LocalMedia::importedAtEpochMillis)
                LocalMusicSort.Title -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                LocalMusicSort.Artist -> filtered.sortedWith { first, second ->
                    val artistOrder = String.CASE_INSENSITIVE_ORDER.compare(
                        first.artist.orEmpty(),
                        second.artist.orEmpty(),
                    )
                    if (artistOrder !=
                        0
                    ) {
                        artistOrder
                    } else {
                        String.CASE_INSENSITIVE_ORDER.compare(first.title, second.title)
                    }
                }
                LocalMusicSort.Duration -> filtered.sortedByDescending(LocalMedia::durationMillis)
            }
        }

    val visibleKaraokeProjects: List<KaraokeProject>
        get() {
            val normalizedQuery = query.trim()
            val filtered = if (normalizedQuery.isEmpty()) {
                karaokeProjects
            } else {
                karaokeProjects.filter { project ->
                    project.songTitle.contains(normalizedQuery, ignoreCase = true) ||
                        project.artist.orEmpty().contains(normalizedQuery, ignoreCase = true)
                }
            }
            return filtered.sortedWith(
                compareByDescending<KaraokeProject> { it.createdAtEpochMillis }
                    .thenByDescending { it.id.value },
            )
        }
}

enum class LocalMusicTab { Songs, KaraokeWorks }

enum class LocalMusicSort { ImportedNewest, Title, Artist, Duration }

sealed interface LocalImportUiState {
    data object Idle : LocalImportUiState

    data object ScanningDirectory : LocalImportUiState

    data class Running(val completed: Int, val total: Int, val imported: Int, val failed: Int) : LocalImportUiState

    data class AwaitingDuplicate(
        val candidate: LocalMediaImportCandidate,
        val existing: List<LocalMedia>,
        val completed: Int,
        val total: Int,
        val imported: Int,
        val failed: Int,
    ) : LocalImportUiState

    data class Completed(
        val total: Int,
        val imported: Int,
        val skipped: Int,
        val failures: List<LocalMediaImportFailure>,
    ) : LocalImportUiState

    data class DirectoryFailed(val reason: LocalDirectoryImportFailure) : LocalImportUiState
}

enum class LocalDirectoryImportFailure {
    NoFiles,
    InvalidTree,
    PermissionDenied,
    Unavailable,
}
