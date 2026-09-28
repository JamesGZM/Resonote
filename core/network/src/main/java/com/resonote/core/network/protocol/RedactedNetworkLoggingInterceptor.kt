package com.resonote.core.network.protocol

import android.util.Log
import com.resonote.core.network.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import kotlin.time.TimeSource

internal class RedactedNetworkLoggingInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!BuildConfig.DEBUG && !BuildConfig.DIAGNOSTIC_LOGGING) return chain.proceed(chain.request())
        val request = chain.request()
        val mark = TimeSource.Monotonic.markNow()
        Log.d(TAG, request.redactedLabel())
        return try {
            chain.proceed(request).also { response ->
                val metadata = if (BuildConfig.DIAGNOSTIC_LOGGING) {
                    runCatching { response.peekBody(64 * 1024).string().diagnosticEnvelope() }
                        .getOrDefault("envelope=unavailable")
                } else {
                    ""
                }
                Log.d(
                    TAG,
                    "${request.method} ${request.url.host}${request.url.encodedPath} ${response.code} ${mark.elapsedNow()} $metadata",
                )
            }
        } catch (throwable: Throwable) {
            Log.d(
                TAG,
                "${request.method} ${request.url.host}${request.url.encodedPath} failed ${throwable.redactedDescription()}",
            )
            throw throwable
        }
    }

    private companion object {
        const val TAG = "ResonoteNetwork"
    }
}

internal fun okhttp3.Request.redactedLabel(): String = "$method ${url.scheme}://${url.host}${url.encodedPath}"

internal fun Throwable.redactedDescription(): String = generateSequence(this) { it.cause }
    .take(MAX_CAUSE_DEPTH)
    .joinToString(separator = " <- ") { throwable ->
        val type = throwable.javaClass.simpleName.ifBlank { "Throwable" }
        throwable.message
            ?.takeIf(String::isNotBlank)
            ?.redactSensitiveValues()
            ?.let { message -> "$type: $message" }
            ?: type
    }

private fun String.redactSensitiveValues(): String = replace(URL_QUERY_PATTERN, "$1?<redacted>")
    .replace(SENSITIVE_VALUE_PATTERN, "$1=<redacted>")
    .take(MAX_MESSAGE_LENGTH)

private const val MAX_CAUSE_DEPTH = 3
private const val MAX_MESSAGE_LENGTH = 240
private val URL_QUERY_PATTERN = Regex("(https?://[^\\s?]+)\\?[^\\s]+", RegexOption.IGNORE_CASE)
private val SENSITIVE_VALUE_PATTERN = Regex(
    "(?i)(token|signature|authorization|cookie|key|mid|dfid|userid)=[^\\s&,;]+",
)

/** Only bounded numeric protocol metadata is allowed; payloads and server messages stay private. */
internal fun String.diagnosticEnvelope(): String {
    val root = runCatching { Json.parseToJsonElement(this) as? JsonObject }.getOrNull()
        ?: return "envelope=non-json-or-truncated"
    return listOf("status", "error_code", "errcode", "code", "ssaCode").joinToString(" ") { field ->
        val value = (root[field] as? JsonPrimitive)?.contentOrNull
        "$field=${value.diagnosticCode()}"
    }
}

internal fun String?.diagnosticCode(): String = when {
    this == null -> "absent"
    matches(Regex("-?[0-9]{1,12}")) -> this
    else -> "redacted"
}

internal fun diagnosticNetworkLog(message: () -> String) {
    if (BuildConfig.DIAGNOSTIC_LOGGING) Log.i("ResonoteDiagnostic", message())
}
