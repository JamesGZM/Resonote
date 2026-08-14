package com.resonote.core.network

import com.resonote.core.network.model.NetworkVipRewardResult

interface VipNetworkDataSource {
    suspend fun claimDailyVip(receiveDay: String): NetworkVipRewardResult
    suspend fun upgradeDailyVip(): NetworkVipRewardResult
}
