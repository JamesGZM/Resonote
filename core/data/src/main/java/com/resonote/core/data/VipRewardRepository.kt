package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.VipReward

interface VipRewardRepository {
    suspend fun claimDaily(receiveDay: String): CollectionLoadResult<VipReward>
    suspend fun upgradeDaily(): CollectionLoadResult<VipReward>
}
