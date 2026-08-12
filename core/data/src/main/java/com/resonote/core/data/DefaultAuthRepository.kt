package com.resonote.core.data

import com.resonote.core.model.AuthAccountOption
import com.resonote.core.model.AuthFailure
import com.resonote.core.model.AuthState
import com.resonote.core.model.MobileCodeLoginResult
import com.resonote.core.model.SendMobileCodeResult
import com.resonote.core.network.ApiException
import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.ApiNetworkDataSource
import com.resonote.core.network.ApiNetworkException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.session.ApiSessionStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
internal class DefaultAuthRepository @Inject constructor(
    private val network: ApiNetworkDataSource,
    private val sessionStore: ApiSessionStore,
    private val riskChallenges: RiskChallengeRegistry,
) : AuthRepository {
    override val authState: Flow<AuthState> =
        sessionStore.session.map { session ->
            if (session?.isAuthenticated == true) AuthState.Authenticated(requireNotNull(session.userId)) else AuthState.Anonymous
        }

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
        if (!MOBILE_PATTERN.matches(mobile) || code.isBlank() || selectedUserId?.let { it.isBlank() || it == "0" } == true) {
            return MobileCodeLoginResult.Failed(AuthFailure.InvalidInput)
        }
        return try {
            when (val result = network.loginWithMobileCode(mobile, code, selectedUserId)) {
                is NetworkMobileCodeLoginResult.Authenticated -> {
                    try {
                        sessionStore.write(result.session)
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

    private fun ApiException.toAuthFailure(): AuthFailure =
        when (this) {
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
