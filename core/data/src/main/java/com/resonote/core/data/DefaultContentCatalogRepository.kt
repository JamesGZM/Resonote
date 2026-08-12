package com.resonote.core.data

import com.resonote.core.model.Album
import com.resonote.core.model.AlbumRegion
import com.resonote.core.model.ArtistInfo
import com.resonote.core.model.ArtistSongsPage
import com.resonote.core.model.Banner
import com.resonote.core.model.CatalogSongPage
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.PlaylistCategory
import com.resonote.core.model.PlaylistSummary
import com.resonote.core.model.SongPage
import com.resonote.core.network.ApiException
import com.resonote.core.network.CatalogNetworkDataSource
import com.resonote.core.network.HomeNetworkDataSource
import com.resonote.core.network.model.NetworkAlbumRegion
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
internal class DefaultContentCatalogRepository @Inject constructor(
    private val network: CatalogNetworkDataSource,
    private val homeNetwork: HomeNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
) : ContentCatalogRepository {
    override suspend fun loadBanners() = loadCollection(riskChallenges) {
        network.banners().map { Banner(it.id, it.title, it.imageUrl.replace("{size}", "720"), it.linkUrl) }
    }

    override suspend fun loadPlaylistCategories() = loadCollection(riskChallenges) {
        network.playlistCategories().map { it.toDomain() }
    }

    override suspend fun loadCategoryPlaylists(categoryId: Int, page: Int, pageSize: Int) = loadCollection(riskChallenges) {
        require(categoryId >= 0) { "categoryId must not be negative" }
        validateCollectionPage(page, pageSize)
        network.categoryPlaylists(categoryId, page, pageSize).map {
            PlaylistSummary(it.id, it.title, it.coverUrl?.replace("{size}", "480"), it.playCount)
        }
    }

    override suspend fun loadNewAlbums(page: Int, pageSize: Int) = loadCollection(riskChallenges) {
        validateCollectionPage(page, pageSize)
        network.newAlbums(page, pageSize).map {
            Album(it.id, it.name, it.artist, it.coverUrl?.replace("{size}", "480"), it.publishDate, it.songCount, it.region.toDomain())
        }
    }

    override suspend fun loadNewSongs(page: Int, pageSize: Int) = loadCollection(riskChallenges) {
        validateCollectionPage(page, pageSize)
        val songs = homeNetwork.newSongs(page, pageSize).map { it.toOnlineSong() }
        SongPage(songs, page, total = null, hasMore = songs.size >= pageSize)
    }

    override suspend fun loadAlbumSongs(albumId: String, page: Int, pageSize: Int) = loadCollection(riskChallenges) {
        require(albumId.isNotBlank()) { "albumId must not be blank" }
        validateCollectionPage(page, pageSize)
        val result = network.albumSongs(albumId, page, pageSize)
        CatalogSongPage(result.songs.map { it.toOnlineSong() }, page, result.total, result.hasMore)
    }

    override suspend fun loadArtistDetail(artistId: String) = loadCollection(riskChallenges) {
        require(artistId.isNotBlank()) { "artistId must not be blank" }
        network.artistDetail(artistId)?.toDomain()
    }

    override suspend fun loadArtistSongs(artistId: String, page: Int, pageSize: Int, newestFirst: Boolean) = loadCollection(riskChallenges) {
        require(artistId.isNotBlank()) { "artistId must not be blank" }
        validateCollectionPage(page, pageSize)
        val info = if (page == 1) try { network.artistDetail(artistId)?.toDomain() } catch (cancel: CancellationException) { throw cancel } catch (_: ApiException) { null } else null
        val result = network.artistSongs(artistId, page, pageSize, newestFirst)
        ArtistSongsPage(info, result.songs.map { it.toOnlineSong() }, page, info?.songCount ?: 0, result.hasMore)
    }

    private fun com.resonote.core.network.model.NetworkPlaylistCategory.toDomain(): PlaylistCategory =
        PlaylistCategory(tagId, name, children.map { it.toDomain() })

    private fun com.resonote.core.network.model.NetworkArtistInfo.toDomain() =
        ArtistInfo(name, avatarUrl?.replace("{size}", "480"), intro, songCount, albumCount, mvCount, fansCount)

    private fun NetworkAlbumRegion.toDomain() = when (this) {
        NetworkAlbumRegion.Chinese -> AlbumRegion.Chinese
        NetworkAlbumRegion.Western -> AlbumRegion.Western
        NetworkAlbumRegion.Japanese -> AlbumRegion.Japanese
        NetworkAlbumRegion.Korean -> AlbumRegion.Korean
    }
}
