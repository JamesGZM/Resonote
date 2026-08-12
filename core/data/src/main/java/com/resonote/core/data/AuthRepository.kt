package com.resonote.core.data

import com.resonote.core.model.AuthState
import com.resonote.core.model.MobileCodeLoginResult
import com.resonote.core.model.PasswordLoginResult
import com.resonote.core.model.SendMobileCodeResult
import com.resonote.core.model.QrLoginCheckResult
import com.resonote.core.model.QrLoginKeyResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>

    suspend fun acknowledgeAuthenticationGate()

    suspend fun sendMobileCode(mobile: String): SendMobileCodeResult

    suspend fun loginWithMobileCode(
        mobile: String,
        code: String,
        selectedUserId: String? = null,
    ): MobileCodeLoginResult

    suspend fun loginWithPassword(
        username: String,
        password: String,
    ): PasswordLoginResult

    suspend fun createQrLoginKey(): QrLoginKeyResult

    suspend fun checkQrLogin(key: String): QrLoginCheckResult
}
