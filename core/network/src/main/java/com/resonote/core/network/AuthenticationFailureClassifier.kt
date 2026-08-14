package com.resonote.core.network

import com.resonote.core.network.protocol.ApiSessionPropagation
import com.resonote.core.network.session.ApiAuthenticationContext
import com.resonote.core.network.session.ApiSessionManager

internal object AuthenticationFailureClassifier {
    fun capturesHttpFailure(statusCode: Int, propagation: ApiSessionPropagation): Boolean =
        propagation == ApiSessionPropagation.Full && statusCode in AUTHENTICATION_HTTP_CODES

    fun capturesServiceFailure(authenticationServiceCodes: Set<String>, serviceCode: String): Boolean =
        serviceCode in authenticationServiceCodes

    suspend fun classify(
        sessions: ApiSessionManager,
        context: ApiAuthenticationContext,
        serviceCode: String? = null,
    ): ApiAuthenticationRequiredException? = sessions.reportAuthenticationFailure(context)?.let { reason ->
        ApiAuthenticationRequiredException(reason, serviceCode)
    }

    private val AUTHENTICATION_HTTP_CODES = setOf(401, 403)
}
