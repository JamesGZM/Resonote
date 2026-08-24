package com.resonote.core.network.session

import com.resonote.core.network.protocol.ApiDeviceIdentityFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiSessionManager @Inject constructor(
    store: Optional<ApiSessionStore>,
    private val identityFactory: ApiDeviceIdentityFactory,
) {
    private val resolvedStore = store.orElseGet(::MemoryApiSessionStore)
    private val mutex = Mutex()

    @Volatile
    private var snapshot: ApiSession? = null

    @Volatile
    private var revision: Long = 0
    private var lastFailureRevision: Long? = null
    private val gateReason = MutableStateFlow<ApiAuthenticationGateReason?>(null)

    val authenticationState: Flow<ApiAuthenticationState> =
        combine(resolvedStore.session, gateReason) { _, _ ->
            mutex.withLock {
                val current = readLatestSessionLocked()
                ApiAuthenticationState(current, gateReason.value)
            }
        }.distinctUntilChanged()
    val session: Flow<ApiSession?> = authenticationState.map { it.session }.distinctUntilChanged()

    suspend fun current(): ApiSession = mutex.withLock {
        (
            readLatestSessionLocked() ?: ApiSession.anonymous(identityFactory).also {
                resolvedStore.write(it)
                revision += 1
            }
            ).also { snapshot = it }
    }

    suspend fun write(value: ApiSession) = mutex.withLock {
        resolvedStore.write(value)
        snapshot = value
        revision += 1
        lastFailureRevision = null
        gateReason.value = null
    }

    suspend fun reportAuthenticationFailure(): ApiAuthenticationGateReason = mutex.withLock {
        reportAuthenticationFailureLocked(expectedRevision = null)
            ?: error("Unconditional authentication failure must be reported")
    }

    internal suspend fun authenticatedSessionOrReportRequired(): ApiSession? = mutex.withLock {
        val current = readLatestSessionLocked()
        if (current?.isAuthenticated == true) return@withLock current
        lastFailureRevision = revision
        gateReason.compareAndSet(null, ApiAuthenticationGateReason.LoginRequired)
        null
    }

    internal suspend fun reportAuthenticationFailure(context: ApiAuthenticationContext): ApiAuthenticationGateReason? =
        mutex.withLock {
            reportAuthenticationFailureLocked(context.revision)
        }

    internal fun authenticationContext(): ApiAuthenticationContext {
        checkNotNull(snapshot) { "API session must be initialized before capturing authentication context" }
        return ApiAuthenticationContext(revision)
    }

    private suspend fun reportAuthenticationFailureLocked(expectedRevision: Long?): ApiAuthenticationGateReason? {
        val current = readLatestSessionLocked()
        if (expectedRevision != null && expectedRevision != revision) {
            return gateReason.value.takeIf { lastFailureRevision == expectedRevision }
        }
        val reason =
            if (current?.isAuthenticated == true) {
                val previousFailureRevision = lastFailureRevision
                val previousGateReason = gateReason.value
                lastFailureRevision = revision
                gateReason.value = ApiAuthenticationGateReason.SessionExpired
                try {
                    resolvedStore.clearAuthentication()
                    snapshot = resolvedStore.read()
                    revision += 1
                    ApiAuthenticationGateReason.SessionExpired
                } catch (failure: Exception) {
                    lastFailureRevision = previousFailureRevision
                    gateReason.value = previousGateReason
                    throw failure
                }
            } else {
                lastFailureRevision = revision
                ApiAuthenticationGateReason.LoginRequired
            }
        if (reason == ApiAuthenticationGateReason.LoginRequired) {
            gateReason.compareAndSet(null, reason)
        }
        return gateReason.value ?: reason
    }

    private suspend fun readLatestSessionLocked(): ApiSession? = resolvedStore.read().also { current ->
        if (current != snapshot) revision += 1
        snapshot = current
    }

    suspend fun acknowledgeAuthenticationGate() = mutex.withLock { gateReason.value = null }

    suspend fun clearAuthentication() = mutex.withLock {
        resolvedStore.clearAuthentication()
        snapshot = resolvedStore.read()
        revision += 1
        lastFailureRevision = null
        gateReason.value = null
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
        state.value = state.value?.copy(
            token = null,
            userId = null,
            cookies = state.value?.cookies.orEmpty().filterKeys {
                it.lowercase() !in apiAuthenticationCookieNames
            },
        )
    }
}
