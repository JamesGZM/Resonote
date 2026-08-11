package com.resonote.core.network.protocol

import java.security.MessageDigest
import javax.inject.Inject

internal class ApiRequestSigner @Inject constructor() {
    fun sign(
        parameters: Map<String, String>,
        body: ByteArray = byteArrayOf(),
    ): String {
        val canonical = parameters.toSortedMap().entries.joinToString(separator = "") { (key, value) -> "$key=$value" }
        return digest(
            ApiProtocolConfig.ANDROID_SIGNATURE_SALT.encodeToByteArray(),
            canonical.encodeToByteArray(),
            body,
            ApiProtocolConfig.ANDROID_SIGNATURE_SALT.encodeToByteArray(),
        )
    }

    fun web(parameters: Map<String, String>): String {
        val canonical = parameters.toSortedMap().entries.joinToString(separator = "") { (key, value) -> "$key=$value" }
        return md5("${ApiProtocolConfig.WEB_SIGNATURE_SALT}$canonical${ApiProtocolConfig.WEB_SIGNATURE_SALT}")
    }

    fun register(parameters: Map<String, String>): String =
        md5(
            ApiProtocolConfig.REGISTER_SIGNATURE_SALT +
                parameters.values.sorted().joinToString(separator = "") +
                ApiProtocolConfig.REGISTER_SIGNATURE_SALT,
        )

    fun signParamsKey(value: String): String =
        md5("${ApiProtocolConfig.APP_ID}${ApiProtocolConfig.ANDROID_SIGNATURE_SALT}${ApiProtocolConfig.CLIENT_VERSION}$value")

    private fun digest(vararg parts: ByteArray): String =
        MessageDigest.getInstance("MD5").run {
            parts.forEach(::update)
            digest().toHex()
        }
}

internal fun ByteArray.toHex(uppercase: Boolean = false): String =
    joinToString(separator = "") { byte -> "%02x".format(byte) }.let { if (uppercase) it.uppercase() else it }
