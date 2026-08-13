package com.resonote.core.network.protocol

import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.AuthenticationFailureClassifier
import com.resonote.core.network.ApiNetworkException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiAuthenticationContext
import com.resonote.core.network.session.ApiSessionManager
import dagger.Lazy
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

@Singleton
internal class ProtocolTransport @Inject constructor(
    private val callFactory: Lazy<Call.Factory>,
    private val json: Json,
    private val clock: Clock,
    private val signer: ApiRequestSigner,
    private val sessionManager: ApiSessionManager,
    private val riskDetector: ApiRiskChallengeDetector,
    private val originPolicy: ApiOriginPolicy,
) {
    suspend fun <T> execute(factory: ApiExchangeFactory<T>): T {
        val session = sessionManager.current()
        val authenticationContext = sessionManager.authenticationContext()
        val nowMillis = clock.millis()
        val exchange = factory(session, nowMillis)
        val raw = executeRaw(
            prepare(exchange.spec, session, nowMillis / 1_000),
            exchange.spec,
            authenticationContext,
            exchange.spec.responseFormat,
        )
        if (exchange.spec.riskPolicy == ApiRiskPolicy.Detect) {
            riskDetector.detect(raw)?.let { challenge ->
                throw ApiRiskException(challenge, ApiRiskException.Reason.VerificationUnavailable)
            }
        }
        raw.serviceFailureCodeOrNull()?.let { serviceCode ->
            AuthenticationFailureClassifier.requestAuthenticationFailure(exchange.spec.id, serviceCode)?.let { throw it }
        }
        return exchange.decode(raw)
    }

    private fun prepare(spec: ApiEndpointSpec, session: ApiSession, clientTime: Long): Request {
        require(spec.id.isNotBlank()) { "Endpoint id must not be blank" }
        require(spec.path.startsWith('/') && '?' !in spec.path && '#' !in spec.path) { "Endpoint path must be absolute and query-free" }
        val origin = spec.origin.toHttpUrl()
        require(originPolicy.isAllowed(spec)) { "Only HTTPS or the fixed login mobile-code origin is allowed" }
        require(
            spec.sessionPropagation == ApiSessionPropagation.None || ApiSessionOriginPolicy.isAllowed(origin.host),
        ) { "Session propagation is not allowed for origin ${spec.origin}" }
        require(origin.encodedPath == "/" && origin.query == null && origin.fragment == null) { "Origin must not include path, query, or fragment" }

        val query = linkedMapOf<String, String>()
        if (spec.includeDefaultParams) {
            query += ApiSessionRequestDecorator.defaultQuery(session, clientTime, spec.sessionPropagation)
        }
        query += spec.query
        if (spec.signatureMode != ApiSignatureMode.None && "signature" !in query) {
            query["signature"] =
                when (spec.signatureMode) {
                    ApiSignatureMode.Android -> signer.sign(query, spec.body.orEmpty())
                    ApiSignatureMode.Web -> signer.web(query)
                    ApiSignatureMode.Register -> signer.register(query)
                    ApiSignatureMode.None -> error("None handled before signing")
                }
        }
        val url = origin.newBuilder().encodedPath(spec.path).apply { query.forEach(::addQueryParameter) }.build()
        val builder = Request.Builder().url(url)
        spec.headers.forEach(builder::header)
        ApiSessionRequestDecorator.applySessionHeaders(
            builder,
            session,
            query["clienttime"] ?: clientTime.toString(),
            spec.sessionPropagation,
        )
        if (builder.build().header("User-Agent") == null) builder.header("User-Agent", ApiProtocolConfig.USER_AGENT)
        when (spec.method) {
            ApiHttpMethod.Get -> builder.get()
            ApiHttpMethod.Post -> builder.post(spec.body.orEmpty().toRequestBody(spec.contentType.toMediaType()))
        }
        return builder.build()
    }

    private suspend fun executeRaw(
        request: Request,
        spec: ApiEndpointSpec,
        authenticationContext: ApiAuthenticationContext,
        responseFormat: ApiResponseFormat,
    ): ApiRawResponse {
        val response =
            try {
                callFactory.get().newCall(request).await()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (timeout: SocketTimeoutException) {
                throw ApiNetworkException(ApiNetworkException.Kind.Timeout, timeout)
            } catch (offline: UnknownHostException) {
                throw ApiNetworkException(ApiNetworkException.Kind.Offline, offline)
            } catch (connection: IOException) {
                throw ApiNetworkException(ApiNetworkException.Kind.Connection, connection)
            }
        return withContext(Dispatchers.IO) {
            response.use {
                val bytes = it.body?.bytes().orEmpty()
                if (!it.isSuccessful) {
                    if (AuthenticationFailureClassifier.capturesHttpFailure(it.code, spec.sessionPropagation)) {
                        AuthenticationFailureClassifier.classify(sessionManager, authenticationContext)?.let { throw it }
                    }
                    throw ApiHttpException(it.code)
                }
                val body =
                    bytes.takeIf(ByteArray::isNotEmpty)?.let { payload ->
                        runCatching { json.parseToJsonElement(payload.decodeToString()) as? JsonObject }.getOrNull()
                    }
                if (bytes.isNotEmpty() && body == null && responseFormat == ApiResponseFormat.Json) {
                    throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
                }
                ApiRawResponse(it.code, it.headers.toMultimap(), bytes, body)
            }
        }
    }

    private fun ApiRawResponse.serviceFailureCodeOrNull(): String? {
        val status = body?.get("status")?.toString()?.trim('"')?.trim()?.takeIf(String::isNotEmpty)
        val errorCode = body?.get("error_code")?.toString()?.trim('"')?.trim()?.takeIf(String::isNotEmpty)
        val failedStatus = status?.toDoubleOrNull() == 0.0
        val failedCode = errorCode != null && errorCode.toDoubleOrNull() != 0.0
        return (errorCode ?: status).takeIf { failedStatus || failedCode }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response) { _, cancelledResponse, _ -> cancelledResponse.close() }
                }
            },
        )
    }

}

private fun ByteArray?.orEmpty(): ByteArray = this ?: byteArrayOf()
