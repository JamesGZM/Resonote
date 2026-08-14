package com.resonote.core.network.api

import com.resonote.core.network.api.model.ApiResponse
import com.resonote.core.network.protocol.ApiRequestPolicy
import kotlinx.serialization.json.JsonElement
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

internal interface RecognitionApi {
    @ApiRequestPolicy
    @Headers("Content-Type: application/octet-stream", "User-Agent: KuGou/11490 (Android)")
    @POST("fingerprint.service/v1/music_trackid_mulit")
    suspend fun recognizeAudio(
        @Query("fpid") fingerprintId: Long,
        @Query("area_code") areaCode: Int = 1,
        @Query("include_unpublish") includeUnpublished: Int = 1,
        @Query("useid") userId: Long,
        @Query("multi_result") multiResult: Int = 1,
        @Body body: RequestBody,
    ): ApiResponse<JsonElement>
}
