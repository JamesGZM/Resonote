package com.resonote.core.network.model

data class NetworkBanner(val id: String, val title: String?, val imageUrl: String, val linkUrl: String?)

data class NetworkPlaylistCategory(val tagId: Int, val name: String, val children: List<NetworkPlaylistCategory>)

enum class NetworkAlbumRegion { Chinese, Western, Japanese, Korean }

data class NetworkAlbum(
    val id: String,
    val name: String,
    val artist: String?,
    val coverUrl: String?,
    val publishDate: String,
    val songCount: Int,
    val region: NetworkAlbumRegion,
)

data class NetworkAlbumSongPage(val songs: List<NetworkSong>, val total: Int, val hasMore: Boolean)

data class NetworkArtistInfo(
    val name: String,
    val avatarUrl: String?,
    val intro: String,
    val songCount: Int,
    val albumCount: Int,
    val mvCount: Int,
    val fansCount: Long,
)

data class NetworkArtistSongPage(val songs: List<NetworkSong>, val hasMore: Boolean)
