package com.resonote.core.network

import com.resonote.core.network.model.NetworkAlbum
import com.resonote.core.network.model.NetworkAlbumSongPage
import com.resonote.core.network.model.NetworkArtistAlbumPage
import com.resonote.core.network.model.NetworkArtistInfo
import com.resonote.core.network.model.NetworkArtistSongPage
import com.resonote.core.network.model.NetworkArtistVideoPage
import com.resonote.core.network.model.NetworkBanner
import com.resonote.core.network.model.NetworkPlaylistCategory
import com.resonote.core.network.model.NetworkPlaylistSummary

interface CatalogNetworkDataSource {
    suspend fun recommendedPlaylists(page: Int = 1, pageSize: Int = 6): List<NetworkPlaylistSummary>
    suspend fun categoryPlaylists(categoryId: Int, page: Int = 1, pageSize: Int = 30): List<NetworkPlaylistSummary>
    suspend fun banners(): List<NetworkBanner>
    suspend fun playlistCategories(): List<NetworkPlaylistCategory>
    suspend fun newAlbums(page: Int = 1, pageSize: Int = 30): List<NetworkAlbum>
    suspend fun albumSongs(albumId: String, page: Int = 1, pageSize: Int = 30): NetworkAlbumSongPage
    suspend fun artistDetail(artistId: String): NetworkArtistInfo?
    suspend fun artistSongs(
        artistId: String,
        page: Int = 1,
        pageSize: Int = 30,
        newestFirst: Boolean = false,
    ): NetworkArtistSongPage
    suspend fun artistAlbums(
        artistId: String,
        page: Int = 1,
        pageSize: Int = 30,
        newestFirst: Boolean = false,
    ): NetworkArtistAlbumPage = error("artistAlbums is not implemented")
    suspend fun artistVideos(artistId: String, page: Int = 1, pageSize: Int = 30): NetworkArtistVideoPage =
        error("artistVideos is not implemented")
    suspend fun isArtistFollowed(artistId: String): Boolean = error("isArtistFollowed is not implemented")
    suspend fun setArtistFollowed(artistId: String, followed: Boolean): Unit =
        error("setArtistFollowed is not implemented")
}
