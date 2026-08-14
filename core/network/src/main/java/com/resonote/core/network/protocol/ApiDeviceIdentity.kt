package com.resonote.core.network.protocol

import java.math.BigInteger
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ApiDeviceIdentity(val guid: String, val mid: String, val dev: String)

@Singleton
class ApiDeviceIdentityFactory @Inject constructor() {
    fun create(): ApiDeviceIdentity {
        val guid = md5(UUID.randomUUID().toString())
        return ApiDeviceIdentity(
            guid = guid,
            mid = BigInteger(md5(guid), 16).toString(),
            dev = randomProtocolString(10),
        )
    }
}

internal fun md5(value: String): String = MessageDigest.getInstance("MD5")
    .digest(value.toByteArray(Charsets.UTF_8))
    .joinToString(separator = "") { byte -> "%02x".format(byte) }

internal fun randomProtocolString(length: Int): String {
    val alphabet = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val random = java.security.SecureRandom()
    return buildString(length) { repeat(length) { append(alphabet[random.nextInt(alphabet.length)]) } }
}
