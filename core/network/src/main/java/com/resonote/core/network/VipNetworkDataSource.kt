package com.resonote.core.network

import com.resonote.core.network.model.NetworkVipRewardResult

interface VipNetworkDataSource {
    suspend fun serverTimeSeconds(): Long
    suspend fun vipCheckInRecords(): List<NetworkVipCheckInRecord>
    suspend fun claimDailyVip(receiveDay: String): NetworkVipRewardResult
    suspend fun upgradeDailyVip(): NetworkVipRewardResult
}

data class NetworkVipCheckInRecord(val date: String, val upgraded: Boolean)
