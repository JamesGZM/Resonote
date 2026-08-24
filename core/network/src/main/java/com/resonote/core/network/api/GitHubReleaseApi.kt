package com.resonote.core.network.api

import com.resonote.core.network.api.model.GitHubReleaseResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Url

internal interface GitHubReleaseApi {
    @Headers(
        "Accept: application/vnd.github+json",
        "X-GitHub-Api-Version: 2022-11-28",
        "User-Agent: Resonote-Update-Check",
    )
    @GET
    suspend fun latestRelease(@Url endpoint: String): Response<GitHubReleaseResponse>
}
