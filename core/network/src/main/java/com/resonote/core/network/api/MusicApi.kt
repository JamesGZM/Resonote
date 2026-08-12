package com.resonote.core.network.api

import com.resonote.core.network.api.model.ApiResponse
import com.resonote.core.network.api.model.MusicSongDto
import com.resonote.core.network.api.model.NewSongsRequest
import com.resonote.core.network.api.model.PlaylistRecommendationsData
import com.resonote.core.network.api.model.PlaylistSongsData
import com.resonote.core.network.api.model.RadioRecommendationsRequest
import com.resonote.core.network.api.model.RankingSongsRequest
import com.resonote.core.network.api.model.RankingSongsData
import com.resonote.core.network.api.model.RankingsData
import com.resonote.core.network.api.model.RecommendedPlaylistsRequest
import com.resonote.core.network.api.model.SearchSongsData
import com.resonote.core.network.api.model.SongListData
import com.resonote.core.network.api.model.SongSourceResponse
import com.resonote.core.network.protocol.ApiRequestPolicy
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/** Standard JSON endpoints consumed by the network data source. */
internal interface MusicApi {
    @ApiRequestPolicy("API-DISCOVER-003")
    @Headers("x-router: everydayrec.service.kugou.com")
    @POST("everyday_song_recommend")
    suspend fun dailyRecommendations(
        @Query("platform") platform: String = "ios",
    ): ApiResponse<SongListData>

    @ApiRequestPolicy("API-DISCOVER-012")
    @Headers("x-router: specialrec.service.kugou.com")
    @POST("v2/special_recommend")
    suspend fun recommendedPlaylists(
        @Body body: RecommendedPlaylistsRequest,
    ): ApiResponse<PlaylistRecommendationsData>

    @ApiRequestPolicy("API-DISCOVER-013")
    @POST("musicadservice/container/v1/newsong_publish")
    suspend fun newSongs(
        @Body body: NewSongsRequest,
    ): ApiResponse<List<MusicSongDto>>

    @ApiRequestPolicy("API-DISCOVER-009")
    @POST("singlecardrec.service/v1/single_card_recommend")
    suspend fun radioRecommendations(
        @Query("card_id") cardId: Int,
        @Query("fakem") fakem: String,
        @Query("area_code") areaCode: Int = 1,
        @Query("platform") platform: String = "ios",
        @Body body: RadioRecommendationsRequest,
    ): ApiResponse<SongListData>

    @ApiRequestPolicy("API-SONG-011")
    @Headers("x-router: trackercdn.kugou.com")
    @GET("v5/url")
    suspend fun songSource(
        @Query("album_id") albumId: String,
        @Query("area_code") areaCode: Int = 1,
        @Query("hash") hash: String,
        @Query("ssa_flag") ssaFlag: String = "is_fromtrack",
        @Query("version") version: String = "11430",
        @Query("page_id") pageId: String = "967177915",
        @Query("quality") quality: String = "128",
        @Query("album_audio_id") albumAudioId: String,
        @Query("behavior") behavior: String = "play",
        @Query("pid") pid: String = "411",
        @Query("cmd") command: String = "26",
        @Query("pidversion") pidVersion: String = "3001",
        @Query("IsFreePart") isFreePart: String = "1",
        @Query("ppage_id") parentPageId: String = "356753938,823673182,967485191",
        @Query("cdnBackup") cdnBackup: String = "1",
        @Query("module") module: String = "",
        @Query("clientver") clientVersion: String = "11430",
        @Query("key") key: String,
    ): SongSourceResponse

    @ApiRequestPolicy("API-RANKING-003")
    @GET("ocean/v6/rank/list")
    suspend fun rankings(
        @Query("plat") platform: Int = 2,
        @Query("withsong") withSong: Int = 1,
        @Query("parentid") parentId: Int = 0,
    ): ApiResponse<RankingsData>

    @ApiRequestPolicy("API-RANKING-001")
    @Headers("kg-tid: 369")
    @POST("openapi/kmr/v2/rank/audio")
    suspend fun rankingSongs(
        @Body body: RankingSongsRequest,
    ): ApiResponse<RankingSongsData>

    @ApiRequestPolicy("API-PLAYLIST-007")
    @GET("pubsongs/v2/get_other_list_file_nofilt")
    suspend fun playlistSongs(
        @Query("area_code") areaCode: Int = 1,
        @Query("begin_idx") beginIndex: Int,
        @Query("plat") platform: Int = 1,
        @Query("type") type: Int = 1,
        @Query("mode") mode: Int = 1,
        @Query("personal_switch") personalSwitch: Int = 1,
        @Query("extend_fields") extendFields: String = "abtags,hot_cmt,popularization",
        @Query("pagesize") pageSize: Int,
        @Query("global_collection_id") globalCollectionId: String,
    ): ApiResponse<PlaylistSongsData>

    @ApiRequestPolicy("API-SEARCH-001")
    @Headers("x-router: complexsearch.kugou.com")
    @GET("v3/search/song")
    suspend fun searchSongs(
        @Query("albumhide") albumHide: Int = 0,
        @Query("iscorrection") correction: Int = 1,
        @Query("keyword") keywords: String,
        @Query("nocollect") noCollect: Int = 0,
        @Query("page") page: Int,
        @Query("pagesize") pageSize: Int,
        @Query("platform") platform: String = "AndroidFilter",
    ): ApiResponse<SearchSongsData>
}
