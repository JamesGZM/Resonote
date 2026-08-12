package com.resonote.core.model

data class UserProfile(
    val userId: String,
    val nickname: String,
    val avatarUrl: String?,
    val backgroundUrl: String?,
    val signature: String,
    val fans: Long,
    val follows: Long,
    val listenMinutes: Long,
    val isVip: Boolean,
    val vipLabel: String,
)

data class UserPlaylist(
    val listId: String,
    val globalId: String,
    val name: String,
    val coverUrl: String?,
    val count: Long,
    val isMine: Boolean,
    val isLike: Boolean,
)

data class PlaylistTrackInput(
    val hash: String,
    val title: String,
    val artist: String,
    val albumId: String? = null,
    val albumAudioId: String? = null,
)
