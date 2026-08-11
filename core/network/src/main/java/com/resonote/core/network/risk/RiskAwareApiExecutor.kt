package com.resonote.core.network.risk

import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.retrofit.ApiRawResponse
import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal enum class ApiRiskHandling {
    HandleAndRetryOnce,
    Bypass,
}

@Singleton
internal class RiskAwareApiExecutor @Inject constructor(
    private val detector: ApiRiskChallengeDetector,
    verifier: Optional<ApiRiskVerifier>,
) {
    private val verifier = verifier.orElse(ApiRiskVerifier { ApiRiskVerificationResult.Unavailable })
    private val verificationMutex = Mutex()

    suspend fun execute(
        riskHandling: ApiRiskHandling = ApiRiskHandling.HandleAndRetryOnce,
        request: suspend () -> ApiRawResponse,
    ): ApiRawResponse {
        val firstResponse = request()
        val challenge = detector.detect(firstResponse) ?: return firstResponse
        if (riskHandling == ApiRiskHandling.Bypass) {
            throw ApiRiskException(challenge, ApiRiskException.Reason.Failed)
        }

        when (verificationMutex.withLock { verifier.verify(challenge) }) {
            ApiRiskVerificationResult.Verified -> Unit
            ApiRiskVerificationResult.Cancelled -> throw ApiRiskException(challenge, ApiRiskException.Reason.Cancelled)
            ApiRiskVerificationResult.Unavailable ->
                throw ApiRiskException(challenge, ApiRiskException.Reason.VerificationUnavailable)
            is ApiRiskVerificationResult.Failed -> throw ApiRiskException(challenge, ApiRiskException.Reason.Failed)
        }

        val retryResponse = request()
        val repeated = detector.detect(retryResponse)
        if (repeated != null) throw ApiRiskException(repeated, ApiRiskException.Reason.RepeatedChallenge)
        return retryResponse
    }
}
