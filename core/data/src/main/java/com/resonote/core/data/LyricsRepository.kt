package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.LyricLine

interface LyricsRepository {
    suspend fun loadLyrics(hash: String, albumAudioId: String? = null): CollectionLoadResult<List<LyricLine>>
}
