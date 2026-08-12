package com.resonote.feature.discover.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.ContentCatalogRepository
import com.resonote.core.data.RankingRepository
import com.resonote.core.model.AlbumRegion
import com.resonote.core.model.CollectionLoadResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val catalogRepository: ContentCatalogRepository,
    private val rankingRepository: RankingRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = mutableUiState.asStateFlow()

    private var categoriesJob: Job? = null
    private var playlistsJob: Job? = null
    private var rankingsJob: Job? = null
    private var albumsJob: Job? = null
    private var songsJob: Job? = null

    init {
        loadCategories()
        loadPlaylists(categoryId = 0, page = 1)
    }

    fun selectSection(section: DiscoverSection) {
        mutableUiState.update { it.copy(selectedSection = section) }
        when (section) {
            DiscoverSection.PLAYLISTS -> if (mutableUiState.value.playlists is DiscoverPageState.Idle) {
                loadPlaylists(mutableUiState.value.selectedPlaylistCategoryId, page = 1)
            }
            DiscoverSection.RANKINGS -> if (mutableUiState.value.rankings is DiscoverLoadState.Idle) loadRankings()
            DiscoverSection.ALBUMS -> if (mutableUiState.value.albums is DiscoverLoadState.Idle) loadAlbums()
            DiscoverSection.SONGS -> if (mutableUiState.value.songs is DiscoverPageState.Idle) loadSongs(page = 1)
        }
    }

    fun selectPlaylistParent(parentId: Int?) {
        val categories = (mutableUiState.value.categories as? DiscoverLoadState.Content)?.value.orEmpty()
        val parent = parentId?.let { id -> categories.firstOrNull { it.tagId == id } }
        if (parentId != null && parent == null) return
        val categoryId = parent?.children?.firstOrNull()?.tagId ?: parent?.tagId ?: 0
        mutableUiState.update {
            it.copy(selectedParentCategoryId = parentId, selectedPlaylistCategoryId = categoryId)
        }
        loadPlaylists(categoryId, page = 1)
    }

    fun selectPlaylistCategory(categoryId: Int) {
        val state = mutableUiState.value
        val categories = (state.categories as? DiscoverLoadState.Content)?.value.orEmpty()
        val parent = categories.firstOrNull { it.tagId == state.selectedParentCategoryId } ?: return
        if (parent.children.none { it.tagId == categoryId }) return
        mutableUiState.update { it.copy(selectedPlaylistCategoryId = categoryId) }
        loadPlaylists(categoryId, page = 1)
    }

    fun selectAlbumRegion(region: AlbumRegion?) {
        mutableUiState.update { it.copy(selectedAlbumRegion = region) }
    }

    fun retryCategories() = loadCategories()

    fun retryCurrent() {
        when (mutableUiState.value.selectedSection) {
            DiscoverSection.PLAYLISTS -> loadPlaylists(mutableUiState.value.selectedPlaylistCategoryId, page = 1)
            DiscoverSection.RANKINGS -> loadRankings()
            DiscoverSection.ALBUMS -> loadAlbums()
            DiscoverSection.SONGS -> loadSongs(page = 1)
        }
    }

    fun loadMore() {
        when (mutableUiState.value.selectedSection) {
            DiscoverSection.PLAYLISTS -> {
                val page = mutableUiState.value.playlists as? DiscoverPageState.Content ?: return
                if (!page.hasMore || page.isLoadingMore || playlistsJob?.isActive == true) return
                loadPlaylists(mutableUiState.value.selectedPlaylistCategoryId, page.page + 1)
            }
            DiscoverSection.SONGS -> {
                val page = mutableUiState.value.songs as? DiscoverPageState.Content ?: return
                if (!page.hasMore || page.isLoadingMore || songsJob?.isActive == true) return
                loadSongs(page.page + 1)
            }
            DiscoverSection.RANKINGS,
            DiscoverSection.ALBUMS,
            -> Unit
        }
    }

    private fun loadCategories() {
        categoriesJob?.cancel()
        mutableUiState.update { it.copy(categories = DiscoverLoadState.Loading) }
        categoriesJob = viewModelScope.launch {
            when (val result = catalogRepository.loadPlaylistCategories()) {
                is CollectionLoadResult.Available -> mutableUiState.update {
                    it.copy(
                        categories = if (result.value.isEmpty()) {
                            DiscoverLoadState.Empty
                        } else {
                            DiscoverLoadState.Content(result.value)
                        },
                    )
                }
                is CollectionLoadResult.Failed -> mutableUiState.update {
                    it.copy(categories = DiscoverLoadState.Error(result.failure))
                }
            }
        }
    }

    private fun loadPlaylists(categoryId: Int, page: Int) {
        playlistsJob?.cancel()
        val current = mutableUiState.value.playlists as? DiscoverPageState.Content
        mutableUiState.update {
            it.copy(
                playlists = if (page == 1) {
                    DiscoverPageState.Loading
                } else {
                    requireNotNull(current).copy(isLoadingMore = true, loadMoreFailure = null)
                },
            )
        }
        playlistsJob = viewModelScope.launch {
            when (val result = catalogRepository.loadCategoryPlaylists(categoryId, page, PAGE_SIZE)) {
                is CollectionLoadResult.Available -> mutableUiState.update { state ->
                    if (state.selectedPlaylistCategoryId != categoryId) return@update state
                    val items = if (page == 1) {
                        result.value.distinctBy { it.id }
                    } else {
                        val latest = state.playlists as? DiscoverPageState.Content ?: return@update state
                        val seen = latest.items.mapTo(mutableSetOf()) { it.id }
                        latest.items + result.value.filter { seen.add(it.id) }
                    }
                    state.copy(
                        playlists = if (items.isEmpty()) {
                            DiscoverPageState.Empty
                        } else {
                            DiscoverPageState.Content(
                                items = items,
                                page = page,
                                hasMore = result.value.size >= PAGE_SIZE,
                            )
                        },
                    )
                }
                is CollectionLoadResult.Failed -> mutableUiState.update { state ->
                    if (state.selectedPlaylistCategoryId != categoryId) return@update state
                    state.copy(
                        playlists = if (page == 1) {
                            DiscoverPageState.Error(result.failure)
                        } else {
                            val latest = state.playlists as? DiscoverPageState.Content ?: return@update state
                            latest.copy(isLoadingMore = false, loadMoreFailure = result.failure)
                        },
                    )
                }
            }
        }
    }

    private fun loadRankings() {
        rankingsJob?.cancel()
        mutableUiState.update { it.copy(rankings = DiscoverLoadState.Loading) }
        rankingsJob = viewModelScope.launch {
            when (val result = rankingRepository.loadRankings()) {
                is CollectionLoadResult.Available -> mutableUiState.update {
                    it.copy(
                        rankings = if (result.value.isEmpty()) {
                            DiscoverLoadState.Empty
                        } else {
                            DiscoverLoadState.Content(result.value.distinctBy { ranking -> ranking.id })
                        },
                    )
                }
                is CollectionLoadResult.Failed -> mutableUiState.update {
                    it.copy(rankings = DiscoverLoadState.Error(result.failure))
                }
            }
        }
    }

    private fun loadAlbums() {
        albumsJob?.cancel()
        mutableUiState.update { it.copy(albums = DiscoverLoadState.Loading) }
        albumsJob = viewModelScope.launch {
            when (val result = catalogRepository.loadNewAlbums(page = 1, pageSize = PAGE_SIZE)) {
                is CollectionLoadResult.Available -> mutableUiState.update {
                    it.copy(
                        albums = if (result.value.isEmpty()) {
                            DiscoverLoadState.Empty
                        } else {
                            DiscoverLoadState.Content(result.value.distinctBy { album -> album.id })
                        },
                    )
                }
                is CollectionLoadResult.Failed -> mutableUiState.update {
                    it.copy(albums = DiscoverLoadState.Error(result.failure))
                }
            }
        }
    }

    private fun loadSongs(page: Int) {
        songsJob?.cancel()
        val current = mutableUiState.value.songs as? DiscoverPageState.Content
        mutableUiState.update {
            it.copy(
                songs = if (page == 1) {
                    DiscoverPageState.Loading
                } else {
                    requireNotNull(current).copy(isLoadingMore = true, loadMoreFailure = null)
                },
            )
        }
        songsJob = viewModelScope.launch {
            when (val result = catalogRepository.loadNewSongs(page, PAGE_SIZE)) {
                is CollectionLoadResult.Available -> mutableUiState.update { state ->
                    val items = if (page == 1) {
                        result.value.songs.distinctBy { it.hash }
                    } else {
                        val latest = state.songs as? DiscoverPageState.Content ?: return@update state
                        val seen = latest.items.mapTo(mutableSetOf()) { it.hash }
                        latest.items + result.value.songs.filter { seen.add(it.hash) }
                    }
                    state.copy(
                        songs = if (items.isEmpty()) {
                            DiscoverPageState.Empty
                        } else {
                            DiscoverPageState.Content(items, page, result.value.hasMore)
                        },
                    )
                }
                is CollectionLoadResult.Failed -> mutableUiState.update { state ->
                    state.copy(
                        songs = if (page == 1) {
                            DiscoverPageState.Error(result.failure)
                        } else {
                            val latest = state.songs as? DiscoverPageState.Content ?: return@update state
                            latest.copy(isLoadingMore = false, loadMoreFailure = result.failure)
                        },
                    )
                }
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}
