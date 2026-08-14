package com.resonote.core.datastore

import kotlinx.coroutines.flow.Flow

data class EncryptedSessionEnvelope(val schemaVersion: Int, val iv: ByteArray, val ciphertext: ByteArray) {
    override fun equals(other: Any?): Boolean = other is EncryptedSessionEnvelope &&
        schemaVersion == other.schemaVersion &&
        iv.contentEquals(other.iv) &&
        ciphertext.contentEquals(other.ciphertext)

    override fun hashCode(): Int = 31 * (31 * schemaVersion + iv.contentHashCode()) + ciphertext.contentHashCode()

    override fun toString(): String =
        "EncryptedSessionEnvelope(schemaVersion=$schemaVersion, ivBytes=${iv.size}, ciphertextBytes=${ciphertext.size})"
}

interface EncryptedSessionStorage {
    val data: Flow<EncryptedSessionEnvelope?>

    suspend fun write(envelope: EncryptedSessionEnvelope)

    suspend fun clear()
}

data class Ciphertext(val iv: ByteArray, val bytes: ByteArray)

interface SessionCipher {
    fun encrypt(plaintext: ByteArray): Ciphertext

    fun decrypt(ciphertext: Ciphertext): ByteArray

    /** Deletes unusable key material so the next write can create a fresh key. */
    fun reset()
}
