package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ComplexSearchResult
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.SearchAlbum
import com.resonote.core.model.SearchArtist
import com.resonote.core.model.SearchKeyword
import com.resonote.core.model.SearchMv
import com.resonote.core.model.SearchPage
import com.resonote.core.model.SearchPlaylist

interface SearchRepository {
    suspend fun searchSongs(
        keywords: String,
        page: Int = 1,
        pageSize: Int = 30,
    ): CollectionLoadResult<SearchPage<OnlineSong>>
    suspend fun searchPlaylists(
        keywords: String,
        page: Int = 1,
        pageSize: Int = 30,
    ): CollectionLoadResult<SearchPage<SearchPlaylist>>
    suspend fun searchAlbums(
        keywords: String,
        page: Int = 1,
        pageSize: Int = 30,
    ): CollectionLoadResult<SearchPage<SearchAlbum>>
    suspend fun searchArtists(
        keywords: String,
        page: Int = 1,
        pageSize: Int = 30,
    ): CollectionLoadResult<SearchPage<SearchArtist>>
    suspend fun searchMvs(
        keywords: String,
        page: Int = 1,
        pageSize: Int = 30,
    ): CollectionLoadResult<SearchPage<SearchMv>>
    suspend fun searchComplex(keywords: String): CollectionLoadResult<ComplexSearchResult>
    suspend fun loadHotKeywords(): CollectionLoadResult<List<SearchKeyword>>
    suspend fun loadSuggestions(keywords: String): CollectionLoadResult<List<String>>
}
