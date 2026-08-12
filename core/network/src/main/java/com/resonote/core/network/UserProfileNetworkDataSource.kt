package com.resonote.core.network

import com.resonote.core.network.model.NetworkUserDetail
import com.resonote.core.network.model.NetworkUserVip

interface UserProfileNetworkDataSource {
    suspend fun userDetail(): NetworkUserDetail
    suspend fun userVip(): NetworkUserVip
}
