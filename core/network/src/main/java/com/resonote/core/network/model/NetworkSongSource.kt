package com.resonote.core.network.model

data class NetworkSongSource(
    val uri: String,
    val durationMillis: Long,
    val extension: String?,
    val isPreview: Boolean = false,
)
