package com.resonote.feature.local.impl

import com.resonote.core.karaoke.KaraokePreviewState
import com.resonote.core.model.KaraokeProject
import com.resonote.core.model.KaraokeProjectId
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaImportCandidate
import com.resonote.core.model.LocalMediaImportFailure

data class LocalMusicUiState(
    val media: List<LocalMedia> = emptyList(),
    val isLoading: Boolean = true,
    val query: String = "",
    val sort: LocalMusicSort = LocalMusicSort.ImportedNewest,
    val importState: LocalImportUiState = LocalImportUiState.Idle,
    val pendingDelete: LocalMedia? = null,
    val deletingMediaId: String? = null,
    val deleteFailed: Boolean = false,
    val selectedTab: LocalMusicTab = LocalMusicTab.Songs,
    val karaokeProjects: List<KaraokeProject> = emptyList(),
    val karaokeProjectsLoading: Boolean = true,
    val karaokeProjectsLoadFailed: Boolean = false,
    val selectedProjectIds: Set<KaraokeProjectId> = emptySet(),
    val editingProject: KaraokeProject? = null,
    val preview: KaraokePreviewState = KaraokePreviewState(),
) {
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
            return if (normalizedQuery.isEmpty()) {
                karaokeProjects
            } else {
                karaokeProjects.filter { project ->
                    project.songTitle.contains(normalizedQuery, ignoreCase = true) ||
                        project.artist.orEmpty().contains(normalizedQuery, ignoreCase = true)
                }
            }
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
