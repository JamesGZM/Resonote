package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.UserProfile
import com.resonote.core.network.ApiException
import com.resonote.core.network.UserProfileNetworkDataSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

@Singleton
internal class DefaultUserProfileRepository @Inject constructor(
    private val network: UserProfileNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
) : UserProfileRepository {
    override suspend fun loadProfile(): CollectionLoadResult<UserProfile> =
        try {
            supervisorScope {
                val detailRequest = async { network.userDetail() }
                val vipRequest = async { network.userVip() }
                val detail = detailRequest.await()
                val vip =
                    try {
                        vipRequest.await()
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: ApiException) {
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
                    ),
                )
            }
        } catch (failure: ApiException) {
            CollectionLoadResult.Failed(failure.toContentFailure(riskChallenges))
        }

}
