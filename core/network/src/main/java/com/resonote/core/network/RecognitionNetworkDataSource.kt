package com.resonote.core.network

import com.resonote.core.network.model.NetworkRecognitionMatch

interface RecognitionNetworkDataSource {
    suspend fun recognizeAudio(pcm: ByteArray): List<NetworkRecognitionMatch>
}
