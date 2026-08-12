package com.resonote.core.network.protocol

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer

/** Signs the final query and the exact request body bytes produced by Retrofit. */
internal class ApiSigningInterceptor @Inject constructor(
    private val signer: ApiRequestSigner,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val policy = request.apiRequestPolicy() ?: return chain.proceed(request)
        if (policy.signatureMode == ApiSignatureMode.None || request.url.queryParameter("signature") != null) {
            return chain.proceed(request)
        }
        return chain.proceed(request.withSignature(policy.signatureMode))
    }

    private fun Request.withSignature(mode: ApiSignatureMode): Request {
        val query = linkedMapOf<String, String>()
        for (index in 0 until url.querySize) {
            val name = url.queryParameterName(index)
            require(name !in query) { "Duplicate query parameter is not supported: $name" }
            query[name] = url.queryParameterValue(index).orEmpty()
        }
        val signature =
            when (mode) {
                ApiSignatureMode.Android -> signer.sign(query, bodyBytes())
                ApiSignatureMode.Web -> signer.web(query)
                ApiSignatureMode.Register -> signer.register(query)
                ApiSignatureMode.None -> error("Unsigned requests are handled before signing")
            }
        return newBuilder().url(url.newBuilder().addQueryParameter("signature", signature).build()).build()
    }

    private fun Request.bodyBytes(): ByteArray {
        val requestBody = body ?: return byteArrayOf()
        val buffer = Buffer()
        requestBody.writeTo(buffer)
        return buffer.readByteArray()
    }
}
