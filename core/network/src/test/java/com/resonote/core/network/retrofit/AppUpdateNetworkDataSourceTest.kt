package com.resonote.core.network.retrofit

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.api.GitHubReleaseApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppUpdateNetworkDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var dataSource: RealAppUpdateNetworkDataSource
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubReleaseApi::class.java)
        dataSource =
            RealAppUpdateNetworkDataSource(api, server.url("/repos/JamesGZM/Resonote/releases/latest").toString())
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun latestReleaseUsesGitHubContractAndMapsRelease() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"tag_name":"v0.2.0","html_url":"https://github.com/JamesGZM/Resonote/releases/tag/v0.2.0"}""",
            ),
        )

        val release = dataSource.latestRelease()

        assertThat(release.version).isEqualTo("v0.2.0")
        assertThat(release.releaseUrl).isEqualTo("https://github.com/JamesGZM/Resonote/releases/tag/v0.2.0")
        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/repos/JamesGZM/Resonote/releases/latest")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.github+json")
        assertThat(request.getHeader("X-GitHub-Api-Version")).isEqualTo("2022-11-28")
        assertThat(request.getHeader("User-Agent")).isEqualTo("Resonote-Update-Check")
    }

    @Test
    fun nonSuccessResponseRemainsTyped() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        val error = runCatching { dataSource.latestRelease() }.exceptionOrNull()

        assertThat(error).isInstanceOf(ApiHttpException::class.java)
        assertThat((error as ApiHttpException).statusCode).isEqualTo(404)
    }

    @Test
    fun releaseUrlMustRemainOnGitHubHttps() = runTest {
        server.enqueue(MockResponse().setBody("""{"tag_name":"v0.2.0","html_url":"http://example.com/app.apk"}"""))

        assertThat(runCatching { dataSource.latestRelease() }.exceptionOrNull())
            .isInstanceOf(ApiProtocolException::class.java)
    }
}
