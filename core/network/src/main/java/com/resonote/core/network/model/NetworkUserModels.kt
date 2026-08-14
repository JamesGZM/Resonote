package com.resonote.core.network.model

data class NetworkUserDetail(
    val userId: String,
    val nickname: String,
    val avatarUrl: String?,
    val backgroundUrl: String?,
    val signature: String,
    val fans: Long,
    val follows: Long,
    val listenMinutes: Long,
)

data class NetworkUserVip(val isVip: Boolean, val label: String)

data class NetworkUserPlaylist(
    val listId: String,
    val globalId: String,
    val name: String,
    val coverUrl: String?,
    val count: Long,
    val isMine: Boolean,
    val isLike: Boolean,
)

data class NetworkPlaylistTrackInput(
    val hash: String,
    val title: String,
    val artist: String,
    val albumId: String?,
    val albumAudioId: String?,
)
