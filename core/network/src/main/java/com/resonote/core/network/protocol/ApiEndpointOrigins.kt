package com.resonote.core.network.protocol

import okhttp3.HttpUrl.Companion.toHttpUrl

internal data class ApiEndpointOrigins(
    val gateway: String = "https://gateway.kugou.com",
    val mobileCode: String = "http://login.user.kugou.com",
    val mobileLogin: String = "https://loginserviceretry.kugou.com",
    val deviceRegistration: String = "https://userservice.kugou.com",
    val riskVerification: String = "https://verifyservice.kugou.com",
)

internal fun interface ApiOriginPolicy {
    fun isAllowed(spec: ApiEndpointSpec): Boolean
}

internal class ProductionApiOriginPolicy : ApiOriginPolicy {
    override fun isAllowed(spec: ApiEndpointSpec): Boolean {
        val origin = spec.origin.toHttpUrl()
        val allowedLoginHttp =
            spec.cleartextPolicy == ApiCleartextPolicy.LoginMobileCode &&
                origin.scheme == "http" && origin.host == LOGIN_MOBILE_CODE_HOST && origin.port == 80
        return origin.scheme == "https" || allowedLoginHttp
    }

    private companion object {
        const val LOGIN_MOBILE_CODE_HOST = "login.user.kugou.com"
    }
}
