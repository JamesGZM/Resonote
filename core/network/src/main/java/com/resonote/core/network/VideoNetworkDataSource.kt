package com.resonote.core.network

interface VideoNetworkDataSource {
    suspend fun resolveVideoUrl(hash: String): String?
}
