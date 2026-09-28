package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.database.vip.VipCheckInDao
import com.resonote.core.database.vip.VipCheckInEntity
import com.resonote.core.datastore.VipPreferencesStorage
import com.resonote.core.model.AuthState
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.MobileCodeLoginResult
import com.resonote.core.model.PasswordLoginResult
import com.resonote.core.model.QrLoginCheckResult
import com.resonote.core.model.QrLoginKeyResult
import com.resonote.core.model.SendMobileCodeResult
import com.resonote.core.model.VipCheckInRecord
import com.resonote.core.model.VipReward
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class VipCheckInRepositoryTest {
    private val auth = FakeAuth()
    private val rewards = FakeRewards()
    private val dao = FakeDao()
    private val preferences = object : VipPreferencesStorage {
        override val automaticEnabled = MutableStateFlow(true)
        override suspend fun setAutomaticEnabled(enabled: Boolean) {
            automaticEnabled.value = enabled
        }
    }
    private val profile = object : UserProfileRepository {
        override suspend fun loadProfile() = CollectionLoadResult.Failed(ContentFailure.Network)
    }
    private fun repository() = VipCheckInRepository(
        rewards,
        auth,
        dao,
        preferences,
        profile,
        Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneId.systemDefault()),
    )

    @Test fun automaticUpgradeFailureStillMarksSignedButManualDoesNot() = runTest {
        rewards.upgradeResult = CollectionLoadResult.Failed(ContentFailure.ServiceRejected)
        val repo = repository()
        val manual = repo.signIn(false)!!
        assertThat(manual.claimed).isTrue()
        assertThat(manual.upgraded).isFalse()
        assertThat(dao.rows.value.single().signed).isFalse()
        val auto = repo.signIn(true)!!
        assertThat(auto.failure).isEqualTo(ContentFailure.ServiceRejected)
        assertThat(dao.rows.value.single().signed).isTrue()
        assertThat(dao.rows.value.single().upgraded).isFalse()
    }

    @Test fun failedClaimDoesNotUpgradeOrWriteRecord() = runTest {
        rewards.claimResult = CollectionLoadResult.Failed(ContentFailure.Network)
        repository().signIn(true)
        assertThat(rewards.upgrades).isEqualTo(0)
        assertThat(dao.rows.value).isEmpty()
    }

    @Test fun coldStartChecksOncePerProcessButNewProcessRetriesEvenIfSigned() = runTest {
        val repo = repository()
        repo.onColdStart()
        repo.onColdStart()
        assertThat(rewards.claims).isEqualTo(1)
        repository().onColdStart()
        assertThat(rewards.claims).isEqualTo(2)
        assertThat(dao.rows.value.single().upgraded).isTrue()
    }

    @Test fun disabledAndAnonymousColdStartNeverAutoSubmitAfterLaterLogin() = runTest {
        preferences.setAutomaticEnabled(false)
        repository().onColdStart()
        assertThat(rewards.claims).isEqualTo(0)
        preferences.setAutomaticEnabled(true)
        auth.authState.value = AuthState.Anonymous
        val repo = repository()
        repo.onColdStart()
        auth.authState.value = AuthState.Authenticated("A")
        repo.onColdStart()
        assertThat(rewards.claims).isEqualTo(0)
        repo.signIn(false)
        assertThat(rewards.claims).isEqualTo(1)
    }

    @Test fun concurrentCallsDoNotQueueDuplicateWrites() = runTest {
        rewards.barrier = CompletableDeferred()
        val repo = repository()
        val first = async { repo.signIn(true) }
        runCurrent()
        assertThat(repo.signIn(false)).isNull()
        rewards.barrier!!.complete(Unit)
        first.await()
        assertThat(rewards.claims).isEqualTo(1)
    }

    @Test fun changingAccountCancelsPendingWriteAndHidesOldRecords() = runTest {
        rewards.barrier = CompletableDeferred()
        val repo = repository()
        val first = async { repo.signIn(true) }
        runCurrent()
        auth.authState.value = AuthState.Authenticated("B")
        runCurrent()
        assertThat(first.isCancelled).isTrue()
        assertThat(dao.rows.value).isEmpty()
        assertThat(repo.records.first()).isEmpty()
    }

    @Test fun serverDateIsUsedAndFallbackDoesNotFreezeDateAtViewModelCreation() = runTest {
        rewards.time = CollectionLoadResult.Available(Instant.parse("2026-08-10T12:00:00Z").epochSecond)
        val first = repository().signIn(false)!!
        assertThat(
            first.date,
        ).isEqualTo(Instant.parse("2026-08-10T12:00:00Z").atZone(ZoneId.systemDefault()).toLocalDate())
        rewards.time = CollectionLoadResult.Failed(ContentFailure.Network)
        val fallback = repository().signIn(false)!!
        assertThat(
            fallback.date,
        ).isEqualTo(Instant.parse("2026-09-28T12:00:00Z").atZone(ZoneId.systemDefault()).toLocalDate())
    }

    private class FakeRewards : VipRewardRepository {
        var claims = 0
        var upgrades = 0
        var barrier: CompletableDeferred<Unit>? = null
        var time: CollectionLoadResult<Long> = CollectionLoadResult.Failed(ContentFailure.Network)
        var claimResult: CollectionLoadResult<VipReward> = CollectionLoadResult.Available(VipReward(false, true))
        var upgradeResult: CollectionLoadResult<VipReward> = CollectionLoadResult.Available(VipReward(false, false))
        override suspend fun serverTimeSeconds() = time
        override suspend fun records() = CollectionLoadResult.Available(emptyList<VipCheckInRecord>())
        override suspend fun claimDaily(receiveDay: String): CollectionLoadResult<VipReward> {
            claims++
            barrier?.await()
            return claimResult
        }
        override suspend fun upgradeDaily(): CollectionLoadResult<VipReward> {
            upgrades++
            return upgradeResult
        }
    }
    private class FakeDao : VipCheckInDao() {
        val rows = MutableStateFlow<List<VipCheckInEntity>>(emptyList())
        override fun observe(userId: String) = rows.map { all -> all.filter { it.userId == userId } }
        override suspend fun find(userId: String, date: String) = rows.value.find {
            it.userId == userId &&
                it.date == date
        }
        override suspend fun insert(record: VipCheckInEntity) {
            rows.value = rows.value.filterNot { it.userId == record.userId && it.date == record.date } + record
        }
    }
    private class FakeAuth : AuthRepository {
        override val authState = MutableStateFlow<AuthState>(AuthState.Authenticated("A"))
        override suspend fun acknowledgeAuthenticationGate() = Unit
        override suspend fun logout() {
            authState.value = AuthState.Anonymous
        }
        override suspend fun sendMobileCode(mobile: String): SendMobileCodeResult = error("unused")
        override suspend fun loginWithMobileCode(
            mobile: String,
            code: String,
            selectedUserId: String?,
        ): MobileCodeLoginResult = error("unused")
        override suspend fun loginWithPassword(username: String, password: String): PasswordLoginResult =
            error("unused")
        override suspend fun createQrLoginKey(): QrLoginKeyResult = error("unused")
        override suspend fun checkQrLogin(key: String): QrLoginCheckResult = error("unused")
    }
}
