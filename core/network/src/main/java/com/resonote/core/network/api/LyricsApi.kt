package com.resonote.core.network.api

import com.resonote.core.network.protocol.ApiRequestPolicy
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

internal interface LyricsApi {
    @ApiRequestPolicy("API-SEARCH-005", includeDefaultParams = false)
    @GET
    suspend fun searchLyric(
        @Url url: String, @Query("album_audio_id") albumAudioId: String, @Query("appid") appId: Int = 3116,
        @Query("clientver") clientVersion: Int = 11440, @Query("duration") duration: Int = 0,
        @Query("hash") hash: String, @Query("keyword") keyword: String = "", @Query("lrctxt") lyricText: Int = 1,
        @Query("man") man: String = "no",
    ): JsonElement

    @ApiRequestPolicy("API-LYRICS-001")
    @GET
    suspend fun downloadLyric(
        @Url url: String, @Query("ver") version: Int = 1, @Query("client") client: String = "android",
        @Query("id") id: String, @Query("accesskey") accessKey: String, @Query("fmt") format: String = "lrc",
        @Query("charset") charset: String = "utf8",
    ): JsonElement
}
