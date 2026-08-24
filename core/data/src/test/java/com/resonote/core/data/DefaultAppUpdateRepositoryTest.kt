package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AppRelease
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.network.AppUpdateNetworkDataSource
import com.resonote.core.network.model.NetworkAppRelease
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultAppUpdateRepositoryTest {
    @Test
    fun latestReleaseMapsNetworkModel() = runTest {
        val repository = DefaultAppUpdateRepository(
            network = object : AppUpdateNetworkDataSource {
                override suspend fun latestRelease() = NetworkAppRelease("v0.2.0", "https://github.com/release")
            },
            riskChallenges = RiskChallengeRegistry(),
        )

        assertThat(repository.latestRelease()).isEqualTo(
            CollectionLoadResult.Available(AppRelease("v0.2.0", "https://github.com/release")),
        )
    }
}
