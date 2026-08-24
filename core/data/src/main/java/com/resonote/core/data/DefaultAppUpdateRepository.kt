package com.resonote.core.data

import com.resonote.core.model.AppRelease
import com.resonote.core.network.AppUpdateNetworkDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultAppUpdateRepository @Inject constructor(
    private val network: AppUpdateNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
) : AppUpdateRepository {
    override suspend fun latestRelease() = loadCollection(riskChallenges) {
        val release = network.latestRelease()
        AppRelease(version = release.version, releaseUrl = release.releaseUrl)
    }
}
