package com.resonote.core.data

import com.resonote.core.model.ContentFailure
import com.resonote.core.model.RiskChallengeHandle
import com.resonote.core.model.RiskVerificationMethod
import com.resonote.core.model.RiskVerificationMethodResult
import com.resonote.core.model.RiskVerificationProof
import com.resonote.core.model.RiskVerificationSubmitResult
import com.resonote.core.network.ApiException
import com.resonote.core.network.risk.ApiRiskMethod
import com.resonote.core.network.risk.ApiRiskProof
import com.resonote.core.network.risk.ApiRiskVerificationService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultRiskVerificationRepository @Inject constructor(
    private val service: ApiRiskVerificationService,
    private val challenges: RiskChallengeRegistry,
) : RiskVerificationRepository {
    override suspend fun methodFor(challenge: RiskChallengeHandle): RiskVerificationMethodResult {
        val networkChallenge = challenges.find(challenge) ?: return missingChallengeMethod()
        return try {
            RiskVerificationMethodResult.Available(service.methodFor(networkChallenge).toDomain())
        } catch (failure: ApiException) {
            RiskVerificationMethodResult.Failed(failure.toContentFailure(challenges))
        }
    }

    override suspend fun submit(
        challenge: RiskChallengeHandle,
        proof: RiskVerificationProof,
    ): RiskVerificationSubmitResult {
        val networkChallenge = challenges.find(challenge) ?: return missingChallengeSubmit()
        return try {
            service.submit(networkChallenge, proof.toNetwork())
            challenges.remove(challenge)
            RiskVerificationSubmitResult.Verified
        } catch (failure: ApiException) {
            RiskVerificationSubmitResult.Failed(failure.toContentFailure(challenges))
        }
    }

    private fun missingChallengeMethod() = RiskVerificationMethodResult.Failed(ContentFailure.Protocol)

    private fun missingChallengeSubmit() = RiskVerificationSubmitResult.Failed(ContentFailure.Protocol)
}

private fun ApiRiskMethod.toDomain(): RiskVerificationMethod = when (this) {
    ApiRiskMethod.Sms -> RiskVerificationMethod.Sms
    is ApiRiskMethod.Tencent -> RiskVerificationMethod.Tencent(appId)
    is ApiRiskMethod.Unsupported -> RiskVerificationMethod.Unsupported(type)
}

private fun RiskVerificationProof.toNetwork(): ApiRiskProof = when (this) {
    is RiskVerificationProof.Sms -> ApiRiskProof.Sms(code)
    is RiskVerificationProof.Tencent -> ApiRiskProof.Tencent(ticket, randomString, applicationId)
}
