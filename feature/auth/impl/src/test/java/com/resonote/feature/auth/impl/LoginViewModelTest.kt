package com.resonote.feature.auth.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.AuthRepository
import com.resonote.core.model.AuthAccountOption
import com.resonote.core.model.AuthFailure
import com.resonote.core.model.AuthState
import com.resonote.core.model.MobileCodeLoginResult
import com.resonote.core.model.PasswordLoginResult
import com.resonote.core.model.QrLoginCheckResult
import com.resonote.core.model.QrLoginKeyResult
import com.resonote.core.model.SendMobileCodeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun mobileInputIsNormalizedAndCodeCanOnlyBeSentForValidNumber() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(repository)

        viewModel.updateMobile("13a8 0000-00001")
        assertThat(viewModel.uiState.value.mobile).isEqualTo("13800000000")
        assertThat(viewModel.uiState.value.canSendCode).isTrue()

        viewModel.sendCode()
        advanceUntilIdle()

        assertThat(repository.sentCodeMobiles).containsExactly("13800000000")
        assertThat(viewModel.uiState.value.message).isEqualTo(LoginMessage.CodeSent)
    }

    @Test
    fun mobileLoginOffersAccountsThenResubmitsSelectedUser() = runTest(dispatcher) {
        val accounts = listOf(
            AuthAccountOption("42", "海岸线", null, "VIP 8"),
            AuthAccountOption("84", "凌晨电台", null, null),
        )
        val repository = FakeAuthRepository(
            mobileResults = ArrayDeque(
                listOf(MobileCodeLoginResult.MultipleAccounts(accounts), MobileCodeLoginResult.Authenticated),
            ),
        )
        val viewModel = LoginViewModel(repository)
        viewModel.updateMobile("13800000000")
        viewModel.updateCode("246810")

        viewModel.login()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.accounts).containsExactlyElementsIn(accounts)

        viewModel.selectAccount("84")
        advanceUntilIdle()

        assertThat(repository.mobileRequests).containsExactly(
            MobileRequest("13800000000", "246810", null),
            MobileRequest("13800000000", "246810", "84"),
        ).inOrder()
        assertThat(viewModel.uiState.value.code).isEmpty()
    }

    @Test
    fun passwordLoginTrimsUsernameClearsPasswordAndKeepsUsername() = runTest(dispatcher) {
        val repository = FakeAuthRepository(passwordResult = PasswordLoginResult.Authenticated)
        val viewModel = LoginViewModel(repository)
        viewModel.selectMethod(LoginMethod.Password)
        viewModel.updateUsername("  listener@example.com ")
        viewModel.updatePassword("never-log-this")

        viewModel.login()
        advanceUntilIdle()

        assertThat(repository.passwordRequests).containsExactly("listener@example.com" to "never-log-this")
        assertThat(viewModel.uiState.value.username).isEqualTo("  listener@example.com ")
        assertThat(viewModel.uiState.value.password).isEmpty()
    }

    @Test
    fun failuresMapToActionableUiMessages() = runTest(dispatcher) {
        val repository = FakeAuthRepository(
            sendCodeResult = SendMobileCodeResult.Failed(AuthFailure.Network),
            passwordResult = PasswordLoginResult.MultipleAccounts(emptyList()),
        )
        val viewModel = LoginViewModel(repository)
        viewModel.updateMobile("13800000000")
        viewModel.sendCode()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.message).isEqualTo(LoginMessage.Network)

        viewModel.selectMethod(LoginMethod.Password)
        viewModel.updateUsername("listener")
        viewModel.updatePassword("secret")
        viewModel.login()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.message).isEqualTo(LoginMessage.PasswordMultipleAccounts)
    }

    private data class MobileRequest(val mobile: String, val code: String, val selectedUserId: String?)

    private class FakeAuthRepository(
        private val sendCodeResult: SendMobileCodeResult = SendMobileCodeResult.Sent,
        private val mobileResults: ArrayDeque<MobileCodeLoginResult> = ArrayDeque(
            listOf(MobileCodeLoginResult.Authenticated),
        ),
        private val passwordResult: PasswordLoginResult = PasswordLoginResult.Failed(AuthFailure.ServiceRejected),
    ) : AuthRepository {
        override val authState = MutableStateFlow<AuthState>(AuthState.Anonymous)
        val sentCodeMobiles = mutableListOf<String>()
        val mobileRequests = mutableListOf<MobileRequest>()
        val passwordRequests = mutableListOf<Pair<String, String>>()

        override suspend fun acknowledgeAuthenticationGate() = Unit

        override suspend fun sendMobileCode(mobile: String): SendMobileCodeResult {
            sentCodeMobiles += mobile
            return sendCodeResult
        }

        override suspend fun loginWithMobileCode(
            mobile: String,
            code: String,
            selectedUserId: String?,
        ): MobileCodeLoginResult {
            mobileRequests += MobileRequest(mobile, code, selectedUserId)
            return mobileResults.removeFirst()
        }

        override suspend fun loginWithPassword(username: String, password: String): PasswordLoginResult {
            passwordRequests += username to password
            return passwordResult
        }

        override suspend fun createQrLoginKey(): QrLoginKeyResult = error("unused")

        override suspend fun checkQrLogin(key: String): QrLoginCheckResult = error("unused")
    }
}
