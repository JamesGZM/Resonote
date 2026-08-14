package com.resonote.core.network.api

import com.resonote.core.network.api.model.ApiResponse
import com.resonote.core.network.api.model.PlaylistCreateData
import com.resonote.core.network.api.model.PlaylistCreateRequest
import com.resonote.core.network.api.model.PlaylistTracksAddRequest
import com.resonote.core.network.api.model.PlaylistTracksDeleteRequest
import com.resonote.core.network.api.model.UserDetailData
import com.resonote.core.network.api.model.UserDetailRequest
import com.resonote.core.network.api.model.UserPlaylistsData
import com.resonote.core.network.api.model.UserPlaylistsRequest
import com.resonote.core.network.api.model.UserVipData
import com.resonote.core.network.protocol.ApiRequestPolicy
import com.resonote.core.network.protocol.ApiSessionPropagation
import com.resonote.core.network.protocol.ApiSignatureMode
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

internal interface AccountApi {
    @ApiRequestPolicy(router = "usercenter.kugou.com")
    @POST("v3/get_my_info")
    suspend fun userDetail(@Query("plat") platform: Int = 1, @Body body: UserDetailRequest): ApiResponse<UserDetailData>

    @ApiRequestPolicy
    @GET
    suspend fun userVip(
        @Url url: String,
        @Query("busi_type") businessType: String = "concept",
    ): ApiResponse<UserVipData>

    @ApiRequestPolicy(router = "cloudlist.service.kugou.com")
    @POST("v7/get_all_list")
    suspend fun userPlaylists(
        @Query("plat") platform: Int = 1,
        @Query("userid") userId: Long,
        @Query("token") token: String,
        @Body body: UserPlaylistsRequest,
    ): ApiResponse<UserPlaylistsData>

    @ApiRequestPolicy
    @POST("cloudlist.service/v5/add_list")
    suspend fun createPlaylist(
        @Query("last_time") lastTime: Long,
        @Query("last_area") lastArea: String = "gztx",
        @Query("userid") userId: Long,
        @Query("token") token: String,
        @Body body: PlaylistCreateRequest,
    ): ApiResponse<PlaylistCreateData>

    @ApiRequestPolicy
    @POST("cloudlist.service/v6/add_song")
    suspend fun addPlaylistTracks(
        @Query("last_time") lastTime: Long,
        @Query("last_area") lastArea: String = "gztx",
        @Query("userid") userId: Long,
        @Query("token") token: String,
        @Body body: PlaylistTracksAddRequest,
    ): ApiResponse<JsonElement>

    @ApiRequestPolicy(router = "cloudlist.service.kugou.com")
    @POST("v4/delete_songs")
    suspend fun deletePlaylistTracks(@Body body: PlaylistTracksDeleteRequest): ApiResponse<JsonElement>

    @ApiRequestPolicy(signatureMode = ApiSignatureMode.Web, sessionPropagation = ApiSessionPropagation.DeviceOnly)
    @GET
    suspend fun createQrLoginKey(
        @Url url: String,
        @Query("appid") appId: Int = 1001,
        @Query("type") type: Int = 1,
        @Query("plat") platform: Int = 4,
        @Query("qrcode_txt") qrCodeText: String = "https://h5.kugou.com/apps/loginQRCode/html/index.html?appid=3116&",
        @Query("srcappid") sourceAppId: Int = 2919,
    ): ApiResponse<JsonElement>

    @ApiRequestPolicy(signatureMode = ApiSignatureMode.Web, sessionPropagation = ApiSessionPropagation.DeviceOnly)
    @GET
    suspend fun checkQrLogin(
        @Url url: String,
        @Query("plat") platform: Int = 4,
        @Query("appid") appId: Int = 3116,
        @Query("srcappid") sourceAppId: Int = 2919,
        @Query("qrcode") qrCode: String,
    ): ApiResponse<JsonElement>

    @ApiRequestPolicy
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("youth/v1/recharge/receive_vip_listen_song")
    suspend fun claimDailyVip(
        @Query("source_id") sourceId: Int = 90139,
        @Query("receive_day") receiveDay: String,
    ): ApiResponse<JsonElement>

    @ApiRequestPolicy
    @POST("youth/v1/listen_song/upgrade_vip_reward")
    suspend fun upgradeDailyVip(
        @Query("kugouid") userId: Long,
        @Query("ad_type") adType: Int = 1,
    ): ApiResponse<JsonElement>
}
