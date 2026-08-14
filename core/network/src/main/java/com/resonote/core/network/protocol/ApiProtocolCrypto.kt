package com.resonote.core.network.protocol

import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

internal fun interface ProtocolRandom {
    fun string(length: Int): String
}

internal data class TemporaryEncryption(val ciphertextHex: String, val temporaryKey: String)

internal data class PlaylistEncryption(val ciphertext: ByteArray, val key: String)

@Singleton
internal class ApiProtocolCrypto @Inject constructor(private val random: ProtocolRandom) {
    fun encryptTemporary(plaintext: String): TemporaryEncryption {
        val temporaryKey = random.string(16).lowercase()
        return TemporaryEncryption(
            ciphertextHex = temporaryAes(plaintext.encodeToByteArray(), temporaryKey, Cipher.ENCRYPT_MODE).toHex(),
            temporaryKey = temporaryKey,
        )
    }

    fun decryptTemporary(ciphertextHex: String, temporaryKey: String): String =
        temporaryAes(ciphertextHex.hexToBytes(), temporaryKey, Cipher.DECRYPT_MODE).decodeToString()

    fun fixedAesHex(plaintext: String, key: String, iv: String): String =
        aes(plaintext.encodeToByteArray(), key.encodeToByteArray(), iv.encodeToByteArray(), Cipher.ENCRYPT_MODE).toHex()

    fun rawLiteRsa(plaintext: String): String {
        val bytes = plaintext.encodeToByteArray()
        require(bytes.size <= RSA_BYTES) { "RSA plaintext exceeds Lite key size" }
        val padded = ByteArray(RSA_BYTES)
        bytes.copyInto(padded)
        return rsa("RSA/ECB/NoPadding", padded).toHex()
    }

    fun pkcs1LiteRsa(plaintext: String): String = rsa("RSA/ECB/PKCS1Padding", plaintext.encodeToByteArray()).toHex()

    fun encryptPlaylist(plaintext: String): PlaylistEncryption {
        val key = random.string(6).lowercase()
        val digest = md5Bytes(key.encodeToByteArray()).toHex()
        return PlaylistEncryption(
            ciphertext = aes(
                plaintext.encodeToByteArray(),
                digest.substring(0, 16).encodeToByteArray(),
                digest.substring(16, 32).encodeToByteArray(),
                Cipher.ENCRYPT_MODE,
            ),
            key = key,
        )
    }

    fun decryptPlaylist(ciphertext: ByteArray, key: String): String {
        val digest = md5Bytes(key.encodeToByteArray()).toHex()
        return aes(
            ciphertext,
            digest.substring(0, 16).encodeToByteArray(),
            digest.substring(16, 32).encodeToByteArray(),
            Cipher.DECRYPT_MODE,
        ).decodeToString()
    }

    private fun temporaryAes(input: ByteArray, temporaryKey: String, mode: Int): ByteArray {
        val key = md5Bytes(temporaryKey.encodeToByteArray()).toHex().substring(0, 32)
        return aes(input, key.encodeToByteArray(), key.takeLast(16).encodeToByteArray(), mode)
    }

    private fun aes(input: ByteArray, key: ByteArray, iv: ByteArray, mode: Int): ByteArray =
        Cipher.getInstance("AES/CBC/PKCS5Padding").run {
            init(mode, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            doFinal(input)
        }

    private fun rsa(transformation: String, plaintext: ByteArray): ByteArray = Cipher.getInstance(transformation).run {
        init(Cipher.ENCRYPT_MODE, litePublicKey)
        doFinal(plaintext)
    }

    private val litePublicKey: PublicKey by lazy {
        val der = Base64.getDecoder().decode(LITE_PUBLIC_KEY_BODY)
        KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(der))
    }

    private companion object {
        const val RSA_BYTES = 128
        const val LITE_PUBLIC_KEY_BODY =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDECi0Np2UR87scwrvTr72L6oO01rBbbBPriSDFPxr3Z5syug0O24QyQO8bg27+0+4kBzTBTBOZ/WWU0WryL1JSXRTXLgFVxtzIY41Pe7lPOgsfTCn5kZcvKhYKJesKnnJDNr5/abvTGf+rHG3YRwsCHcQ08/q6ifSioBszvb3QiwIDAQAB"
    }
}

private fun md5Bytes(value: ByteArray): ByteArray = MessageDigest.getInstance("MD5").digest(value)

private fun String.hexToBytes(): ByteArray {
    require(
        length % 2 == 0 &&
            all {
                it.isDigit() || it.lowercaseChar() in 'a'..'f'
            },
    ) { "Ciphertext must be hexadecimal" }
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
