package com.resonote.core.data

import com.resonote.core.datastore.Ciphertext
import com.resonote.core.datastore.EncryptedSessionEnvelope
import com.resonote.core.datastore.EncryptedSessionStorage
import com.resonote.core.datastore.SessionCipher
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionStore
import com.resonote.core.network.session.apiAuthenticationCookieNames
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EncryptedApiSessionStore @Inject constructor(
    private val storage: EncryptedSessionStorage,
    private val cipher: SessionCipher,
) : ApiSessionStore {
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val session: Flow<ApiSession?> =
        storage.data.map { envelope ->
            try {
                decode(envelope)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                cipher.reset()
                storage.clear()
                null
            }
        }.catch { failure ->
            if (failure is CancellationException) throw failure
            storage.clear()
            emit(null)
        }

    override suspend fun read(): ApiSession? = session.first()

    override suspend fun write(session: ApiSession) = mutex.withLock {
        val encrypted = cipher.encrypt(json.encodeToString(ApiSession.serializer(), session).encodeToByteArray())
        storage.write(EncryptedSessionEnvelope(SCHEMA_VERSION, encrypted.iv, encrypted.bytes))
    }

    override suspend fun clearAuthentication() = mutex.withLock {
        val current =
            try {
                session.first()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            } ?: return@withLock
        val anonymous =
            current.copy(
                token = null,
                userId = null,
                cookies = current.cookies.filterKeys { it.lowercase() !in apiAuthenticationCookieNames },
            )
        val encrypted = cipher.encrypt(json.encodeToString(ApiSession.serializer(), anonymous).encodeToByteArray())
        storage.write(EncryptedSessionEnvelope(SCHEMA_VERSION, encrypted.iv, encrypted.bytes))
    }

    private fun decode(envelope: EncryptedSessionEnvelope?): ApiSession? {
        if (envelope == null) return null
        require(envelope.schemaVersion == SCHEMA_VERSION) { "Unsupported API session schema" }
        val plaintext = cipher.decrypt(Ciphertext(envelope.iv, envelope.ciphertext))
        return json.decodeFromString(ApiSession.serializer(), plaintext.decodeToString())
    }

    private companion object {
        const val SCHEMA_VERSION = 1
    }
}
