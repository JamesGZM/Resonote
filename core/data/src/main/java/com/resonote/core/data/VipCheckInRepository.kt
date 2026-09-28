package com.resonote.core.data

import com.resonote.core.database.vip.VipCheckInDao
import com.resonote.core.database.vip.VipCheckInEntity
import com.resonote.core.datastore.VipPreferencesStorage
import com.resonote.core.model.AuthState
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.VipCheckInRecord
import com.resonote.core.model.VipCheckInResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VipCheckInRepository @Inject constructor(
    private val rewards: VipRewardRepository,
    private val auth: AuthRepository,
    private val recordsDao: VipCheckInDao,
    private val preferences: VipPreferencesStorage,
    private val profile: UserProfileRepository,
    private val clock: Clock,
) {
    private val mutex = Mutex()
    private val coldStartChecked = AtomicBoolean(false)
    val automaticEnabled = preferences.automaticEnabled
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes = mutableChanges.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val records: Flow<List<VipCheckInRecord>> = auth.authState.flatMapLatest { state ->
        if (state is AuthState.Authenticated) {
            recordsDao.observe(state.userId).map { rows ->
                rows.map { VipCheckInRecord(LocalDate.parse(it.date), it.signed, it.upgraded) }
            }
        } else {
            flowOf(emptyList())
        }
    }

    suspend fun setAutomaticEnabled(enabled: Boolean) = preferences.setAutomaticEnabled(enabled)

    suspend fun onColdStart() {
        if (!coldStartChecked.compareAndSet(false, true)) return
        val initial = auth.authState.first()
        if (initial !is AuthState.Authenticated || !automaticEnabled.first()) return
        try {
            signIn(automatic = true)
        } catch (
            cancelled: CancellationException,
        ) {
            throw cancelled
        } catch (_: Exception) { /* Automatic check-in is deliberately silent. */ }
    }

    suspend fun refresh(): ContentFailure? {
        val user = (auth.authState.first() as? AuthState.Authenticated)?.userId
            ?: return ContentFailure.AuthenticationRequired
        return when (val result = rewards.records()) {
            is CollectionLoadResult.Failed -> result.failure
            is CollectionLoadResult.Available -> {
                if ((auth.authState.first() as? AuthState.Authenticated)?.userId != user) return null
                result.value.filter { it.upgraded }.forEach {
                    recordsDao.merge(VipCheckInEntity(user, it.date.toString(), signed = true, upgraded = true))
                }
                null
            }
        }
    }

    // A concurrent caller observes the running operation instead of queuing a second write.
    suspend fun signIn(automatic: Boolean): VipCheckInResult? {
        if (!mutex.tryLock()) return null
        mutableBusy.value = true
        try {
            val user = (auth.authState.first() as? AuthState.Authenticated)?.userId ?: return null
            return coroutineScope {
                val operation = currentCoroutineContext()[kotlinx.coroutines.Job]!!
                val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
                    auth.authState.first { it !is AuthState.Authenticated || it.userId != user }
                    operation.cancel()
                }
                try {
                    val time = try {
                        rewards.serverTimeSeconds()
                    } catch (
                        cancelled: CancellationException,
                    ) {
                        throw cancelled
                    } catch (_: Exception) {
                        CollectionLoadResult.Failed(ContentFailure.Network)
                    }
                    val instant = (time as? CollectionLoadResult.Available)?.value?.let(Instant::ofEpochSecond)
                        ?: clock.instant()
                    val day = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                    val claim = rewards.claimDaily(day.toString())
                    if (claim is CollectionLoadResult.Failed) {
                        return@coroutineScope VipCheckInResult(
                            day,
                            false,
                            false,
                            claim.failure,
                        )
                    }
                    currentCoroutineContext().ensureActive()
                    if ((auth.authState.first() as? AuthState.Authenticated)?.userId !=
                        user
                    ) {
                        throw CancellationException()
                    }
                    val upgrade = try {
                        rewards.upgradeDaily()
                    } catch (
                        cancelled: CancellationException,
                    ) {
                        throw cancelled
                    } catch (_: Exception) {
                        CollectionLoadResult.Failed(ContentFailure.Protocol)
                    }
                    val completed = upgrade is CollectionLoadResult.Available
                    currentCoroutineContext().ensureActive()
                    if ((auth.authState.first() as? AuthState.Authenticated)?.userId !=
                        user
                    ) {
                        throw CancellationException()
                    }
                    recordsDao.merge(VipCheckInEntity(user, day.toString(), automatic || completed, completed))
                    mutableChanges.tryEmit(Unit)
                    // Follow-up reads cannot undo a confirmed write or hide a partial success.
                    try {
                        profile.loadProfile()
                        refresh()
                    } catch (
                        cancelled: CancellationException,
                    ) {
                        throw cancelled
                    } catch (_: Exception) { /* The sign-in result remains authoritative. */ }
                    VipCheckInResult(day, true, completed, (upgrade as? CollectionLoadResult.Failed)?.failure)
                } finally {
                    watcher.cancel()
                }
            }
        } finally {
            mutableBusy.value = false
            mutex.unlock()
        }
    }
}
