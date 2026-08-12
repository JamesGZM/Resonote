package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.RiskVerificationMethod
import com.resonote.core.model.RiskVerificationMethodResult
import com.resonote.core.model.RiskVerificationProof
import com.resonote.core.model.RiskVerificationSubmitResult
import com.resonote.core.network.risk.ApiRiskChallenge
import com.resonote.core.network.risk.ApiRiskMethod
import com.resonote.core.network.risk.ApiRiskProof
import com.resonote.core.network.risk.ApiRiskVerificationService
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RiskVerificationRepositoryTest {
    @Test
    fun opaqueChallengeCanBeVerifiedWithoutExposingNetworkModel() = runTest {
        val registry = RiskChallengeRegistry()
        val challenge = ApiRiskChallenge(eventId = "provider-event", sid = "provider-sid", edt = "provider-edt")
        val handle = registry.register(challenge)
        val service = FakeRiskVerificationService()
        val repository = DefaultRiskVerificationRepository(service, registry)

        val method = repository.methodFor(handle) as RiskVerificationMethodResult.Available
        val result = repository.submit(handle, RiskVerificationProof.Sms("246810"))

        assertThat(method.method).isEqualTo(RiskVerificationMethod.Tencent("fixture-app"))
        assertThat(result).isEqualTo(RiskVerificationSubmitResult.Verified)
        assertThat(service.methodChallenge).isSameInstanceAs(challenge)
        assertThat(service.submittedChallenge).isSameInstanceAs(challenge)
        assertThat(service.proof).isEqualTo(ApiRiskProof.Sms("246810"))
        assertThat(handle.toString()).doesNotContain("provider-event")
        assertThat(repository.methodFor(handle))
            .isEqualTo(RiskVerificationMethodResult.Failed(ContentFailure.Protocol))
    }

    private class FakeRiskVerificationService : ApiRiskVerificationService {
        var methodChallenge: ApiRiskChallenge? = null
        var submittedChallenge: ApiRiskChallenge? = null
        var proof: ApiRiskProof? = null

        override suspend fun methodFor(challenge: ApiRiskChallenge): ApiRiskMethod {
            methodChallenge = challenge
            return ApiRiskMethod.Tencent("fixture-app")
        }

        override suspend fun submit(challenge: ApiRiskChallenge, proof: ApiRiskProof) {
            submittedChallenge = challenge
            this.proof = proof
        }
    }
}
