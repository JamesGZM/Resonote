package com.resonote.core.network.risk

/** Protocol-only risk operations coordinated by the application flow that received the challenge. */
interface ApiRiskVerificationService {
    suspend fun methodFor(challenge: ApiRiskChallenge): ApiRiskMethod

    suspend fun submit(challenge: ApiRiskChallenge, proof: ApiRiskProof)
}

sealed interface ApiRiskProof {
    data class Sms(val code: String) : ApiRiskProof {
        override fun toString(): String = "ApiRiskProof.Sms(code=<redacted>)"
    }

    data class Tencent(val ticket: String, val randomString: String, val appId: String) : ApiRiskProof {
        override fun toString(): String =
            "ApiRiskProof.Tencent(ticket=<redacted>, randomString=<redacted>, appId=<redacted>)"
    }
}
