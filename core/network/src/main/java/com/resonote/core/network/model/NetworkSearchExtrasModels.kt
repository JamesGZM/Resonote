package com.resonote.core.network.model

data class NetworkSearchKeyword(val keyword: String, val reason: String)
data class NetworkSearchArtist(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val albumCount: Int,
    val songCount: Int,
)
data class NetworkSearchAlbum(
    val id: String,
    val name: String,
    val artist: String?,
    val coverUrl: String?,
    val songCount: Int,
    val publishDate: String,
)
data class NetworkSearchPlaylist(
    val id: String,
    val name: String,
    val creator: String?,
    val coverUrl: String?,
    val songCount: Int,
    val playCount: Long,
)
data class NetworkSearchMv(
    val hash: String,
    val name: String,
    val singer: String?,
    val coverUrl: String?,
    val durationMillis: Long,
)
data class NetworkSearchResultPage<T>(val items: List<T>, val total: Int, val hasMore: Boolean)
data class NetworkComplexSearch(
    val artists: List<NetworkSearchArtist>,
    val songs: List<NetworkSong>,
    val songsTotal: Int,
    val albums: List<NetworkSearchAlbum>,
    val albumsTotal: Int,
    val playlists: List<NetworkSearchPlaylist>,
    val playlistsTotal: Int,
    val mvs: List<NetworkSearchMv>,
    val mvsTotal: Int,
)
data class NetworkLyricCandidate(val id: String, val accessKey: String)
data class NetworkRecognitionMatch(val confidence: Double, val song: NetworkSong)
