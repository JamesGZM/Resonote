package com.resonote.core.network.protocol

import android.util.Log
import com.resonote.core.network.BuildConfig
import javax.inject.Inject
import kotlin.time.TimeSource
import okhttp3.Interceptor
import okhttp3.Response

internal class RedactedNetworkLoggingInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!BuildConfig.DEBUG) return chain.proceed(chain.request())
        val request = chain.request()
        val mark = TimeSource.Monotonic.markNow()
        Log.d(TAG, request.redactedLabel())
        return try {
            chain.proceed(request).also { response ->
                Log.d(TAG, "${request.method} ${request.url.host}${request.url.encodedPath} ${response.code} ${mark.elapsedNow()}")
            }
        } catch (throwable: Throwable) {
            Log.d(TAG, "${request.method} ${request.url.host}${request.url.encodedPath} failed ${throwable.javaClass.simpleName}")
            throw throwable
        }
    }

    private companion object {
        const val TAG = "ResonoteNetwork"
    }
}

internal fun okhttp3.Request.redactedLabel(): String = "$method ${url.scheme}://${url.host}${url.encodedPath}"
