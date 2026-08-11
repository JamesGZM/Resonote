package com.resonote.core.network.protocol

import javax.inject.Inject

internal class ApiRequestSigner @Inject constructor() {
    fun sign(
        parameters: Map<String, String>,
        body: String = "",
    ): String {
        val canonical = parameters.toSortedMap().entries.joinToString(separator = "") { (key, value) -> "$key=$value" }
        return md5("${ApiProtocolConfig.ANDROID_SIGNATURE_SALT}$canonical$body${ApiProtocolConfig.ANDROID_SIGNATURE_SALT}")
    }
}
