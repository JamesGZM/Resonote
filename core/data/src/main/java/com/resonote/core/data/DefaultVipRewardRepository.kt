package com.resonote.core.data

import com.resonote.core.model.VipReward
import com.resonote.core.network.VipNetworkDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultVipRewardRepository @Inject constructor(
    private val network: VipNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
) : VipRewardRepository {
    override suspend fun claimDaily(receiveDay: String) = loadCollection(riskChallenges) {
        require(RECEIVE_DAY_PATTERN.matches(receiveDay)) { "receiveDay must use yyyy-MM-dd" }
        network.claimDailyVip(receiveDay).let {
            VipReward(
                alreadyDone = it.alreadyDone,
                canUpgrade = it.canUpgrade,
            )
        }
    }

    override suspend fun upgradeDaily() = loadCollection(riskChallenges) {
        network.upgradeDailyVip().let {
            VipReward(
                alreadyDone = it.alreadyDone,
                canUpgrade = it.canUpgrade,
            )
        }
    }

    private companion object {
        val RECEIVE_DAY_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    }
}
