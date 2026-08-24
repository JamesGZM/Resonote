package com.resonote.core.network.session

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.protocol.ApiDeviceIdentityFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Optional

@OptIn(ExperimentalCoroutinesApi::class)
class ApiSessionManagerTest {
    @Test
    fun authenticatedFailureClearsOnlyAuthenticationAndPublishesExpiredGate() = runTest {
        val store = TestStore(authenticatedSession())
        val manager = ApiSessionManager(Optional.of(store), ApiDeviceIdentityFactory())
        manager.current()

        val reason = manager.reportAuthenticationFailure()

        assertThat(reason).isEqualTo(ApiAuthenticationGateReason.SessionExpired)
        assertThat(manager.authenticationState.first { it.gateReason != null }.gateReason)
            .isEqualTo(ApiAuthenticationGateReason.SessionExpired)
        with(requireNotNull(store.read())) {
            assertThat(isAuthenticated).isFalse()
            assertThat(dfid).isEqualTo("fixture-dfid")
            assertThat(mid).isEqualTo("fixture-mid")
            assertThat(cookies).containsExactly("dfid", "fixture-dfid", "device", "kept")
        }
    }

    @Test
    fun expirationDoesNotPublishAnUngatedAnonymousIntermediateState() = runTest {
        val manager = ApiSessionManager(Optional.of(TestStore(authenticatedSession())), ApiDeviceIdentityFactory())
        manager.current()
        val states = mutableListOf<ApiAuthenticationState>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.authenticationState.toList(states)
        }

        manager.reportAuthenticationFailure()
        advanceUntilIdle()
        collector.cancelAndJoin()

