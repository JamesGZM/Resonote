package com.resonote.core.model

data class Banner(val id: String, val title: String?, val imageUrl: String, val linkUrl: String?)

data class PlaylistCategory(val tagId: Int, val name: String, val children: List<PlaylistCategory>)

enum class AlbumRegion { Chinese, Western, Japanese, Korean }

data class Album(
    val id: String,
    val name: String,
    val artist: String?,
    val coverUrl: String?,
    val publishDate: String,
    val songCount: Int,
    val region: AlbumRegion,
)

data class CatalogSongPage(val songs: List<OnlineSong>, val page: Int, val total: Int, val hasMore: Boolean)

data class ArtistInfo(
    val name: String,
    val avatarUrl: String?,
    val intro: String,
    val songCount: Int,
    val albumCount: Int,
    val mvCount: Int,
    val fansCount: Long,
)

data class ArtistSongsPage(
    val info: ArtistInfo?,
    val songs: List<OnlineSong>,
    val page: Int,
    val total: Int,
    val hasMore: Boolean,
)
