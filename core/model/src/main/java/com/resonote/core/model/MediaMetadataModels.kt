package com.resonote.core.model

data class SearchKeyword(val keyword: String, val reason: String)
data class SearchArtist(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val albumCount: Int,
    val songCount: Int,
)
data class SearchAlbum(
    val id: String,
    val name: String,
    val artist: String?,
    val coverUrl: String?,
    val songCount: Int,
    val publishDate: String,
)
data class SearchPlaylist(
    val id: String,
    val name: String,
    val creator: String?,
    val coverUrl: String?,
    val songCount: Int,
    val playCount: Long,
)
data class SearchMv(
    val hash: String,
    val name: String,
    val singer: String?,
    val coverUrl: String?,
    val durationMillis: Long,
)
data class SearchPage<T>(val items: List<T>, val page: Int, val total: Int, val hasMore: Boolean)
data class ComplexSearchResult(
    val artists: List<SearchArtist>,
    val songs: List<OnlineSong>,
    val songsTotal: Int,
    val albums: List<SearchAlbum>,
    val albumsTotal: Int,
    val playlists: List<SearchPlaylist>,
    val playlistsTotal: Int,
    val mvs: List<SearchMv>,
    val mvsTotal: Int,
)
data class LyricLine(val timeMillis: Long, val text: String)
data class RecognitionMatch(val confidence: Double, val song: OnlineSong)
