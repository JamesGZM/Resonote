package com.resonote.core.network.protocol

import okhttp3.Request
import retrofit2.Invocation

/** Static protocol policy attached to a Retrofit endpoint. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class ApiRequestPolicy(
    val signatureMode: ApiSignatureMode = ApiSignatureMode.Android,
    val sessionPropagation: ApiSessionPropagation = ApiSessionPropagation.Full,
    val includeDefaultParams: Boolean = true,
    val serviceAuthentication: ApiServiceAuthenticationPolicy = ApiServiceAuthenticationPolicy.None,
    val router: String = "",
    val kgTid: Int = 0,
)

internal fun Request.apiRequestPolicy(): ApiRequestPolicy? =
    tag(Invocation::class.java)?.method()?.getAnnotation(ApiRequestPolicy::class.java)
