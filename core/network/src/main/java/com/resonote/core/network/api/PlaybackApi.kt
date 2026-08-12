package com.resonote.core.network.api

import com.resonote.core.network.api.model.ApiResponse
import com.resonote.core.network.api.model.SongSourceResponse
import com.resonote.core.network.protocol.ApiRequestPolicy
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

internal interface PlaybackApi {
    @ApiRequestPolicy("API-SONG-011")
    @Headers("x-router: trackercdn.kugou.com")
    @GET("v5/url")
    suspend fun songSource(
        @Query("album_id") albumId: String, @Query("area_code") areaCode: Int = 1, @Query("hash") hash: String,
        @Query("ssa_flag") ssaFlag: String = "is_fromtrack", @Query("version") version: String = "11430",
        @Query("page_id") pageId: String = "967177915", @Query("quality") quality: String = "128",
        @Query("album_audio_id") albumAudioId: String, @Query("behavior") behavior: String = "play",
        @Query("pid") pid: String = "411", @Query("cmd") command: String = "26",
        @Query("pidversion") pidVersion: String = "3001", @Query("IsFreePart") isFreePart: String = "1",
        @Query("ppage_id") parentPageId: String = "356753938,823673182,967485191",
        @Query("cdnBackup") cdnBackup: String = "1", @Query("module") module: String = "",
        @Query("clientver") clientVersion: String = "11430", @Query("key") key: String,
    ): SongSourceResponse

    @ApiRequestPolicy("API-CLOUD-003")
    @GET("bsstrackercdngz/v2/query_musicclound_url")
    suspend fun cloudSongUrl(
        @Query("hash") hash: String, @Query("ssa_flag") ssaFlag: String = "is_fromtrack",
        @Query("version") version: Int = 20102, @Query("ssl") ssl: Int = 0,
        @Query("album_audio_id") albumAudioId: String, @Query("pid") pid: Int = 20026,
        @Query("audio_id") audioId: Int = 0, @Query("kv_id") kvId: Int = 2, @Query("key") key: String,
        @Query("bucket") bucket: String = "musicclound", @Query("name") name: String,
        @Query("with_res_tag") withResourceTag: Int = 0,
    ): ApiResponse<JsonElement>
}
