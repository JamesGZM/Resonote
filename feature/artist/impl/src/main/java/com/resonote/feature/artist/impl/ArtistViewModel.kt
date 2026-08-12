package com.resonote.feature.artist.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.ContentCatalogRepository
import com.resonote.core.model.ArtistInfo
import com.resonote.core.model.CollectionLoadResult
import com.resonote.feature.artist.api.ArtistNavKey
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repository: ContentCatalogRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ArtistUiState())
    val uiState: StateFlow<ArtistUiState> = mutableUiState.asStateFlow()

    private var artistKey: ArtistNavKey? = null
    private var popularJob: Job? = null
    private var latestJob: Job? = null

    fun load(key: ArtistNavKey) {
        if (key == artistKey) return
        artistKey = key
        popularJob?.cancel()
        latestJob?.cancel()
        mutableUiState.value = ArtistUiState(profile = key.toProfile())
        loadFirstPage(ArtistSongSection.POPULAR)
    }

    fun selectSection(section: ArtistSongSection) {
        mutableUiState.update { it.copy(selectedSection = section) }
        if (mutableUiState.value.page(section) is ArtistPageUiState.Idle) loadFirstPage(section)
    }

    fun retry() = loadFirstPage(mutableUiState.value.selectedSection)

    fun loadMore() {
        val key = artistKey ?: return
        val section = mutableUiState.value.selectedSection
        val current = mutableUiState.value.page(section) as? ArtistPageUiState.Content ?: return
        if (!current.hasMore || current.isLoadingMore || job(section)?.isActive == true) return
        mutableUiState.update {
            it.withPage(section, current.copy(isLoadingMore = true, loadMoreFailure = null))
        }
        setJob(section, viewModelScope.launch {
            when (
                val result = repository.loadArtistSongs(
                    artistId = key.artistId,
                    page = current.page + 1,
                    newestFirst = section == ArtistSongSection.LATEST,
                )
            ) {
                is CollectionLoadResult.Available -> mutableUiState.update { state ->
                    val latest = state.page(section) as? ArtistPageUiState.Content ?: return@update state
                    val existing = latest.songs.mapTo(mutableSetOf()) { it.hash }
                    state.withPage(
                        section,
                        latest.copy(
                            songs = latest.songs + result.value.songs.filter { existing.add(it.hash) },
                            page = result.value.page,
                            total = maxOf(latest.total, result.value.total, latest.songs.size + result.value.songs.size),
                            hasMore = result.value.hasMore,
                            isLoadingMore = false,
                            loadMoreFailure = null,
                        ),
                    )
                }
                is CollectionLoadResult.Failed -> mutableUiState.update { state ->
                    val latest = state.page(section) as? ArtistPageUiState.Content ?: return@update state
                    state.withPage(
                        section,
                        latest.copy(isLoadingMore = false, loadMoreFailure = result.failure),
                    )
                }
            }
        })
    }

    private fun loadFirstPage(section: ArtistSongSection) {
        val key = artistKey ?: return
        job(section)?.cancel()
        mutableUiState.update { it.withPage(section, ArtistPageUiState.Loading) }
        setJob(section, viewModelScope.launch {
            when (
                val result = repository.loadArtistSongs(
                    artistId = key.artistId,
                    page = 1,
                    newestFirst = section == ArtistSongSection.LATEST,
                )
            ) {
                is CollectionLoadResult.Available -> mutableUiState.update { state ->
                    val profile = state.profile.merge(result.value.info)
                    val songs = result.value.songs
                    val total = maxOf(result.value.total, profile?.songCount ?: 0, songs.size)
                    state.copy(profile = profile).withPage(
                        section,
                        if (songs.isEmpty()) {
                            ArtistPageUiState.Empty
                        } else {
                            ArtistPageUiState.Content(
                                songs = songs,
                                page = result.value.page,
                                total = total,
                                hasMore = result.value.hasMore,
                            )
                        },
                    )
                }
                is CollectionLoadResult.Failed -> mutableUiState.update {
                    it.withPage(section, ArtistPageUiState.Error(result.failure))
                }
            }
        })
    }

    private fun job(section: ArtistSongSection): Job? = when (section) {
        ArtistSongSection.POPULAR -> popularJob
        ArtistSongSection.LATEST -> latestJob
    }

    private fun setJob(section: ArtistSongSection, job: Job) {
        when (section) {
            ArtistSongSection.POPULAR -> popularJob = job
            ArtistSongSection.LATEST -> latestJob = job
        }
    }
}

private fun ArtistNavKey.toProfile() = ArtistProfile(
    id = artistId,
    name = name.nonBlank(),
    avatarUrl = avatarUrl.nonBlank(),
    intro = null,
    songCount = songCount,
    albumCount = albumCount,
    mvCount = null,
    fansCount = null,
)

private fun ArtistProfile?.merge(info: ArtistInfo?): ArtistProfile? {
    if (info == null) return this
    return ArtistProfile(
        id = requireNotNull(this).id,
        name = info.name.nonBlank() ?: name,
        avatarUrl = info.avatarUrl.nonBlank() ?: avatarUrl,
        intro = info.intro.nonBlank(),
        songCount = info.songCount,
        albumCount = info.albumCount,
        mvCount = info.mvCount,
        fansCount = info.fansCount,
    )
}

private fun String?.nonBlank(): String? = this?.takeIf(String::isNotBlank)
