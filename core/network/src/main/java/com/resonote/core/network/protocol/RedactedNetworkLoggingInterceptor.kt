package com.resonote.core.network.protocol

import android.util.Log
import com.resonote.core.network.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import kotlin.time.TimeSource

internal class RedactedNetworkLoggingInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!BuildConfig.DEBUG) return chain.proceed(chain.request())
        val request = chain.request()
        val mark = TimeSource.Monotonic.markNow()
        Log.d(TAG, request.redactedLabel())
        return try {
            chain.proceed(request).also { response ->
                Log.d(
                    TAG,
                    "${request.method} ${request.url.host}${request.url.encodedPath} ${response.code} ${mark.elapsedNow()}",
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
