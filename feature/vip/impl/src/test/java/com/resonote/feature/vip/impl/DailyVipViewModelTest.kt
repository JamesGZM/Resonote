package com.resonote.feature.vip.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.VipRewardRepository
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.RiskChallengeHandle
import com.resonote.core.model.VipReward
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DailyVipViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-08-13T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun openingPageDoesNotClaimAndUsesUtcDate() = runTest(dispatcher) {
        val repository = FakeVipRewardRepository()
        val viewModel = DailyVipViewModel(repository, clock)

        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(DailyVipUiState.Ready("2026-08-13"))
        assertThat(repository.claimDays).isEmpty()
        assertThat(repository.upgradeCalls).isEqualTo(0)
    }

    @Test
    fun claimedRewardRequiresExplicitUpgradeAndCompletes() = runTest(dispatcher) {
        val repository = FakeVipRewardRepository(
            claimResults = listOf(available(alreadyDone = false, canUpgrade = true)),
            upgradeResults = listOf(available(alreadyDone = false, canUpgrade = false)),
        )
        val viewModel = DailyVipViewModel(repository, clock)
        var refreshEvents = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.rewardApplied.collect { refreshEvents++ }
        }

        viewModel.claim()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(DailyVipUiState.UpgradeChoice("2026-08-13", false))
        assertThat(repository.claimDays).containsExactly("2026-08-13")
        assertThat(repository.upgradeCalls).isEqualTo(0)
        assertThat(refreshEvents).isEqualTo(1)

        viewModel.upgrade()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(DailyVipUiState.UpgradeComplete("2026-08-13", false))
        assertThat(repository.upgradeCalls).isEqualTo(1)
        assertThat(refreshEvents).isEqualTo(2)
    }

    @Test
    fun alreadyClaimedIsNeutralAndDeclineDoesNotUpgrade() = runTest(dispatcher) {
        val repository = FakeVipRewardRepository(
            claimResults = listOf(available(alreadyDone = true, canUpgrade = true)),
        )
        val viewModel = DailyVipViewModel(repository, clock)

        viewModel.claim()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isEqualTo(DailyVipUiState.UpgradeChoice("2026-08-13", true))

        viewModel.declineUpgrade()

        assertThat(viewModel.uiState.value).isEqualTo(DailyVipUiState.ClaimComplete("2026-08-13", true))
        assertThat(repository.upgradeCalls).isEqualTo(0)
    }

    @Test
    fun claimFailureRetriesOnlyClaimOperation() = runTest(dispatcher) {
        val repository = FakeVipRewardRepository(
            claimResults = listOf(
                CollectionLoadResult.Failed(ContentFailure.Network),
                available(alreadyDone = false, canUpgrade = false),
            ),
        )
        val viewModel = DailyVipViewModel(repository, clock)

        viewModel.claim()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isEqualTo(
            DailyVipUiState.Failed("2026-08-13", DailyVipOperation.Claim, ContentFailure.Network),
        )

        viewModel.retry()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(DailyVipUiState.ClaimComplete("2026-08-13", false))
        assertThat(repository.claimDays).hasSize(2)
        assertThat(repository.upgradeCalls).isEqualTo(0)
    }

    @Test
    fun upgradeFailureRetriesUpgradeWithoutClaimingAgain() = runTest(dispatcher) {
        val repository = FakeVipRewardRepository(
            claimResults = listOf(available(alreadyDone = false, canUpgrade = true)),
            upgradeResults = listOf(
                CollectionLoadResult.Failed(ContentFailure.Network),
                available(alreadyDone = true, canUpgrade = false),
            ),
        )
        val viewModel = DailyVipViewModel(repository, clock)

        viewModel.claim()
        advanceUntilIdle()
        viewModel.upgrade()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            DailyVipUiState.Failed(
                "2026-08-13",
                DailyVipOperation.Upgrade,
                ContentFailure.Network,
                alreadyClaimed = false,
            ),
        )

        viewModel.retry()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(DailyVipUiState.UpgradeComplete("2026-08-13", true))
        assertThat(repository.claimDays).hasSize(1)
        assertThat(repository.upgradeCalls).isEqualTo(2)
    }

    @Test
    fun riskChallengeStopsFlowWithoutUpgrade() = runTest(dispatcher) {
        val repository = FakeVipRewardRepository(
            claimResults = listOf(
                CollectionLoadResult.Failed(ContentFailure.RiskBlocked),
            ),
        )
        val viewModel = DailyVipViewModel(repository, clock)

        viewModel.claim()
        advanceUntilIdle()
        viewModel.upgrade()

        assertThat(viewModel.uiState.value).isEqualTo(DailyVipUiState.RiskBlocked("2026-08-13"))
        assertThat(repository.upgradeCalls).isEqualTo(0)
    }

    @Test
    fun riskVerificationChallengeIsPreservedForStandalonePage() = runTest(dispatcher) {
        val challenge = RiskChallengeHandle("fixture-risk")
        val repository = FakeVipRewardRepository(
            claimResults = listOf(
                CollectionLoadResult.Failed(ContentFailure.RiskVerificationRequired(challenge)),
            ),
        )
        val viewModel = DailyVipViewModel(repository, clock)

        viewModel.claim()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            DailyVipUiState.RiskVerificationRequired("2026-08-13", challenge, DailyVipOperation.Claim),
        )
        assertThat(repository.upgradeCalls).isEqualTo(0)
    }

    @Test
    fun completedRiskVerificationRetriesOriginalClaimOnce() = runTest(dispatcher) {
        val challenge = RiskChallengeHandle("fixture-risk")
        val repository = FakeVipRewardRepository(
            claimResults = listOf(
                CollectionLoadResult.Failed(ContentFailure.RiskVerificationRequired(challenge)),
                available(alreadyDone = false, canUpgrade = false),
            ),
        )
        val viewModel = DailyVipViewModel(repository, clock)

        viewModel.claim()
        advanceUntilIdle()
        viewModel.resumeAfterRisk(challenge)
        advanceUntilIdle()

        assertThat(repository.claimDays).hasSize(2)
        assertThat(viewModel.uiState.value).isEqualTo(DailyVipUiState.ClaimComplete("2026-08-13", false))
    }

    private class FakeVipRewardRepository(
        claimResults: List<CollectionLoadResult<VipReward>> = emptyList(),
        upgradeResults: List<CollectionLoadResult<VipReward>> = emptyList(),
    ) : VipRewardRepository {
        private val claims = ArrayDeque(claimResults)
        private val upgrades = ArrayDeque(upgradeResults)
        val claimDays = mutableListOf<String>()
        var upgradeCalls = 0

        override suspend fun claimDaily(receiveDay: String): CollectionLoadResult<VipReward> {
            claimDays += receiveDay
            return claims.removeFirst()
        }

        override suspend fun upgradeDaily(): CollectionLoadResult<VipReward> {
            upgradeCalls++
            return upgrades.removeFirst()
        }
    }

    private companion object {
        fun available(alreadyDone: Boolean, canUpgrade: Boolean) =
            CollectionLoadResult.Available(VipReward(alreadyDone, canUpgrade))
    }
}
