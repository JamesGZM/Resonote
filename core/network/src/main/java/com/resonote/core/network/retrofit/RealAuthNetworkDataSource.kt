package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.AuthNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.model.NetworkPasswordLoginResult
import com.resonote.core.network.model.NetworkQrLoginStatus
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import com.resonote.core.network.protocol.MobileAuthProtocolClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Singleton
internal class RealAuthNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val mobileAuth: MobileAuthProtocolClient,
    private val origins: ApiEndpointOrigins,
    private val calls: ApiCallExecutor,
    private val responses: ApiResponseVerifier,
) : AuthNetworkDataSource {
    override suspend fun sendMobileCode(mobile: String) = mobileAuth.sendMobileCode(mobile)

    override suspend fun loginWithMobileCode(
        mobile: String,
        code: String,
        selectedUserId: String?,
    ): NetworkMobileCodeLoginResult = mobileAuth.loginWithMobileCode(mobile, code, selectedUserId)

    override suspend fun loginWithPassword(username: String, password: String): NetworkPasswordLoginResult =
        mobileAuth.loginWithPassword(username, password)

    override suspend fun createQrLoginKey(): String {
        registration.ensureRegisteredSession()
        val response = calls.execute(detectHttpAuthenticationFailure = false) {
            musicApi.createQrLoginKey("${origins.qrLogin}/v2/qrcode")
        }
        responses.requireSuccess(response)
        return response.data.obj()?.text("qrcode")?.takeIf(String::isNotBlank) ?: throw missingField()
    }

    override suspend fun checkQrLogin(key: String): NetworkQrLoginStatus {
        require(key.isNotBlank()) { "key must not be blank" }
        val current = registration.ensureRegisteredSession()
        val response = calls.execute(detectHttpAuthenticationFailure = false) {
            musicApi.checkQrLogin("${origins.qrLogin}/v2/get_userinfo_qrcode", qrCode = key.trim())
        }
        responses.requireSuccess(response)
        val data = response.data.obj() ?: throw missingField()
        return when (data.int("status")) {
            0 -> NetworkQrLoginStatus.Expired
            1 -> NetworkQrLoginStatus.Waiting
            2 -> NetworkQrLoginStatus.Scanned(data.text("nickname").orEmpty())
            4 -> {
                val token = data.text("token")?.takeIf(String::isNotBlank) ?: throw missingField()
                val userId = data.text("userid")?.takeIf { it.isNotBlank() && it != "0" } ?: throw missingField()
                NetworkQrLoginStatus.Authenticated(
                    current.copy(
                        token = token,
                        userId = userId,
                        cookies = current.cookies + mapOf("token" to token, "userid" to userId),
                    ),
                )
            }
            else -> throw ApiServiceException(data.text("status"))
        }
    }

    private fun kotlinx.serialization.json.JsonElement?.obj(): JsonObject? = this as? JsonObject
    private fun JsonObject.text(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.int(name: String): Int? = text(name)?.toDoubleOrNull()?.toInt()
    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)
}
