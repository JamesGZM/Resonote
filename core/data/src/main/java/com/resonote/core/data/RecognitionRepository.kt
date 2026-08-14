package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.RecognitionMatch

interface RecognitionRepository {
    suspend fun recognizeAudio(pcm: ByteArray): CollectionLoadResult<List<RecognitionMatch>>
}
