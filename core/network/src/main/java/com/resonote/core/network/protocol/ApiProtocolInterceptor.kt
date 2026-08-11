package com.resonote.core.network.protocol

import java.time.Clock
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer

internal class ApiProtocolInterceptor @Inject constructor(
    private val clock: Clock,
    private val identity: ApiDeviceIdentity,
    private val signer: ApiRequestSigner,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header(BYPASS_PROTOCOL_HEADER) == "true") {
            return chain.proceed(original.newBuilder().removeHeader(BYPASS_PROTOCOL_HEADER).build())
        }
        if (original.url.queryParameter("signature") != null) return chain.proceed(original)

        val clientTime = clock.instant().epochSecond.toString()
        val unsignedUrl =
            original.url.newBuilder()
                .addQueryParameter("dfid", identity.dfid)
                .addQueryParameter("mid", identity.mid)
                .addQueryParameter("uuid", ApiProtocolConfig.UUID)
                .addQueryParameter("appid", ApiProtocolConfig.APP_ID)
                .addQueryParameter("clientver", ApiProtocolConfig.CLIENT_VERSION)
                .addQueryParameter("clienttime", clientTime)
                .build()
        val parameters =
            unsignedUrl.queryParameterNames.associateWith { name ->
                unsignedUrl.queryParameterValues(name).firstOrNull().orEmpty()
            }
        val signature = signer.sign(parameters = parameters, body = original.bodyText())
        val signedUrl = unsignedUrl.newBuilder().addQueryParameter("signature", signature).build()

        val request =
            original.newBuilder()
                .url(signedUrl)
                .header("User-Agent", "Android15-1070-11083-46-0-DiscoveryDRADProtocol-wifi")
                .header("dfid", identity.dfid)
                .header("mid", identity.mid)
                .header("clienttime", clientTime)
                .header("kg-rc", "1")
                .header("kg-thash", "5d816a0")
                .header("kg-rec", "1")
                .header("kg-rf", "B9EDA08A64250DEFFBCADDEE00F8F25F")
                .build()
        return chain.proceed(request)
    }

    private fun okhttp3.Request.bodyText(): String {
        val requestBody = body ?: return ""
        return Buffer().use { buffer ->
            requestBody.writeTo(buffer)
            buffer.readUtf8()
        }
    }

    internal companion object {
        const val BYPASS_PROTOCOL_HEADER = "X-Resonote-Bypass-Api-Protocol"
    }
}
