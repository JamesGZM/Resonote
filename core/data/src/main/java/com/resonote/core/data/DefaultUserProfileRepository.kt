package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.UserProfile
import com.resonote.core.network.ApiAuthenticationRequiredException
import com.resonote.core.network.ApiException
import com.resonote.core.network.UserProfileNetworkDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import java.time.Instant
import java.time.Year
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultUserProfileRepository @Inject constructor(
    private val network: UserProfileNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
) : UserProfileRepository {
    override suspend fun loadProfile(): CollectionLoadResult<UserProfile> = try {
        supervisorScope {
            val detailRequest = async { network.userDetail() }
            val vipRequest = async { network.userVip() }
            val detailResult =
                try {
                    Result.success(detailRequest.await())
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: ApiException) {
                    Result.failure(failure)
                }
            val vipResult =
                try {
                    Result.success(vipRequest.await())
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: ApiException) {
                    Result.failure(failure)
                }
            val detail = detailResult.getOrElse { detailFailure ->
                throw (vipResult.exceptionOrNull() as? ApiAuthenticationRequiredException ?: detailFailure)
            }
            val vip = vipResult.getOrElse { vipFailure ->
                if (vipFailure is ApiAuthenticationRequiredException) throw vipFailure
                null
            }
            CollectionLoadResult.Available(
                UserProfile(
                    userId = detail.userId,
                    nickname = detail.nickname,
                    avatarUrl = detail.avatarUrl?.replace("{size}", "240"),
                    backgroundUrl = detail.backgroundUrl?.replace("{size}", "720"),
                    signature = detail.signature,
                    fans = detail.fans,
                    follows = detail.follows,
                    listenMinutes = detail.listenMinutes,
                    isVip = vip?.isVip == true,
                    vipLabel = vip?.label.orEmpty(),
                    musicAgeYears = detail.registrationEpochSeconds?.musicAgeYears(),
                ),
            )
        }
    } catch (failure: ApiException) {
        CollectionLoadResult.Failed(failure.toContentFailure(riskChallenges))
    }
}

internal fun Long.musicAgeYears(currentYear: Int = Year.now().value, zoneId: ZoneId = ZoneId.systemDefault()): Int? {
    if (this <= 0) return null
    val registrationYear = runCatching { Instant.ofEpochSecond(this).atZone(zoneId).year }.getOrNull() ?: return null
    return (currentYear - registrationYear).coerceAtLeast(0)
}
