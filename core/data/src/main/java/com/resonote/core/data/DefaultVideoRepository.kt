package com.resonote.core.data

import com.resonote.core.network.VideoNetworkDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultVideoRepository @Inject constructor(
    private val network: VideoNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
) : VideoRepository {
    override suspend fun resolveVideoUrl(hash: String) = loadCollection(riskChallenges) {
        require(hash.isNotBlank()) { "hash must not be blank" }
        network.resolveVideoUrl(hash)
    }
}
