package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.OnlineSong
import com.resonote.core.network.ApiException
import com.resonote.core.network.ListeningHistoryNetworkDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultListeningHistoryRepository @Inject constructor(
    private val network: ListeningHistoryNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
) : ListeningHistoryRepository {
    override suspend fun loadAccountHistory(): CollectionLoadResult<List<OnlineSong>> =
        try {
            CollectionLoadResult.Available(network.accountHistory().map { it.toOnlineSong() })
        } catch (failure: ApiException) {
            CollectionLoadResult.Failed(failure.toContentFailure(riskChallenges))
        }
}
