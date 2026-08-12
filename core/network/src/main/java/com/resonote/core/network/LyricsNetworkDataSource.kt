package com.resonote.core.network

import com.resonote.core.network.model.NetworkLyricCandidate

interface LyricsNetworkDataSource {
    suspend fun searchLyric(hash: String, albumAudioId: String? = null): NetworkLyricCandidate?
    suspend fun downloadLyric(candidate: NetworkLyricCandidate): String?
}
