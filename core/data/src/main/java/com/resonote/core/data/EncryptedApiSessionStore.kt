package com.resonote.core.data

import com.resonote.core.datastore.Ciphertext
import com.resonote.core.datastore.EncryptedSessionEnvelope
import com.resonote.core.datastore.EncryptedSessionStorage
import com.resonote.core.datastore.SessionCipher
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

@Singleton
internal class EncryptedApiSessionStore @Inject constructor(
    private val storage: EncryptedSessionStorage,
    private val cipher: SessionCipher,
) : ApiSessionStore {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override val session: Flow<ApiSession?> =
        storage.data.map { envelope ->
            try {
                decode(envelope)
            } catch (_: Exception) {
                storage.clear()
                null
            }
        }.catch {
            storage.clear()
            emit(null)
        }

    override suspend fun read(): ApiSession? = session.first()

    override suspend fun write(session: ApiSession) = mutex.withLock {
        val encrypted = cipher.encrypt(json.encodeToString(ApiSession.serializer(), session).encodeToByteArray())
        storage.write(EncryptedSessionEnvelope(SCHEMA_VERSION, encrypted.iv, encrypted.bytes))
    }

    override suspend fun clearAuthentication() = mutex.withLock {
        val current = runCatching { session.first() }.getOrNull() ?: return@withLock
        val anonymous =
            current.copy(
                token = null,
                userId = null,
                cookies = current.cookies.filterKeys { it !in AUTH_COOKIE_NAMES },
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
        val AUTH_COOKIE_NAMES = setOf("token", "userid", "t1", "vip_type", "vip_token")
    }
}
