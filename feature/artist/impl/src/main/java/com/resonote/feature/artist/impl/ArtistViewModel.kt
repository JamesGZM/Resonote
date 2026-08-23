package com.resonote.feature.artist.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.ContentCatalogRepository
import com.resonote.core.model.ArtistInfo
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.feature.artist.api.ArtistNavKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(private val repository: ContentCatalogRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ArtistUiState())
    val uiState: StateFlow<ArtistUiState> = mutableUiState.asStateFlow()
    private val mutableLoginRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loginRequests: SharedFlow<Unit> = mutableLoginRequests.asSharedFlow()

    private var artistKey: ArtistNavKey? = null
    private var accountId: String? = null
    private val jobs = mutableMapOf<ArtistPageKey, Job>()
    private var followJob: Job? = null

    fun load(key: ArtistNavKey) {
        if (key == artistKey) return
        artistKey = key
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        followJob?.cancel()
        mutableUiState.value = ArtistUiState(profile = key.toProfile())
        loadFirstPage(ArtistPageKey(ArtistSection.SONGS, ArtistSort.POPULAR))
        loadFollowState()
    }

    fun onAccountChanged(accountId: String?) {
        if (this.accountId == accountId) return
        this.accountId = accountId
        if (artistKey != null) loadFollowState()
    }

    fun selectSection(section: ArtistSection) {
        mutableUiState.update { it.copy(selectedSection = section) }
        ensureSelectedPageLoaded()
    }

    fun selectSort(sort: ArtistSort) {
        if (mutableUiState.value.selectedSection == ArtistSection.MVS) return
        mutableUiState.update { it.copy(selectedSort = sort) }
        ensureSelectedPageLoaded()
    }

    fun retry() = loadFirstPage(selectedPageKey())

    fun refresh() {
        val key = selectedPageKey()
        val current = mutableUiState.value.page(key.section, key.sort) as? ArtistPageUiState.Content ?: return
        if (current.isRefreshing) return
        jobs[key]?.cancel()
        mutableUiState.update {
            it.withPage(
                key.section,
                key.sort,
                current.copy(
                    isLoadingMore = false,
                    isRefreshing = true,
                    refreshFailure = null,
                    loadMoreFailure = null,
                ),
            )
        }
        loadPage(key = key, page = 1, isRefresh = true)
    }

    fun acknowledgeRefreshFailure() {
        val key = selectedPageKey()
        mutableUiState.update { state ->
            val page = state.page(key.section, key.sort) as? ArtistPageUiState.Content ?: return@update state
            state.withPage(key.section, key.sort, page.copy(refreshFailure = null))
        }
    }

    fun toggleFollow() {
        when (val follow = mutableUiState.value.follow) {
            ArtistFollowUiState.AuthenticationRequired -> mutableLoginRequests.tryEmit(Unit)
            is ArtistFollowUiState.Error -> loadFollowState()
            ArtistFollowUiState.Loading -> Unit
            is ArtistFollowUiState.Available -> updateFollow(follow)
        }
    }

    fun acknowledgeFollowFailure() {
        mutableUiState.update { state ->
            val follow = state.follow as? ArtistFollowUiState.Available ?: return@update state
            state.copy(follow = follow.copy(updateFailure = null))
        }
    }

    fun loadMore() {
        val key = selectedPageKey()
        val current = mutableUiState.value.page(key.section, key.sort) as? ArtistPageUiState.Content ?: return
        if (!current.hasMore || current.isLoadingMore || current.isRefreshing || jobs[key]?.isActive == true) return
        mutableUiState.update {
            it.withPage(
                key.section,
                key.sort,
                current.copy(isLoadingMore = true, loadMoreFailure = null),
            )
        }
        loadPage(key = key, page = current.page + 1, isLoadMore = true)
    }

    private fun ensureSelectedPageLoaded() {
        val key = selectedPageKey()
        if (mutableUiState.value.page(key.section, key.sort) is ArtistPageUiState.Idle) loadFirstPage(key)
    }

    private fun loadFollowState() {
        val artistId = artistKey?.artistId ?: return
        followJob?.cancel()
        mutableUiState.update { it.copy(follow = ArtistFollowUiState.Loading) }
        followJob = viewModelScope.launch {
            when (val result = repository.loadArtistFollowed(artistId)) {
                is CollectionLoadResult.Available -> mutableUiState.update {
                    it.copy(follow = ArtistFollowUiState.Available(result.value))
                }
                is CollectionLoadResult.Failed -> mutableUiState.update {
                    it.copy(
                        follow = if (result.failure == ContentFailure.AuthenticationRequired) {
                            ArtistFollowUiState.AuthenticationRequired
                        } else {
                            ArtistFollowUiState.Error(result.failure)
                        },
                    )
                }
            }
        }
    }

    private fun updateFollow(current: ArtistFollowUiState.Available) {
        val artistId = artistKey?.artistId ?: return
        if (current.isUpdating) return
        val target = !current.isFollowed
        followJob?.cancel()
        mutableUiState.update {
            it.copy(follow = current.copy(isUpdating = true, updateFailure = null))
        }
        followJob = viewModelScope.launch {
            when (val result = repository.setArtistFollowed(artistId, target)) {
                is CollectionLoadResult.Available -> mutableUiState.update {
                    it.copy(follow = ArtistFollowUiState.Available(result.value))
                }
                is CollectionLoadResult.Failed -> {
                    if (result.failure == ContentFailure.AuthenticationRequired) {
                        mutableUiState.update { it.copy(follow = ArtistFollowUiState.AuthenticationRequired) }
                        mutableLoginRequests.emit(Unit)
                    } else {
                        mutableUiState.update {
                            it.copy(follow = current.copy(isUpdating = false, updateFailure = result.failure))
                        }
                    }
                }
            }
        }
    }

    private fun loadFirstPage(key: ArtistPageKey) {
        if (artistKey == null) return
        jobs[key]?.cancel()
        mutableUiState.update { it.withPage(key.section, key.sort, ArtistPageUiState.Loading) }
        loadPage(key = key, page = 1)
    }

    private fun loadPage(key: ArtistPageKey, page: Int, isRefresh: Boolean = false, isLoadMore: Boolean = false) {
        val artistId = artistKey?.artistId ?: return
        jobs[key] = viewModelScope.launch {
            val result = when (key.section) {
                ArtistSection.SONGS -> repository.loadArtistSongs(
                    artistId = artistId,
                    page = page,
                    newestFirst = key.sort == ArtistSort.LATEST,
                ).mapValue { artistPage ->
                    ArtistLoadPage(
                        items = artistPage.songs.map(ArtistItem::Song),
                        page = artistPage.page,
                        total = artistPage.total,
                        hasMore = artistPage.hasMore,
                        info = artistPage.info,
                    )
                }
                ArtistSection.ALBUMS -> repository.loadArtistAlbums(
                    artistId = artistId,
                    page = page,
                    newestFirst = key.sort == ArtistSort.LATEST,
                ).mapValue { albumPage ->
                    ArtistLoadPage(
                        items = albumPage.albums.map(ArtistItem::Album),
                        page = albumPage.page,
                        total = albumPage.total,
                        hasMore = albumPage.hasMore,
                    )
                }
                ArtistSection.MVS -> repository.loadArtistVideos(
                    artistId = artistId,
                    page = page,
                ).mapValue { videoPage ->
                    ArtistLoadPage(
                        items = videoPage.videos.map(ArtistItem::Video),
                        page = videoPage.page,
                        total = videoPage.total,
                        hasMore = videoPage.hasMore,
                    )
                }
            }
            when (result) {
                is CollectionLoadResult.Available -> applyAvailable(key, result.value, isLoadMore)
                is CollectionLoadResult.Failed -> applyFailure(key, result.failure, isRefresh, isLoadMore)
            }
        }
    }

    private fun applyAvailable(key: ArtistPageKey, loaded: ArtistLoadPage, isLoadMore: Boolean) {
        mutableUiState.update { state ->
            val profile = state.profile.merge(loaded.info)
            if (isLoadMore) {
                val current = state.page(key.section, key.sort) as? ArtistPageUiState.Content ?: return@update state
                val existingIds = current.items.mapTo(mutableSetOf()) { it.stableId }
                val items = current.items + loaded.items.filter { existingIds.add(it.stableId) }
                return@update state.copy(profile = profile).withPage(
                    key.section,
                    key.sort,
                    current.copy(
                        items = items,
                        page = loaded.page,
                        total = maxOf(current.total ?: 0, loaded.total ?: 0, items.size),
                        hasMore = loaded.hasMore,
                        isLoadingMore = false,
                        loadMoreFailure = null,
                    ),
                )
            }
            val items = loaded.items.distinctBy(ArtistItem::stableId)
            val pageState = if (items.isEmpty()) {
                ArtistPageUiState.Empty
            } else {
                ArtistPageUiState.Content(
                    items = items,
                    page = loaded.page,
                    total = maxOf(loaded.total ?: 0, profile.totalHint(key.section), items.size),
                    hasMore = loaded.hasMore,
                )
            }
            state.copy(profile = profile).withPage(key.section, key.sort, pageState)
        }
    }

    private fun applyFailure(key: ArtistPageKey, failure: ContentFailure, isRefresh: Boolean, isLoadMore: Boolean) {
        mutableUiState.update { state ->
            val current = state.page(key.section, key.sort) as? ArtistPageUiState.Content
            when {
                isRefresh && current != null -> state.withPage(
                    key.section,
                    key.sort,
                    current.copy(isRefreshing = false, refreshFailure = failure),
                )
                isLoadMore && current != null -> state.withPage(
                    key.section,
                    key.sort,
                    current.copy(isLoadingMore = false, loadMoreFailure = failure),
                )
                else -> state.withPage(key.section, key.sort, ArtistPageUiState.Error(failure))
            }
        }
    }

    private fun selectedPageKey(): ArtistPageKey {
        val state = mutableUiState.value
        return ArtistPageKey(state.selectedSection, state.selectedSort)
    }
}

