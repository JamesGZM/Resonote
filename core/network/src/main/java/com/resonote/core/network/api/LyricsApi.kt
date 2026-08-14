package com.resonote.core.network.api

import com.resonote.core.network.protocol.ApiRequestPolicy
import com.resonote.core.network.protocol.ApiSessionPropagation
import com.resonote.core.network.protocol.ApiSignatureMode
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

internal interface LyricsApi {
    @ApiRequestPolicy(
        signatureMode = ApiSignatureMode.None,
        sessionPropagation = ApiSessionPropagation.None,
        includeDefaultParams = false,
    )
    @GET
    suspend fun searchLyric(
        @Url url: String,
        @Query("ver") version: Int = 1,
        @Query("man") man: String = "yes",
        @Query("client") client: String = "pc",
        @Query("hash") hash: String,
    ): JsonElement

    @ApiRequestPolicy(
        signatureMode = ApiSignatureMode.None,
        sessionPropagation = ApiSessionPropagation.None,
        includeDefaultParams = false,
    )
    @GET
    suspend fun downloadLyric(
        @Url url: String,
        @Query("ver") version: Int = 1,
        @Query("client") client: String = "android",
        @Query("id") id: String,
        @Query("accesskey") accessKey: String,
        @Query("fmt") format: String = "krc",
        @Query("charset") charset: String = "utf8",
    ): JsonElement
}
