package com.resonote.core.network.api

import com.resonote.core.network.api.model.*
import com.resonote.core.network.protocol.ApiRequestPolicy
import kotlinx.serialization.json.JsonElement
import retrofit2.http.*

internal interface ContentApi {
    @ApiRequestPolicy
    @Headers("x-router: everydayrec.service.kugou.com")
    @POST("everyday_song_recommend")
    suspend fun dailyRecommendations(@Query("platform") platform: String = "ios"): ApiResponse<SongListData>

    @ApiRequestPolicy
    @Headers("x-router: specialrec.service.kugou.com")
    @POST("v2/special_recommend")
    suspend fun recommendedPlaylists(@Body body: RecommendedPlaylistsRequest): ApiResponse<PlaylistRecommendationsData>

    @ApiRequestPolicy
    @POST("musicadservice/container/v1/newsong_publish")
    suspend fun newSongs(@Body body: NewSongsRequest): ApiResponse<List<MusicSongDto>>

    @ApiRequestPolicy
    @POST("singlecardrec.service/v1/single_card_recommend")
    suspend fun radioRecommendations(
        @Query("card_id") cardId: Int, @Query("fakem") fakem: String,
        @Query("area_code") areaCode: Int = 1, @Query("platform") platform: String = "ios",
        @Body body: RadioRecommendationsRequest,
    ): ApiResponse<SongListData>

    @ApiRequestPolicy
    @GET("ocean/v6/rank/list")
    suspend fun rankings(@Query("plat") platform: Int = 2, @Query("withsong") withSong: Int = 1, @Query("parentid") parentId: Int = 0): ApiResponse<RankingsData>

    @ApiRequestPolicy
    @Headers("kg-tid: 369")
    @POST("openapi/kmr/v2/rank/audio")
    suspend fun rankingSongs(@Body body: RankingSongsRequest): ApiResponse<RankingSongsData>

    @ApiRequestPolicy
    @GET("pubsongs/v2/get_other_list_file_nofilt")
    suspend fun playlistSongs(
        @Query("area_code") areaCode: Int = 1, @Query("begin_idx") beginIndex: Int,
        @Query("plat") platform: Int = 1, @Query("type") type: Int = 1, @Query("mode") mode: Int = 1,
        @Query("personal_switch") personalSwitch: Int = 1,
        @Query("extend_fields") extendFields: String = "abtags,hot_cmt,popularization",
        @Query("pagesize") pageSize: Int, @Query("global_collection_id") globalCollectionId: String,
    ): ApiResponse<PlaylistSongsData>

    @ApiRequestPolicy
    @POST("ads.gateway/v3/listen_banner")
    suspend fun banners(@Body body: BannerRequest): ApiResponse<JsonElement>

    @ApiRequestPolicy
    @POST("pubsongs/v1/get_tags_by_type")
    suspend fun playlistTags(@Body body: PlaylistTagsRequest): ApiResponse<JsonElement>

    @ApiRequestPolicy
    @POST("musicadservice/v1/mobile_newalbum_sp")
    suspend fun newAlbums(@Body body: TopAlbumsRequest): ApiResponse<JsonElement>

    @ApiRequestPolicy
    @Headers("x-router: openapi.kugou.com", "kg-tid: 255")
    @POST("v1/album_audio/lite")
    suspend fun albumSongs(@Body body: AlbumSongsRequest): ApiResponse<JsonElement>

    @ApiRequestPolicy
    @Headers("x-router: openapi.kugou.com", "kg-tid: 36")
    @POST("kmr/v3/author")
    suspend fun artistDetail(@Body body: ArtistDetailRequest): ApiResponse<JsonElement>

    @ApiRequestPolicy
    @Headers("x-router: openapi.kugou.com", "kg-tid: 220")
    @POST
    suspend fun artistSongs(@Url url: String, @Body body: ArtistAudiosRequest): ApiResponse<JsonElement>
}
