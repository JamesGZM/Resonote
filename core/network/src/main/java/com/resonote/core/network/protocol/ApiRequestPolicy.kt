package com.resonote.core.network.protocol

import okhttp3.Request
import retrofit2.Invocation

/** Static protocol policy attached to a Retrofit endpoint. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class ApiRequestPolicy(
    val id: String,
    val signatureMode: ApiSignatureMode = ApiSignatureMode.Android,
    val sessionMode: ApiSessionMode = ApiSessionMode.Full,
    val includeDefaultParams: Boolean = true,
)

internal fun Request.apiRequestPolicy(): ApiRequestPolicy? =
    tag(Invocation::class.java)?.method()?.getAnnotation(ApiRequestPolicy::class.java)
