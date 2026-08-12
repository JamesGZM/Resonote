package com.resonote.core.data

import com.resonote.core.model.ComplexSearchResult
import com.resonote.core.model.SearchAlbum
import com.resonote.core.model.SearchArtist
import com.resonote.core.model.SearchKeyword
import com.resonote.core.model.SearchMv
import com.resonote.core.model.SearchPlaylist
import com.resonote.core.model.SearchPage
import com.resonote.core.network.SearchNetworkDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultSearchRepository @Inject constructor(
    private val searchNetwork: SearchNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
) : SearchRepository {
    override suspend fun searchSongs(keywords: String, page: Int, pageSize: Int) = searchPage(keywords, page, pageSize) {
        val value = searchNetwork.searchSongs(it, page, pageSize)
        SearchPage(value.items.map { song -> song.toOnlineSong() }, page, value.total, hasMore(value.total, page, pageSize, value.items.size))
    }

    override suspend fun searchPlaylists(keywords: String, page: Int, pageSize: Int) = searchPage(keywords, page, pageSize) {
        val value = searchNetwork.searchPlaylists(it, page, pageSize)
        SearchPage(value.items.map { item -> SearchPlaylist(item.id, item.name, item.creator, item.coverUrl?.size(480), item.songCount, item.playCount) }, page, value.total, value.hasMore)
    }

    override suspend fun searchAlbums(keywords: String, page: Int, pageSize: Int) = searchPage(keywords, page, pageSize) {
        val value = searchNetwork.searchAlbums(it, page, pageSize)
        SearchPage(value.items.map { item -> SearchAlbum(item.id, item.name, item.artist, item.coverUrl?.size(480), item.songCount, item.publishDate) }, page, value.total, value.hasMore)
    }

    override suspend fun searchArtists(keywords: String, page: Int, pageSize: Int) = searchPage(keywords, page, pageSize) {
        val value = searchNetwork.searchArtists(it, page, pageSize)
        SearchPage(value.items.map { item -> SearchArtist(item.id, item.name, item.avatarUrl?.size(480), item.albumCount, item.songCount) }, page, value.total, value.hasMore)
    }

    override suspend fun searchMvs(keywords: String, page: Int, pageSize: Int) = searchPage(keywords, page, pageSize) {
        val value = searchNetwork.searchMvs(it, page, pageSize)
        SearchPage(value.items.map { item -> SearchMv(item.hash, item.name, item.singer, item.coverUrl?.size(480), item.durationMillis) }, page, value.total, value.hasMore)
    }

    override suspend fun searchComplex(keywords: String) = loadCollection(riskChallenges) {
        require(keywords.isNotBlank()) { "keywords must not be blank" }
        val value = searchNetwork.searchComplex(keywords.trim())
        ComplexSearchResult(
            value.artists.map { SearchArtist(it.id, it.name, it.avatarUrl?.size(480), it.albumCount, it.songCount) },
            value.songs.map { it.toOnlineSong() }, value.songsTotal,
            value.albums.map { SearchAlbum(it.id, it.name, it.artist, it.coverUrl?.size(480), it.songCount, it.publishDate) }, value.albumsTotal,
            value.playlists.map { SearchPlaylist(it.id, it.name, it.creator, it.coverUrl?.size(480), it.songCount, it.playCount) }, value.playlistsTotal,
            value.mvs.map { SearchMv(it.hash, it.name, it.singer, it.coverUrl?.size(480), it.durationMillis) }, value.mvsTotal,
        )
    }

    override suspend fun loadHotKeywords() = loadCollection(riskChallenges) {
        searchNetwork.hotSearchKeywords().map { SearchKeyword(it.keyword, it.reason) }
    }

    override suspend fun loadSuggestions(keywords: String) = loadCollection(riskChallenges) {
        require(keywords.isNotBlank()) { "keywords must not be blank" }
        searchNetwork.searchSuggestions(keywords.trim())
    }

    private suspend fun <T> searchPage(
        keywords: String,
        page: Int,
        pageSize: Int,
        block: suspend (String) -> SearchPage<T>,
    ) = loadCollection(riskChallenges) {
        require(keywords.isNotBlank()) { "keywords must not be blank" }
        validateCollectionPage(page, pageSize)
        block(keywords.trim())
    }

    private fun hasMore(total: Int, page: Int, pageSize: Int, received: Int) =
        if (total > 0) page.toLong() * pageSize < total else received >= pageSize

    private fun String.size(value: Int) = replace("{size}", value.toString())

}
