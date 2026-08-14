package com.resonote.core.network.api

import com.resonote.core.network.api.model.ApiResponse
import com.resonote.core.network.protocol.ApiRequestPolicy
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

internal interface VideoApi {
    @ApiRequestPolicy
    @Headers("x-router: trackermv.kugou.com")
    @GET("v2/interface/index")
    suspend fun videoUrl(
        @Query("backupdomain") backupDomain: Int = 1, @Query("cmd") command: Int = 123,
        @Query("ext") extension: String = "mp4", @Query("ismp3") isMp3: Int = 0,
        @Query("hash") hash: String, @Query("pid") pid: Int = 1, @Query("type") type: Int = 1,
        @Query("key") key: String,
    ): ApiResponse<JsonElement>
}
