package com.resonote.core.data

import com.resonote.core.model.AuthAccountOption
import com.resonote.core.model.AuthFailure
import com.resonote.core.model.AuthGateReason
import com.resonote.core.model.AuthState
import com.resonote.core.model.MobileCodeLoginResult
import com.resonote.core.model.PasswordLoginResult
import com.resonote.core.model.QrLoginCheckResult
import com.resonote.core.model.QrLoginKeyResult
import com.resonote.core.model.SendMobileCodeResult
import com.resonote.core.network.ApiException
import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.ApiNetworkException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.AuthNetworkDataSource
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.model.NetworkPasswordLoginResult
import com.resonote.core.network.model.NetworkQrLoginStatus
import com.resonote.core.network.session.ApiAuthenticationGateReason
import com.resonote.core.network.session.ApiSessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultAuthRepository @Inject constructor(
    private val network: AuthNetworkDataSource,
    private val sessionManager: ApiSessionManager,
    private val riskChallenges: RiskChallengeRegistry,
) : AuthRepository {
    override val authState: Flow<AuthState> =
        sessionManager.authenticationState.map { state ->
            val session = state.session
            when (state.gateReason) {
                ApiAuthenticationGateReason.LoginRequired -> AuthState.AuthenticationRequired(AuthGateReason.Required)
                ApiAuthenticationGateReason.SessionExpired -> AuthState.AuthenticationRequired(AuthGateReason.Expired)
                null -> if (session?.isAuthenticated == true) {
                    AuthState.Authenticated(requireNotNull(session.userId))
                } else {
                    AuthState.Anonymous
                }
            }
        }

    override suspend fun acknowledgeAuthenticationGate() = sessionManager.acknowledgeAuthenticationGate()

    override suspend fun logout() = sessionManager.clearAuthentication()

    override suspend fun sendMobileCode(mobile: String): SendMobileCodeResult {
        if (!MOBILE_PATTERN.matches(mobile)) return SendMobileCodeResult.Failed(AuthFailure.InvalidInput)
        return try {
            network.sendMobileCode(mobile)
            SendMobileCodeResult.Sent
        } catch (failure: ApiException) {
            SendMobileCodeResult.Failed(failure.toAuthFailure())
        }
    }

    override suspend fun loginWithMobileCode(
        mobile: String,
        code: String,
        selectedUserId: String?,
    ): MobileCodeLoginResult {
        if (!MOBILE_PATTERN.matches(mobile) ||
            code.isBlank() ||
            selectedUserId?.let { it.isBlank() || it == "0" } == true
        ) {
            return MobileCodeLoginResult.Failed(AuthFailure.InvalidInput)
        }
        return try {
            when (val result = network.loginWithMobileCode(mobile, code, selectedUserId)) {
                is NetworkMobileCodeLoginResult.Authenticated -> {
                    try {
                        sessionManager.write(result.session)
                        MobileCodeLoginResult.Authenticated
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        MobileCodeLoginResult.Failed(AuthFailure.SecureStorage)
                    }
                }

                is NetworkMobileCodeLoginResult.MultipleAccounts ->
                    MobileCodeLoginResult.MultipleAccounts(
                        result.accounts.map { AuthAccountOption(it.userId, it.nickname, it.avatarUrl, it.grade) },
                    )
            }
        } catch (failure: ApiException) {
            MobileCodeLoginResult.Failed(failure.toAuthFailure())
        }
    }

    override suspend fun loginWithPassword(username: String, password: String): PasswordLoginResult {
        if (username.isBlank() || password.isEmpty()) return PasswordLoginResult.Failed(AuthFailure.InvalidInput)
        return try {
            when (val result = network.loginWithPassword(username.trim(), password)) {
                is NetworkPasswordLoginResult.Authenticated -> {
                    try {
                        sessionManager.write(result.session)
                        PasswordLoginResult.Authenticated
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        PasswordLoginResult.Failed(AuthFailure.SecureStorage)
                    }
                }

                is NetworkPasswordLoginResult.MultipleAccounts ->
                    PasswordLoginResult.MultipleAccounts(
                        result.accounts.map { AuthAccountOption(it.userId, it.nickname, it.avatarUrl, it.grade) },
                    )
            }
        } catch (failure: ApiException) {
            PasswordLoginResult.Failed(failure.toAuthFailure())
        }
    }

    override suspend fun createQrLoginKey(): QrLoginKeyResult = try {
        QrLoginKeyResult.Ready(network.createQrLoginKey())
    } catch (failure: ApiException) {
        QrLoginKeyResult.Failed(failure.toAuthFailure())
    }

    override suspend fun checkQrLogin(key: String): QrLoginCheckResult {
        if (key.isBlank()) return QrLoginCheckResult.Failed(AuthFailure.InvalidInput)
        return try {
            when (val result = network.checkQrLogin(key.trim())) {
                NetworkQrLoginStatus.Waiting -> QrLoginCheckResult.Waiting
                NetworkQrLoginStatus.Expired -> QrLoginCheckResult.Expired
                is NetworkQrLoginStatus.Scanned -> QrLoginCheckResult.Scanned(result.nickname)
                is NetworkQrLoginStatus.Authenticated ->
                    try {
                        sessionManager.write(result.session)
                        QrLoginCheckResult.Authenticated
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        QrLoginCheckResult.Failed(AuthFailure.SecureStorage)
                    }
            }
        } catch (failure: ApiException) {
            QrLoginCheckResult.Failed(failure.toAuthFailure())
        }
    }

    private fun ApiException.toAuthFailure(): AuthFailure = when (this) {
        is ApiRiskException -> AuthFailure.RiskVerificationRequired(riskChallenges.register(challenge))
        is ApiNetworkException -> AuthFailure.Network
        is ApiServiceException, is ApiHttpException -> AuthFailure.ServiceRejected
        is ApiProtocolException -> AuthFailure.Protocol
        else -> AuthFailure.Protocol
    }

    private companion object {
        val MOBILE_PATTERN = Regex("^1\\d{10}$")
    }
}
