package com.resonote.core.network.protocol

import com.resonote.core.network.ApiProtocolException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject

/** Normalizes the provider's response-header risk event into the typed JSON envelope. */
internal class ApiResponseMetadataInterceptor @Inject constructor(private val json: Json) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (request.apiRequestPolicy() == null) return response
        val eventId = response.header(SSA_CODE_HEADER)?.takeIf(String::isNotBlank) ?: return response
        val body = response.body ?: throw malformed()
        val contentType = body.contentType()
        val bytes = body.readBounded()
        val root =
            try {
                json.parseToJsonElement(bytes.decodeToString()) as? JsonObject
            } catch (_: SerializationException) {
                null
            } ?: throw malformed()
        val normalized = JsonObject(root + (SSA_CODE_FIELD to JsonPrimitive(eventId)))
        return response.newBuilder().body(normalized.toString().toResponseBody(contentType)).build()
    }

    private fun ResponseBody.readBounded(): ByteArray = use { body ->
        if (body.contentLength() > MAX_JSON_BYTES) throw malformed()
        val source = body.source()
        source.request(MAX_JSON_BYTES + 1L)
        if (source.buffer.size > MAX_JSON_BYTES) throw malformed()
        source.readByteArray()
    }

    private fun malformed() = ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)

    private companion object {
        const val SSA_CODE_HEADER = "ssa-code"
        const val SSA_CODE_FIELD = "ssaCode"
        const val MAX_JSON_BYTES = 2 * 1024 * 1024
    }
}
