package com.resonote.core.network.protocol

import com.resonote.core.network.retrofit.ApiRawResponse
import com.resonote.core.network.session.ApiSession

internal enum class ApiHttpMethod { Get, Post }
internal enum class ApiSignatureMode { Android, Web, Register, None }
internal enum class ApiSessionMode { Full, DeviceOnly, None }
internal enum class ApiResponseFormat { Json, Bytes }
internal enum class ApiCleartextPolicy { Deny, LoginMobileCode }
internal enum class ApiRiskPolicy { HandleAndReplayOnce, Bypass }

internal data class ApiEndpointSpec(
    val id: String,
    val origin: String = ApiProtocolConfig.BASE_URL.removeSuffix("/"),
    val path: String,
    val method: ApiHttpMethod,
    val query: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
    val signatureMode: ApiSignatureMode = ApiSignatureMode.Android,
    val sessionMode: ApiSessionMode = ApiSessionMode.Full,
    val includeDefaultParams: Boolean = true,
    val responseFormat: ApiResponseFormat = ApiResponseFormat.Json,
    val cleartextPolicy: ApiCleartextPolicy = ApiCleartextPolicy.Deny,
    val riskPolicy: ApiRiskPolicy = ApiRiskPolicy.HandleAndReplayOnce,
) {
    override fun toString(): String =
        "ApiEndpointSpec(id=$id, origin=$origin, path=$path, method=$method, " +
            "queryNames=${query.keys.sorted()}, headerNames=${headers.keys.sorted()}, bodyBytes=${body?.size ?: 0}, " +
            "signatureMode=$signatureMode, sessionMode=$sessionMode, responseFormat=$responseFormat, " +
            "cleartextPolicy=$cleartextPolicy, riskPolicy=$riskPolicy)"
}

internal data class ApiExchange<T>(
    val spec: ApiEndpointSpec,
    val decode: (ApiRawResponse) -> T,
)

internal typealias ApiExchangeFactory<T> = suspend (ApiSession, Long) -> ApiExchange<T>
