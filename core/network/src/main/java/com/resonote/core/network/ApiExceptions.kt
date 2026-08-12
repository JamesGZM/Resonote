package com.resonote.core.network

import com.resonote.core.network.risk.ApiRiskChallenge
import com.resonote.core.network.session.ApiAuthenticationGateReason
import java.io.IOException

sealed class ApiException(message: String, cause: Throwable? = null) : IOException(message, cause)

class ApiAuthenticationRequiredException(
    val reason: ApiAuthenticationGateReason = ApiAuthenticationGateReason.LoginRequired,
    val serviceCode: String? = null,
) : ApiException("Authentication is required ($reason)${serviceCode?.let { ", serviceCode=$it" }.orEmpty()}")

class ApiHttpException(val statusCode: Int) : ApiException("HTTP request failed with status $statusCode")

class ApiNetworkException(
    val kind: Kind,
    cause: Throwable,
) : ApiException("Network request failed: $kind", cause) {
    enum class Kind {
        Offline,
        Timeout,
        Connection,
    }
}

class ApiProtocolException(val reason: Reason) : ApiException("API protocol failure: $reason") {
    enum class Reason {
        MalformedResponse,
        MissingRequiredField,
        MissingRiskEvent,
        InsecureMediaUrl,
    }
}

class ApiServiceException(val serviceCode: String?) :
    ApiException("API service rejected the request${serviceCode?.let { " (code=$it)" }.orEmpty()}")

class ApiPlaybackUnavailableException(val reason: Reason) : ApiException("Song playback unavailable: $reason") {
    enum class Reason {
        Copyright,
        Vip,
        Cloud,
    }
}

class ApiRiskException(
    val challenge: ApiRiskChallenge,
    val reason: Reason,
) : ApiException("API risk verification failed: $reason") {
    enum class Reason {
        Cancelled,
        Failed,
        RepeatedChallenge,
        VerificationUnavailable,
    }
}
