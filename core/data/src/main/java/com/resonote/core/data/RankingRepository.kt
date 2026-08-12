package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.Ranking
import com.resonote.core.model.SongPage

interface RankingRepository {
    suspend fun loadRankings(): CollectionLoadResult<List<Ranking>>

    suspend fun loadSongs(
        rankId: String,
        page: Int = 1,
        pageSize: Int = 30,
    ): CollectionLoadResult<SongPage>
}
