package com.resonote.core.network.protocol

import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.ApiNetworkException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.retrofit.ApiRawResponse
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import com.resonote.core.network.session.ApiSession
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
import kotlinx.coroutines.suspendCancellableCoroutine
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
        val nowMillis = clock.millis()
        val exchange = factory(session, nowMillis)
        val raw = executeRaw(
            prepare(exchange.spec, session, nowMillis / 1_000),
            exchange.spec.responseFormat,
        )
        if (exchange.spec.riskPolicy == ApiRiskPolicy.Detect) {
            riskDetector.detect(raw)?.let { challenge ->
                throw ApiRiskException(challenge, ApiRiskException.Reason.VerificationUnavailable)
            }
        }
        return exchange.decode(raw)
    }

    private fun prepare(spec: ApiEndpointSpec, session: ApiSession, clientTime: Long): Request {
        require(spec.id.isNotBlank()) { "Endpoint id must not be blank" }
        require(spec.path.startsWith('/') && '?' !in spec.path && '#' !in spec.path) { "Endpoint path must be absolute and query-free" }
        val origin = spec.origin.toHttpUrl()
        require(originPolicy.isAllowed(spec)) { "Only HTTPS or the fixed login mobile-code origin is allowed" }
        require(origin.encodedPath == "/" && origin.query == null && origin.fragment == null) { "Origin must not include path, query, or fragment" }

        val query = linkedMapOf<String, String>()
        if (spec.includeDefaultParams) {
            query += mapOf(
                "dfid" to session.dfid.orEmpty().ifBlank { "-" },
                "mid" to session.mid,
                "uuid" to ApiProtocolConfig.UUID,
                "appid" to ApiProtocolConfig.APP_ID,
                "clientver" to ApiProtocolConfig.CLIENT_VERSION,
                "clienttime" to clientTime.toString(),
            )
            if (spec.sessionMode == ApiSessionMode.Full) {
                session.token?.takeIf(String::isNotBlank)?.let { query["token"] = it }
                session.userId?.takeIf { it.isNotBlank() && it != "0" }?.let { query["userid"] = it }
            }
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
        if (spec.sessionMode != ApiSessionMode.None) {
            builder.header("dfid", session.dfid.orEmpty().ifBlank { "-" })
            builder.header("mid", session.mid)
            builder.header("clienttime", query["clienttime"] ?: clientTime.toString())
            builder.header("kg-rc", "1")
            builder.header("kg-thash", "5d816a0")
            builder.header("kg-rec", "1")
            builder.header("kg-rf", "B9EDA08A64250DEFFBCADDEE00F8F25F")
            cookieHeader(spec.sessionMode, session)?.let { builder.header("Cookie", it) }
        }
        if (builder.build().header("User-Agent") == null) builder.header("User-Agent", ApiProtocolConfig.USER_AGENT)
        when (spec.method) {
            ApiHttpMethod.Get -> builder.get()
            ApiHttpMethod.Post -> builder.post(spec.body.orEmpty().toRequestBody("application/json".toMediaType()))
        }
        return builder.build()
    }

    private fun cookieHeader(mode: ApiSessionMode, session: ApiSession): String? {
        val cookies =
            when (mode) {
                ApiSessionMode.None -> emptyMap()
                ApiSessionMode.DeviceOnly -> mapOf("mid" to session.mid)
                ApiSessionMode.Full -> session.cookies
            }
        return cookies.entries.joinToString("; ") { (name, value) -> "$name=$value" }.takeIf(String::isNotEmpty)
    }

    private suspend fun executeRaw(request: Request, responseFormat: ApiResponseFormat): ApiRawResponse {
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
        response.use {
            val bytes = it.body?.bytes().orEmpty()
            if (!it.isSuccessful) throw ApiHttpException(it.code)
            val body =
                bytes.takeIf(ByteArray::isNotEmpty)?.let { payload ->
                    runCatching { json.parseToJsonElement(payload.decodeToString()) as? JsonObject }.getOrNull()
                }
            if (bytes.isNotEmpty() && body == null && responseFormat == ApiResponseFormat.Json) {
                throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
            }
            return ApiRawResponse(it.code, it.headers.toMultimap(), bytes, body)
        }
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
