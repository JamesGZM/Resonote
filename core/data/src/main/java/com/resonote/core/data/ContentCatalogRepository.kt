package com.resonote.core.data

import com.resonote.core.model.Album
import com.resonote.core.model.ArtistInfo
import com.resonote.core.model.ArtistSongsPage
import com.resonote.core.model.Banner
import com.resonote.core.model.CatalogSongPage
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.PlaylistCategory
import com.resonote.core.model.PlaylistSummary
import com.resonote.core.model.SongPage

interface ContentCatalogRepository {
    suspend fun loadBanners(): CollectionLoadResult<List<Banner>>
    suspend fun loadPlaylistCategories(): CollectionLoadResult<List<PlaylistCategory>>
    suspend fun loadCategoryPlaylists(
        categoryId: Int,
        page: Int = 1,
        pageSize: Int = 30,
    ): CollectionLoadResult<List<PlaylistSummary>>
    suspend fun loadNewAlbums(page: Int = 1, pageSize: Int = 30): CollectionLoadResult<List<Album>>
    suspend fun loadNewSongs(page: Int = 1, pageSize: Int = 30): CollectionLoadResult<SongPage>
    suspend fun loadAlbumSongs(albumId: String, page: Int = 1, pageSize: Int = 30): CollectionLoadResult<CatalogSongPage>
    suspend fun loadArtistDetail(artistId: String): CollectionLoadResult<ArtistInfo?>
    suspend fun loadArtistSongs(artistId: String, page: Int = 1, pageSize: Int = 30, newestFirst: Boolean = false): CollectionLoadResult<ArtistSongsPage>
}
