package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.datastore.Ciphertext
import com.resonote.core.datastore.EncryptedSessionEnvelope
import com.resonote.core.datastore.EncryptedSessionStorage
import com.resonote.core.datastore.SessionCipher
import com.resonote.core.network.session.ApiSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class EncryptedApiSessionStoreTest {
    @Test
    fun roundTripPersistsOnlyEncryptedEnvelopeAndCanClearAuthentication() = runTest {
        val storage = FakeStorage()
        val store = EncryptedApiSessionStore(storage, XorCipher())
        val session = authenticatedSession()

        store.write(session)

        assertThat(storage.state.value?.ciphertext?.decodeToString()).doesNotContain("secret-token")
        assertThat(store.read()).isEqualTo(session)

        store.clearAuthentication()
        val anonymous = store.read()
        assertThat(anonymous?.isAuthenticated).isFalse()
        assertThat(anonymous?.cookies).containsExactly("dfid", "device-dfid")
    }

    @Test
    fun corruptedCiphertextIsClearedAndReturnsAnonymousState() = runTest {
        val storage = FakeStorage(EncryptedSessionEnvelope(1, byteArrayOf(1), "broken".encodeToByteArray()))
        val cipher = InvalidatableCipher()
        val store = EncryptedApiSessionStore(storage, cipher)

        assertThat(store.read()).isNull()
        assertThat(storage.clearCount).isEqualTo(1)
        assertThat(cipher.resetCount).isEqualTo(1)

        store.write(authenticatedSession())
        assertThat(store.read()).isEqualTo(authenticatedSession())
    }

    @Test
    fun cancelledReadPropagatesWithoutClearingSession() {
        val storage = CancellingStorage()
        val store = EncryptedApiSessionStore(storage, XorCipher())

        assertThrows(CancellationException::class.java) { runTest { store.read() } }
        assertThat(storage.clearCount).isEqualTo(0)
    }

    private class FakeStorage(initial: EncryptedSessionEnvelope? = null) : EncryptedSessionStorage {
        val state = MutableStateFlow(initial)
        var clearCount = 0
        override val data = state
        override suspend fun write(envelope: EncryptedSessionEnvelope) { state.value = envelope }
        override suspend fun clear() { clearCount += 1; state.value = null }
    }

    private class XorCipher : SessionCipher {
        override fun encrypt(plaintext: ByteArray) = Ciphertext(byteArrayOf(7), plaintext.map { (it.toInt() xor 0x55).toByte() }.toByteArray())
        override fun decrypt(ciphertext: Ciphertext) = ciphertext.bytes.map { (it.toInt() xor 0x55).toByte() }.toByteArray()
        override fun reset() = Unit
    }

    private class CancellingStorage : EncryptedSessionStorage {
        var clearCount = 0
        override val data: Flow<EncryptedSessionEnvelope?> = flow { throw CancellationException("cancelled") }
        override suspend fun write(envelope: EncryptedSessionEnvelope) = Unit
        override suspend fun clear() { clearCount += 1 }
    }

    private class InvalidatableCipher : SessionCipher {
        private var invalid = true
        var resetCount = 0
        override fun encrypt(plaintext: ByteArray): Ciphertext {
            check(!invalid) { "key invalidated" }
            return Ciphertext(byteArrayOf(1), plaintext)
        }
        override fun decrypt(ciphertext: Ciphertext): ByteArray {
            check(!invalid) { "key invalidated" }
            return ciphertext.bytes
        }
        override fun reset() { invalid = false; resetCount += 1 }
    }

    private fun authenticatedSession() = ApiSession(
        guid = "fixture-guid",
        mid = "fixture-mid",
        dev = "fixture-dev",
        dfid = "device-dfid",
        token = "secret-token",
        userId = "42",
        cookies = mapOf("dfid" to "device-dfid", "token" to "secret-token", "userid" to "42"),
    )
}
