package com.resonote.core.network

import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.model.NetworkPasswordLoginResult
import com.resonote.core.network.model.NetworkQrLoginStatus

interface AuthNetworkDataSource {
    suspend fun sendMobileCode(mobile: String)
    suspend fun loginWithMobileCode(
        mobile: String,
        code: String,
        selectedUserId: String? = null,
    ): NetworkMobileCodeLoginResult
    suspend fun loginWithPassword(username: String, password: String): NetworkPasswordLoginResult
    suspend fun createQrLoginKey(): String
    suspend fun checkQrLogin(key: String): NetworkQrLoginStatus
}
