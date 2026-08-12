package com.resonote.app

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.AuthRepository
import com.resonote.core.model.AuthGateReason
import com.resonote.core.model.AuthState
import com.resonote.core.model.MobileCodeLoginResult
import com.resonote.core.model.PasswordLoginResult
import com.resonote.core.model.QrLoginCheckResult
import com.resonote.core.model.QrLoginKeyResult
import com.resonote.core.model.SendMobileCodeResult
import com.resonote.core.navigation.LoginGateNavKey
import com.resonote.core.navigation.TabsShellNavKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun requiredExpiredAcknowledgedAndAuthenticatedStatesRemainCentralized() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = MainActivityViewModel(repository)

        repository.state.value = AuthState.AuthenticationRequired(AuthGateReason.Required)
        assertThat(viewModel.authState.value).isEqualTo(AuthState.AuthenticationRequired(AuthGateReason.Required))

        repository.state.value = AuthState.AuthenticationRequired(AuthGateReason.Expired)
        assertThat(viewModel.authState.value).isEqualTo(AuthState.AuthenticationRequired(AuthGateReason.Expired))

        viewModel.acknowledgeAuthenticationGate()
        assertThat(repository.acknowledgements).isEqualTo(1)

        repository.state.value = AuthState.Authenticated("42")
        assertThat(viewModel.authState.value).isEqualTo(AuthState.Authenticated("42"))
    }

    @Test
    fun authenticationGateIsUniqueUpdatesItsReasonAndClearsWithAuthState() {
        val backStack = mutableListOf<NavKey>(TabsShellNavKey)

        backStack.synchronizeAuthenticationGate(AuthState.AuthenticationRequired(AuthGateReason.Required))
        backStack.synchronizeAuthenticationGate(AuthState.AuthenticationRequired(AuthGateReason.Required))
        assertThat(backStack.filterIsInstance<LoginGateNavKey>()).containsExactly(LoginGateNavKey(false))

        backStack.add(TabsShellNavKey)
        backStack.synchronizeAuthenticationGate(AuthState.AuthenticationRequired(AuthGateReason.Expired))
        assertThat(backStack.filterIsInstance<LoginGateNavKey>()).containsExactly(LoginGateNavKey(true))
        assertThat(backStack.last()).isEqualTo(LoginGateNavKey(true))

        backStack.synchronizeAuthenticationGate(AuthState.Anonymous)
        assertThat(backStack.filterIsInstance<LoginGateNavKey>()).isEmpty()
        backStack.synchronizeAuthenticationGate(AuthState.AuthenticationRequired(AuthGateReason.Required))
        backStack.synchronizeAuthenticationGate(AuthState.Authenticated("42"))
        assertThat(backStack.filterIsInstance<LoginGateNavKey>()).isEmpty()
    }

    private class FakeAuthRepository : AuthRepository {
        val state = MutableStateFlow<AuthState>(AuthState.Anonymous)
        var acknowledgements = 0
        override val authState = state

        override suspend fun acknowledgeAuthenticationGate() {
            acknowledgements += 1
            state.value = AuthState.Anonymous
        }

        override suspend fun sendMobileCode(mobile: String): SendMobileCodeResult = error("unused")
        override suspend fun loginWithMobileCode(mobile: String, code: String, selectedUserId: String?): MobileCodeLoginResult = error("unused")
        override suspend fun loginWithPassword(username: String, password: String): PasswordLoginResult = error("unused")
        override suspend fun createQrLoginKey(): QrLoginKeyResult = error("unused")
        override suspend fun checkQrLogin(key: String): QrLoginCheckResult = error("unused")
    }
}
