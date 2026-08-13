package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiException
import com.resonote.core.network.ApiAuthenticationRequiredException
import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.ApiNetworkException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.AuthenticationFailureClassifier
import com.resonote.core.network.api.model.MusicApiResponse
import com.resonote.core.network.protocol.ApiSessionPropagation
import com.resonote.core.network.protocol.ApiRawResponse
import com.resonote.core.network.protocol.apiRequestPolicy
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import com.resonote.core.network.session.ApiAuthenticationContext
import com.resonote.core.network.session.ApiSessionManager
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal class ApiCallExecutor @Inject constructor(
    private val sessions: ApiSessionManager,
) {
    suspend fun <T> execute(
        detectHttpAuthenticationFailure: Boolean = true,
        block: suspend () -> T,
    ): T {
        val authenticationContext = sessions.authenticationContext()
        return try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (api: ApiException) {
            throw api
        } catch (http: retrofit2.HttpException) {
            val propagation = http.response()?.raw()?.request?.apiRequestPolicy()?.sessionPropagation
                ?: ApiSessionPropagation.None
            if (
                detectHttpAuthenticationFailure &&
                AuthenticationFailureClassifier.capturesHttpFailure(http.code(), propagation)
            ) {
                AuthenticationFailureClassifier.classify(sessions, authenticationContext)?.let { throw it }
            }
            throw ApiHttpException(http.code())
        } catch (timeout: SocketTimeoutException) {
            throw ApiNetworkException(ApiNetworkException.Kind.Timeout, timeout)
        } catch (offline: UnknownHostException) {
            throw ApiNetworkException(ApiNetworkException.Kind.Offline, offline)
        } catch (malformed: SerializationException) {
            throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
        } catch (connection: IOException) {
            throw ApiNetworkException(ApiNetworkException.Kind.Connection, connection)
        }
    }
}

internal class ApiResponseVerifier @Inject constructor(
    private val riskDetector: ApiRiskChallengeDetector,
    private val sessions: ApiSessionManager,
) {
    fun authenticationContext(): ApiAuthenticationContext = sessions.authenticationContext()

    suspend fun requireSuccess(
        response: MusicApiResponse,
        endpointId: String? = null,
    ) {
        requireNoRiskChallenge(response)
        serviceFailureCodeOrNull(response)?.let { serviceCode ->
            if (endpointId != null) requireValidAuthentication(endpointId, serviceCode)
            throw ApiServiceException(serviceCode)
        }
    }

    suspend fun requireValidAuthentication(
        endpointId: String,
        serviceCode: String,
    ) {
        AuthenticationFailureClassifier.requestAuthenticationFailure(endpointId, serviceCode)?.let { throw it }
    }

    suspend fun requireAuthenticatedSession(
        serviceCode: String?,
        authenticationContext: ApiAuthenticationContext,
    ) {
        AuthenticationFailureClassifier.classify(sessions, authenticationContext, serviceCode)?.let { throw it }
    }

    suspend fun requireWriteSuccess(
        response: MusicApiResponse,
        endpointId: String? = null,
    ) {
        requireNoRiskChallenge(response)
        serviceFailureCodeOrNull(response)?.let { serviceCode ->
            if (endpointId != null) requireValidAuthentication(endpointId, serviceCode)
            throw ApiServiceException(serviceCode)
        }
    }

    suspend fun requireJsonSuccess(
        response: JsonObject,
        endpointId: String,
        successStatuses: Set<String>,
    ) {
        riskDetector.detect(ApiRawResponse(200, emptyMap(), byteArrayOf(), response))?.let { challenge ->
            throw ApiRiskException(challenge, ApiRiskException.Reason.VerificationUnavailable)
        }
        val status = response.text("status")
        val errorCode = response.text("error_code")
        val failedCode = errorCode != null && errorCode.toDoubleOrNull() != 0.0
        if (status !in successStatuses || failedCode) {
            val serviceCode = errorCode ?: status
            if (serviceCode != null) requireValidAuthentication(endpointId, serviceCode)
            throw ApiServiceException(serviceCode)
        }
    }

    fun requireNoRiskChallenge(response: MusicApiResponse) {
        riskDetector.detect(response)?.let { challenge ->
            throw ApiRiskException(challenge, ApiRiskException.Reason.VerificationUnavailable)
        }
    }

    fun serviceFailureCodeOrNull(response: MusicApiResponse): String? {
        val normalizedStatus = response.status?.trim()?.takeIf(String::isNotEmpty)
        val failedStatus = normalizedStatus?.toDoubleOrNull() == 0.0
        val normalizedCode = response.errorCode?.trim()?.takeIf(String::isNotEmpty)
        val failedCode = normalizedCode != null && normalizedCode.toDoubleOrNull() != 0.0
        return (normalizedCode ?: normalizedStatus).takeIf { failedStatus || failedCode }
    }

    private fun JsonObject.text(name: String): String? =
        (get(name) as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)
}
