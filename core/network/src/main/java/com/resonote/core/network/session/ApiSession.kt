package com.resonote.core.network.session

import com.resonote.core.network.protocol.ApiDeviceIdentityFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class ApiSession(
    val guid: String,
    val mid: String,
    val dev: String,
    val dfid: String? = null,
    val token: String? = null,
    val userId: String? = null,
    val cookies: Map<String, String> = emptyMap(),
) {
    val isAuthenticated: Boolean get() = !token.isNullOrBlank() && !userId.isNullOrBlank() && userId != "0"

    override fun toString(): String =
        "ApiSession(guid=<redacted>, mid=<redacted>, dev=<redacted>, dfidPresent=${!dfid.isNullOrBlank()}, " +
            "authenticated=$isAuthenticated, cookieNames=${cookies.keys.sorted()})"

    companion object {
        fun anonymous(factory: ApiDeviceIdentityFactory): ApiSession =
            factory.create().let { ApiSession(guid = it.guid, mid = it.mid, dev = it.dev) }
    }
}

interface ApiSessionStore {
    val session: Flow<ApiSession?>

    suspend fun read(): ApiSession?

    suspend fun write(session: ApiSession)

    suspend fun clearAuthentication()
}