private data class ArtistPageKey(val section: ArtistSection, val sort: ArtistSort)

private data class ArtistLoadPage(
    val items: List<ArtistItem>,
    val page: Int,
    val total: Int?,
    val hasMore: Boolean,
    val info: ArtistInfo? = null,
)

private fun <T, R> CollectionLoadResult<T>.mapValue(transform: (T) -> R): CollectionLoadResult<R> = when (this) {
    is CollectionLoadResult.Available -> CollectionLoadResult.Available(transform(value))
    is CollectionLoadResult.Failed -> this
}

internal fun ArtistNavKey.toProfile() = ArtistProfile(
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
    val current = requireNotNull(this)
    return ArtistProfile(
        id = current.id,
        name = info.name.nonBlank() ?: current.name,
        avatarUrl = info.avatarUrl.nonBlank() ?: current.avatarUrl,
        intro = info.intro.nonBlank() ?: current.intro,
        songCount = info.songCount,
        albumCount = info.albumCount,
        mvCount = info.mvCount,
        fansCount = info.fansCount,
    )
}

private fun ArtistProfile?.totalHint(section: ArtistSection): Int = when (section) {
    ArtistSection.SONGS -> this?.songCount
    ArtistSection.ALBUMS -> this?.albumCount
    ArtistSection.MVS -> this?.mvCount
} ?: 0

private fun String?.nonBlank(): String? = this?.takeIf(String::isNotBlank)
