package com.resonote.core.data

import com.resonote.core.model.RiskChallengeHandle
import com.resonote.core.model.RiskVerificationMethodResult
import com.resonote.core.model.RiskVerificationProof
import com.resonote.core.model.RiskVerificationSubmitResult

interface RiskVerificationRepository {
    suspend fun methodFor(challenge: RiskChallengeHandle): RiskVerificationMethodResult

    suspend fun submit(challenge: RiskChallengeHandle, proof: RiskVerificationProof): RiskVerificationSubmitResult
}
