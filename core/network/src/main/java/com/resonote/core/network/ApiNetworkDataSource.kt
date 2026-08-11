package com.resonote.core.network

import com.resonote.core.network.model.NetworkSearchPage
import com.resonote.core.network.model.NetworkMobileCodeLoginResult

interface ApiNetworkDataSource {
    suspend fun searchSongs(
        keywords: String,
        page: Int = 1,
        pageSize: Int = 30,
    ): NetworkSearchPage

    suspend fun sendMobileCode(mobile: String)

    suspend fun loginWithMobileCode(
        mobile: String,
        code: String,
        selectedUserId: String? = null,
    ): NetworkMobileCodeLoginResult
}
