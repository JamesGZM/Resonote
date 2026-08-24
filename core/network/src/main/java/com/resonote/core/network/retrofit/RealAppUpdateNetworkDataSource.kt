package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiException
import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.ApiNetworkException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.AppUpdateNetworkDataSource
import com.resonote.core.network.api.GitHubReleaseApi
import com.resonote.core.network.model.NetworkAppRelease
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RealAppUpdateNetworkDataSource internal constructor(
    private val api: GitHubReleaseApi,
    private val endpoint: String,
) : AppUpdateNetworkDataSource {
    @Inject
    constructor(api: GitHubReleaseApi) : this(api, LATEST_RELEASE_ENDPOINT)

    override suspend fun latestRelease(): NetworkAppRelease {
        val response = try {
            api.latestRelease(endpoint)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (api: ApiException) {
            throw api
        } catch (timeout: SocketTimeoutException) {
            throw ApiNetworkException(ApiNetworkException.Kind.Timeout, timeout)
        } catch (offline: UnknownHostException) {
            throw ApiNetworkException(ApiNetworkException.Kind.Offline, offline)
        } catch (malformed: SerializationException) {
            throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
        } catch (connection: IOException) {
            throw ApiNetworkException(ApiNetworkException.Kind.Connection, connection)
        }
        if (!response.isSuccessful) throw ApiHttpException(response.code())
        val release = response.body() ?: throw malformedResponse()
        val version = release.tagName?.trim()?.takeIf(String::isNotEmpty) ?: throw malformedResponse()
        val releaseUrl = release.htmlUrl?.trim()?.toHttpUrlOrNull()
            ?.takeIf { it.isHttps && (it.host == GITHUB_DOMAIN || it.host.endsWith(".$GITHUB_DOMAIN")) }
            ?.toString()
            ?: throw malformedResponse()
        return NetworkAppRelease(version, releaseUrl)
    }

    private fun malformedResponse() = ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)

    private companion object {
        const val LATEST_RELEASE_ENDPOINT = "https://api.github.com/repos/JamesGZM/Resonote/releases/latest"
        const val GITHUB_DOMAIN = "github.com"
    }
}
