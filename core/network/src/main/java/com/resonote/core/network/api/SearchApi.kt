package com.resonote.core.network.api

import com.resonote.core.network.api.model.ApiResponse
import com.resonote.core.network.api.model.SearchSongsData
import com.resonote.core.network.protocol.ApiRequestPolicy
import kotlinx.serialization.json.JsonElement
import retrofit2.http.*

internal interface SearchApi {
    @ApiRequestPolicy("API-SEARCH-001")
    @Headers("x-router: complexsearch.kugou.com")
    @GET("v3/search/song")
    suspend fun searchSongs(
        @Query("albumhide") albumHide: Int = 0, @Query("iscorrection") correction: Int = 1,
        @Query("keyword") keywords: String, @Query("nocollect") noCollect: Int = 0,
        @Query("page") page: Int, @Query("pagesize") pageSize: Int,
        @Query("platform") platform: String = "AndroidFilter",
    ): ApiResponse<SearchSongsData>

    @ApiRequestPolicy("API-SEARCH-001")
    @Headers("x-router: complexsearch.kugou.com")
    @GET
    suspend fun searchTyped(
        @Url url: String, @Query("albumhide") albumHide: Int = 0, @Query("iscorrection") correction: Int = 1,
        @Query("keyword") keywords: String, @Query("nocollect") noCollect: Int = 0,
        @Query("page") page: Int, @Query("pagesize") pageSize: Int,
        @Query("platform") platform: String = "AndroidFilter",
    ): ApiResponse<JsonElement>

    @ApiRequestPolicy("API-SEARCH-002")
    @GET
    suspend fun searchComplex(
        @Url url: String, @Query("platform") platform: String = "AndroidFilter", @Query("keyword") keyword: String,
        @Query("page") page: Int = 1, @Query("pagesize") pageSize: Int = 30, @Query("cursor") cursor: Int = 0,
    ): ApiResponse<JsonElement>

    @ApiRequestPolicy("API-SEARCH-004")
    @Headers("x-router: msearch.kugou.com")
    @GET("api/v3/search/hot_tab")
    suspend fun hotSearch(@Query("navid") navigationId: Int = 1, @Query("plat") platform: Int = 2): ApiResponse<JsonElement>

    @ApiRequestPolicy("API-SEARCH-007")
    @Headers("x-router: searchtip.kugou.com")
    @GET("v2/getSearchTip")
    suspend fun searchSuggestions(
        @Query("keyword") keyword: String, @Query("AlbumTipCount") albumCount: Int = 10,
        @Query("CorrectTipCount") correctCount: Int = 10, @Query("MVTipCount") mvCount: Int = 10,
        @Query("MusicTipCount") musicCount: Int = 10, @Query("radiotip") radioTip: Int = 1,
    ): ApiResponse<JsonElement>
}