        assertThat(states).doesNotContain(
            ApiAuthenticationState(
                session = anonymousSession(),
                gateReason = null,
            ),
        )
    }

    @Test
    fun failedAuthenticationClearRollsBackTheGateAndKeepsTheSession() = runTest {
        val store = TestStore(authenticatedSession()).apply { failClear = true }
        val manager = ApiSessionManager(Optional.of(store), ApiDeviceIdentityFactory())
        manager.current()

        val failure = runCatching { manager.reportAuthenticationFailure() }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        assertThat(manager.authenticationState.first().gateReason).isNull()
        assertThat(store.read()).isEqualTo(authenticatedSession())
    }

    @Test
    fun anonymousAndRepeatedFailuresKeepOneGateAndDoNotRepeatClearing() = runTest {
        val store = TestStore(authenticatedSession())
        val manager = ApiSessionManager(Optional.of(store), ApiDeviceIdentityFactory())
        manager.current()

        val reasons = List(8) { async { manager.reportAuthenticationFailure() } }.awaitAll()

        assertThat(reasons).containsExactlyElementsIn(List(8) { ApiAuthenticationGateReason.SessionExpired })
        assertThat(store.clearCount).isEqualTo(1)
        manager.acknowledgeAuthenticationGate()
        assertThat(manager.authenticationState.first().gateReason).isNull()

        val anonymousReason = manager.reportAuthenticationFailure()
        assertThat(anonymousReason).isEqualTo(ApiAuthenticationGateReason.LoginRequired)
        assertThat(store.clearCount).isEqualTo(1)
    }

    @Test
    fun successfulSessionWriteClearsExistingGate() = runTest {
        val store = TestStore(anonymousSession())
        val manager = ApiSessionManager(Optional.of(store), ApiDeviceIdentityFactory())
        manager.current()
        manager.reportAuthenticationFailure()

        manager.write(authenticatedSession())

        val state = manager.authenticationState.first()
        assertThat(state.gateReason).isNull()
        assertThat(state.session?.isAuthenticated).isTrue()
    }

    @Test
    fun explicitAuthenticationClearPublishesAnonymousStateWithoutAGate() = runTest {
        val store = TestStore(authenticatedSession())
        val manager = ApiSessionManager(Optional.of(store), ApiDeviceIdentityFactory())
        manager.current()

        manager.clearAuthentication()

        val state = manager.authenticationState.first()
        assertThat(state.session?.isAuthenticated).isFalse()
        assertThat(state.gateReason).isNull()
        assertThat(store.clearCount).isEqualTo(1)
        assertThat(state.session?.dfid).isEqualTo("fixture-dfid")
    }

    @Test
    fun authenticationStateReflectsStoreChangesOutsideTheManager() = runTest {
        val store = TestStore(authenticatedSession())
        val manager = ApiSessionManager(Optional.of(store), ApiDeviceIdentityFactory())
        manager.current()
        val staleRequest = manager.authenticationContext()
        val replacement = authenticatedSession().copy(token = "replacement-token")

        store.replace(replacement)

        assertThat(manager.authenticationState.first { it.session == replacement })
            .isEqualTo(ApiAuthenticationState(replacement))
        assertThat(manager.reportAuthenticationFailure(staleRequest)).isNull()
        assertThat(manager.snapshot()).isEqualTo(replacement)
        assertThat(store.clearCount).isEqualTo(0)
    }

    @Test
    fun staleFailureCannotClearAnExternalStoreReplacementWithoutAFlowCollector() = runTest {
        val store = TestStore(authenticatedSession())
        val manager = ApiSessionManager(Optional.of(store), ApiDeviceIdentityFactory())
        manager.current()
        val staleRequest = manager.authenticationContext()
        val replacement = authenticatedSession().copy(token = "replacement-token")

        store.replace(replacement)
        val reason = manager.reportAuthenticationFailure(staleRequest)

        assertThat(reason).isNull()
        assertThat(manager.snapshot()).isEqualTo(replacement)
        assertThat(store.clearCount).isEqualTo(0)
    }

    @Test
    fun staleFailureCannotClearANewerAuthenticatedSession() = runTest {
        val store = TestStore(authenticatedSession())
        val manager = ApiSessionManager(Optional.of(store), ApiDeviceIdentityFactory())
        manager.current()
        val staleRequest = manager.authenticationContext()
        val newerSession = authenticatedSession().copy(
            token = "new-token",
            cookies = mapOf(
                "token" to "new-token",
                "userid" to "42",
            ),
        )

        manager.write(newerSession)
        val reason = manager.reportAuthenticationFailure(staleRequest)

        assertThat(reason).isNull()
        assertThat(store.read()).isEqualTo(newerSession)
        assertThat(manager.authenticationState.first().gateReason).isNull()
        assertThat(store.clearCount).isEqualTo(0)
    }

    @Test
    fun localAuthenticationCheckUsesTheLatestSessionInsteadOfAStaleAnonymousRead() = runTest {
        val store = TestStore(anonymousSession())
        val manager = ApiSessionManager(Optional.of(store), ApiDeviceIdentityFactory())
        val staleAnonymousSession = manager.current()
        assertThat(staleAnonymousSession.isAuthenticated).isFalse()
        val newerSession = authenticatedSession().copy(token = "new-token")

        manager.write(newerSession)
        val resolved = manager.authenticatedSessionOrReportRequired()

        assertThat(resolved).isEqualTo(newerSession)
        assertThat(store.read()).isEqualTo(newerSession)
        assertThat(manager.authenticationState.first().gateReason).isNull()
        assertThat(store.clearCount).isEqualTo(0)
    }

    private class TestStore(initial: ApiSession) : ApiSessionStore {
        private val state = MutableStateFlow<ApiSession?>(initial)
        override val session = state
        var clearCount = 0
        var failClear = false

        override suspend fun read(): ApiSession? = state.value

        override suspend fun write(session: ApiSession) {
            state.value = session
        }

        override suspend fun clearAuthentication() {
            clearCount += 1
            check(!failClear) { "fixture clear failure" }
            state.value = state.value?.copy(
                token = null,
                userId = null,
                cookies = state.value?.cookies.orEmpty().filterKeys { it !in AUTH_KEYS },
            )
        }

        fun replace(session: ApiSession?) {
            state.value = session
        }
    }

    private fun authenticatedSession() = anonymousSession().copy(
        token = "secret-token",
        userId = "42",
        cookies = mapOf(
            "token" to "secret-token",
            "userid" to "42",
            "t1" to "secret-t1",
            "vip_type" to "1",
            "vip_token" to "secret-vip",
            "dfid" to "fixture-dfid",
            "device" to "kept",
        ),
    )

    private fun anonymousSession() = ApiSession(
        guid = "fixture-guid",
        mid = "fixture-mid",
        dev = "fixture-dev",
        dfid = "fixture-dfid",
        cookies = mapOf("dfid" to "fixture-dfid", "device" to "kept"),
    )

    private companion object {
        val AUTH_KEYS = setOf("token", "userid", "t1", "vip_type", "vip_token")
    }
}
