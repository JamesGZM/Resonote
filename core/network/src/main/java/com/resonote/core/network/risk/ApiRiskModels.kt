package com.resonote.core.network.risk

data class ApiRiskChallenge(
    val eventId: String,
    val sid: String? = null,
    val edt: String? = null,
    val serviceCode: String? = null,
) {
    override fun toString(): String =
        "ApiRiskChallenge(eventId=<redacted>, sid=<redacted>, edt=<redacted>, serviceCode=$serviceCode)"
}

sealed interface ApiRiskMethod {
    data object Sms : ApiRiskMethod

    data class Tencent(val appId: String) : ApiRiskMethod {
        override fun toString(): String = "ApiRiskMethod.Tencent(appId=<redacted>)"
    }

    data class Unsupported(val type: Int) : ApiRiskMethod
}

fun interface ApiRiskVerifier {
    suspend fun verify(challenge: ApiRiskChallenge): ApiRiskVerificationResult
}

sealed interface ApiRiskVerificationResult {
    data object Verified : ApiRiskVerificationResult

    data object Cancelled : ApiRiskVerificationResult

    data object Unavailable : ApiRiskVerificationResult

    data class Failed(val reason: String? = null) : ApiRiskVerificationResult
}
