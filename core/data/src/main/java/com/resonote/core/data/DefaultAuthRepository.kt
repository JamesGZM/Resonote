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
) : AuthRepository {
    override val authState: Flow<AuthState> =
        sessionStore.session.map { session ->
            if (session?.isAuthenticated == true) AuthState.Authenticated(requireNotNull(session.userId)) else AuthState.Anonymous
        }

    override suspend fun sendMobileCode(mobile: String): SendMobileCodeResult =
        try {
            network.sendMobileCode(mobile)
            SendMobileCodeResult.Sent
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            SendMobileCodeResult.Failed(failure.toAuthFailure())
        }

    override suspend fun loginWithMobileCode(
        mobile: String,
        code: String,
        selectedUserId: String?,
    ): MobileCodeLoginResult =
        try {
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
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            MobileCodeLoginResult.Failed(failure.toAuthFailure())
        }

    private fun Throwable.toAuthFailure(): AuthFailure =
        when (this) {
            is IllegalArgumentException -> AuthFailure.InvalidInput
            is ApiRiskException -> AuthFailure.RiskVerificationUnavailable
            is ApiNetworkException -> AuthFailure.Network
            is ApiServiceException, is ApiHttpException -> AuthFailure.ServiceRejected
            is ApiProtocolException, is ApiException -> AuthFailure.Protocol
            else -> AuthFailure.Protocol
        }
}
