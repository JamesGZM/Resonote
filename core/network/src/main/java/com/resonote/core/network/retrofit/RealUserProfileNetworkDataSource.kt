package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.UserProfileNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.api.model.UserDetailRequest
import com.resonote.core.network.model.NetworkUserDetail
import com.resonote.core.network.model.NetworkUserVip
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.ApiProtocolCrypto
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Singleton
internal class RealUserProfileNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val clock: Clock,
    private val crypto: ApiProtocolCrypto,
    private val origins: ApiEndpointOrigins,
    private val calls: ApiCallExecutor,
    private val responses: ApiResponseVerifier,
) : UserProfileNetworkDataSource {
    override suspend fun userDetail(): NetworkUserDetail {
        val session = requireAuthenticatedSession()
        val clientTime = clock.millis() / 1_000
        val envelope = buildJsonObject {
            put("token", requireNotNull(session.token))
            put("clienttime", clientTime)
        }.toString()
        val response = calls.execute {
            musicApi.userDetail(
                body = UserDetailRequest(
                    visitTime = clientTime,
                    usertype = 1,
                    p = crypto.rawLiteRsa(envelope).uppercase(),
                    userid = requireNotNull(session.userId).toLongOrNull() ?: throw missingField(),
                ),
            )
        }
        responses.requireSuccess(response)
        val data = response.data ?: throw missingField()
        val userId = data.userid?.takeIf { it.isNotBlank() && it != "0" } ?: session.userId ?: throw missingField()
        return NetworkUserDetail(
            userId = userId,
            nickname = data.nickname?.takeIf(String::isNotBlank) ?: userId,
            avatarUrl = data.pic?.takeIf(String::isNotBlank),
            backgroundUrl = data.backgroundPic?.takeIf(String::isNotBlank),
            signature = data.descri.orEmpty(),
            fans = data.fans?.coerceAtLeast(0) ?: 0,
            follows = data.follows?.coerceAtLeast(0) ?: 0,
            listenMinutes = data.duration?.coerceAtLeast(0) ?: 0,
        )
    }

    override suspend fun userVip(): NetworkUserVip {
        requireAuthenticatedSession()
        val response = calls.execute { musicApi.userVip("${origins.vip}/v1/get_union_vip") }
        responses.requireSuccess(response)
        val active = response.data?.businessVip.orEmpty().firstOrNull { it.isVip == 1L }
        return NetworkUserVip(
            isVip = active != null,
            label = if (active?.productType.equals("svip", ignoreCase = true)) "SVIP" else if (active != null) "VIP" else "",
        )
    }

    private suspend fun requireAuthenticatedSession() =
        registration.requireAuthenticatedSession()

    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)

}
