package com.resonote.core.network

import com.resonote.core.network.model.NetworkComplexSearch
import com.resonote.core.network.model.NetworkSearchAlbum
import com.resonote.core.network.model.NetworkSearchArtist
import com.resonote.core.network.model.NetworkSearchKeyword
import com.resonote.core.network.model.NetworkSearchMv
import com.resonote.core.network.model.NetworkSearchPage
import com.resonote.core.network.model.NetworkSearchPlaylist
import com.resonote.core.network.model.NetworkSearchResultPage

interface SearchNetworkDataSource {
    suspend fun searchSongs(keywords: String, page: Int = 1, pageSize: Int = 30): NetworkSearchPage
    suspend fun searchPlaylists(keywords: String, page: Int = 1, pageSize: Int = 30): NetworkSearchResultPage<NetworkSearchPlaylist>
    suspend fun searchAlbums(keywords: String, page: Int = 1, pageSize: Int = 30): NetworkSearchResultPage<NetworkSearchAlbum>
    suspend fun searchArtists(keywords: String, page: Int = 1, pageSize: Int = 30): NetworkSearchResultPage<NetworkSearchArtist>
    suspend fun searchMvs(keywords: String, page: Int = 1, pageSize: Int = 30): NetworkSearchResultPage<NetworkSearchMv>
    suspend fun searchComplex(keywords: String): NetworkComplexSearch
    suspend fun hotSearchKeywords(): List<NetworkSearchKeyword>
    suspend fun searchSuggestions(keywords: String): List<String>
}
