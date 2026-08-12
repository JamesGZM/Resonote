package com.resonote.core.network.session

import com.resonote.core.network.protocol.ApiDeviceIdentityFactory
import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
internal class ApiSessionManager @Inject constructor(
    store: Optional<ApiSessionStore>,
    private val identityFactory: ApiDeviceIdentityFactory,
) {
    private val store = store.orElseGet(::MemoryApiSessionStore)
    private val mutex = Mutex()

    @Volatile
    private var snapshot: ApiSession? = null

    val session: Flow<ApiSession?> get() = store.session

    suspend fun current(): ApiSession = mutex.withLock {
        (store.read() ?: ApiSession.anonymous(identityFactory).also { store.write(it) }).also { snapshot = it }
    }

    suspend fun write(value: ApiSession) = mutex.withLock {
        store.write(value)
        snapshot = value
    }

    fun snapshot(): ApiSession = checkNotNull(snapshot) { "API session must be initialized before a Retrofit request" }
}

private class MemoryApiSessionStore : ApiSessionStore {
    private val state = MutableStateFlow<ApiSession?>(null)
    override val session: Flow<ApiSession?> = state

    override suspend fun read(): ApiSession? = state.value

    override suspend fun write(session: ApiSession) {
        state.value = session
    }

    override suspend fun clearAuthentication() {
        state.value = state.value?.copy(token = null, userId = null, cookies = state.value?.cookies.orEmpty().filterKeys { it !in AUTH_KEYS })
    }

    private companion object {
        val AUTH_KEYS = setOf("token", "userid", "t1", "vip_type", "vip_token")
    }
}
