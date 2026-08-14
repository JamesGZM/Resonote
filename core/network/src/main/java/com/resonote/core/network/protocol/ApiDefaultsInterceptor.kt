package com.resonote.core.network.protocol

import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.time.Clock
import javax.inject.Inject

/** Adds the common query, header, and cookie values required by typed Retrofit endpoints. */
internal class ApiDefaultsInterceptor @Inject constructor(
    private val clock: Clock,
    private val sessions: ApiSessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val policy = request.apiRequestPolicy() ?: return chain.proceed(request)
        require(
            policy.sessionPropagation == ApiSessionPropagation.None ||
                ApiSessionOriginPolicy.isAllowed(request.url.host),
        ) { "Session propagation is not allowed for host ${request.url.host}" }
        val session = sessions.snapshot()
        val clientTimeSeconds = clock.millis() / 1_000
        return chain.proceed(request.withDefaults(policy, session, clientTimeSeconds))
    }

    private fun Request.withDefaults(policy: ApiRequestPolicy, session: ApiSession, clientTimeSeconds: Long): Request {
        val endpointQuery = linkedMapOf<String, String>()
        for (index in 0 until url.querySize) {
            val name = url.queryParameterName(index)
            require(name !in endpointQuery) { "Duplicate query parameter is not supported: $name" }
            endpointQuery[name] = url.queryParameterValue(index).orEmpty()
        }

        val query = linkedMapOf<String, String>()
        if (policy.includeDefaultParams) {
            query += ApiSessionRequestDecorator.defaultQuery(session, clientTimeSeconds, policy.sessionPropagation)
        }
        query += endpointQuery

        val updatedUrl = url.newBuilder().query(null).apply { query.forEach(::addQueryParameter) }.build()
        val builder = newBuilder().url(updatedUrl)
        ApiSessionRequestDecorator.applySessionHeaders(
            builder,
            session,
            query["clienttime"] ?: clientTimeSeconds.toString(),
            policy.sessionPropagation,
        )
        policy.router.takeIf(String::isNotEmpty)?.let { builder.policyHeader(this, ROUTER_HEADER, it) }
        policy.kgTid.takeIf { it != 0 }?.let { builder.policyHeader(this, KG_TID_HEADER, it.toString()) }
        if (header("User-Agent") == null) builder.header("User-Agent", ApiProtocolConfig.USER_AGENT)
        return builder.build()
    }

    private fun Request.Builder.policyHeader(request: Request, name: String, value: String) {
        val existing = request.header(name)
        require(existing == null || existing == value) {
            "Request header $name conflicts with ApiRequestPolicy"
        }
        header(name, value)
    }

    private companion object {
        const val ROUTER_HEADER = "x-router"
        const val KG_TID_HEADER = "kg-tid"
    }
}
