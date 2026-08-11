package com.resonote.core.network.retrofit

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.protocol.ApiDeviceIdentityFactory
import com.resonote.core.network.protocol.ApiProtocolConfig
import com.resonote.core.network.protocol.ApiProtocolInterceptor
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import com.resonote.core.network.risk.ApiRiskVerifier
import com.resonote.core.network.risk.RiskAwareApiExecutor
import java.time.Clock
import java.util.Optional
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.Retrofit

class LiveApiSearchCanaryTest {
    @Test
    fun currentServiceAcceptsSignedAnonymousSearchAndReturnsSongs() = runBlocking {
        assumeTrue("Live API tests are opt-in", System.getenv(LIVE_TEST_ENV) == "true")
        val json = Json { ignoreUnknownKeys = true }
        val identity = ApiDeviceIdentityFactory().create()
        val client =
            OkHttpClient.Builder()
                .addInterceptor(ApiProtocolInterceptor(Clock.systemUTC(), identity, ApiRequestSigner()))
                .build()
        val retrofit = Retrofit.Builder().baseUrl(ApiProtocolConfig.BASE_URL).client(client).build()
        val executor = RiskAwareApiExecutor(ApiRiskChallengeDetector(), Optional.empty<ApiRiskVerifier>())
        val dataSource = RetrofitApiNetworkDataSource(retrofit, json, executor)

        val result = dataSource.searchSongs("周杰伦", page = 1, pageSize = 5)

        assertThat(result.items).isNotEmpty()
        assertThat(result.total).isAtLeast(result.items.size)
    }

    private companion object {
        const val LIVE_TEST_ENV = "RESONOTE_RUN_LIVE_API_TESTS"
    }
}
