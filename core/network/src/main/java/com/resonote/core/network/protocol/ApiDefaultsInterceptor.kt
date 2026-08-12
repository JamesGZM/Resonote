package com.resonote.core.network.protocol

import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionManager
import java.time.Clock
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/** Adds the common query, header, and cookie values required by typed Retrofit endpoints. */
internal class ApiDefaultsInterceptor @Inject constructor(
    private val clock: Clock,
    private val sessions: ApiSessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val policy = request.apiRequestPolicy() ?: return chain.proceed(request)
        val session = sessions.snapshot()
        val clientTimeSeconds = clock.millis() / 1_000
        return chain.proceed(request.withDefaults(policy, session, clientTimeSeconds))
    }

    private fun Request.withDefaults(
        policy: ApiRequestPolicy,
        session: ApiSession,
        clientTimeSeconds: Long,
    ): Request {
        val endpointQuery = linkedMapOf<String, String>()
        for (index in 0 until url.querySize) {
            val name = url.queryParameterName(index)
            require(name !in endpointQuery) { "Duplicate query parameter is not supported: $name" }
            endpointQuery[name] = url.queryParameterValue(index).orEmpty()
        }

        val query = linkedMapOf<String, String>()
        if (policy.includeDefaultParams) {
            query += mapOf(
                "dfid" to session.dfid.orEmpty().ifBlank { "-" },
                "mid" to session.mid,
                "uuid" to ApiProtocolConfig.UUID,
                "appid" to ApiProtocolConfig.APP_ID,
                "clientver" to ApiProtocolConfig.CLIENT_VERSION,
                "clienttime" to clientTimeSeconds.toString(),
            )
            if (policy.sessionMode == ApiSessionMode.Full) {
                session.token?.takeIf(String::isNotBlank)?.let { query["token"] = it }
                session.userId?.takeIf { it.isNotBlank() && it != "0" }?.let { query["userid"] = it }
            }
        }
        query += endpointQuery

        val updatedUrl = url.newBuilder().query(null).apply { query.forEach(::addQueryParameter) }.build()
        val builder = newBuilder().url(updatedUrl)
        if (policy.sessionMode != ApiSessionMode.None) {
            builder.header("dfid", session.dfid.orEmpty().ifBlank { "-" })
            builder.header("mid", session.mid)
            builder.header("clienttime", query["clienttime"] ?: clientTimeSeconds.toString())
            builder.header("kg-rc", "1")
            builder.header("kg-thash", "5d816a0")
            builder.header("kg-rec", "1")
            builder.header("kg-rf", "B9EDA08A64250DEFFBCADDEE00F8F25F")
            cookieHeader(policy.sessionMode, session)?.let { builder.header("Cookie", it) }
        }
        if (header("User-Agent") == null) builder.header("User-Agent", ApiProtocolConfig.USER_AGENT)
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
}
