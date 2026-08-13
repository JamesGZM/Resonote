package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.Ranking
import com.resonote.core.model.SongPage
import com.resonote.core.network.ApiException
import com.resonote.core.network.RankingNetworkDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultRankingRepository @Inject constructor(
    private val network: RankingNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
) : RankingRepository {
    override suspend fun loadRankings(): CollectionLoadResult<List<Ranking>> =
        loadCollection(riskChallenges) {
            network.rankings().map { Ranking(it.id, it.title, it.coverUrl.toRemoteImageUrl(480)) }
        }

    override suspend fun loadSongs(rankId: String, page: Int, pageSize: Int): CollectionLoadResult<SongPage> {
        require(rankId.isNotBlank()) { "rankId must not be blank" }
        validateCollectionPage(page, pageSize)
        return loadCollection(riskChallenges) {
            val result = network.rankingSongs(rankId, page, pageSize)
            SongPage(result.songs.map { it.toOnlineSong() }, page, result.total, result.hasMore)
        }
    }
}

internal suspend fun <T> loadCollection(
    riskChallenges: RiskChallengeRegistry,
    block: suspend () -> T,
): CollectionLoadResult<T> =
    try {
        CollectionLoadResult.Available(block())
    } catch (failure: ApiException) {
        CollectionLoadResult.Failed(failure.toContentFailure(riskChallenges))
    }

internal fun validateCollectionPage(page: Int, pageSize: Int) {
    require(page > 0) { "page must be positive" }
    require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
}
