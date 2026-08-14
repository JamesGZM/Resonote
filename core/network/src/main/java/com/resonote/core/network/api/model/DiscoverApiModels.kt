package com.resonote.core.network.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal typealias SongListResponse = ApiResponse<SongListData>
internal typealias NewSongsResponse = ApiResponse<List<MusicSongDto>>
internal typealias PlaylistRecommendationsResponse = ApiResponse<PlaylistRecommendationsData>

@Serializable
internal data class SongListData(
    @SerialName("song_list") val songs: List<MusicSongDto>? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val status: String? = null,
    @SerialName(
        "error_code",
    ) @Serializable(with = FlexibleStringSerializer::class) override val errorCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val ssaCode: String? = null,
    @SerialName(
        "ssa_code",
    ) @Serializable(with = FlexibleStringSerializer::class) override val legacySsaCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val sid: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val edt: String? = null,
) : ApiRiskFieldsContract

@Serializable
internal data class PlaylistRecommendationsData(
    @SerialName("special_list") val playlists: List<MusicPlaylistDto>? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val status: String? = null,
    @SerialName(
        "error_code",
    ) @Serializable(with = FlexibleStringSerializer::class) override val errorCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val ssaCode: String? = null,
    @SerialName(
        "ssa_code",
    ) @Serializable(with = FlexibleStringSerializer::class) override val legacySsaCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val sid: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val edt: String? = null,
) : ApiRiskFieldsContract

@Serializable
internal data class MusicPlaylistDto(
    @SerialName("global_collection_id") @Serializable(with = FlexibleStringSerializer::class) val globalCollectionId:
    String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val specialid: String? = null,
    val specialname: String? = null,
    @SerialName("flexible_cover") val flexibleCover: String? = null,
    val cover: String? = null,
    val imgurl: String? = null,
    @SerialName("play_count") @Serializable(with = FlexibleLongSerializer::class) val playCount: Long? = null,
)

@Serializable
internal data class RecommendedPlaylistsRequest(
    val appid: Int,
    val mid: String,
    val clientver: Int,
    val platform: String,
    val clienttime: String,
    val userid: Long,
    @SerialName("module_id") val moduleId: Int,
    val page: Int,
    val pagesize: Int,
    val key: String,
    @SerialName("special_recommend") val specialRecommend: SpecialRecommendRequest,
    @SerialName("req_multi") val requestMultiple: Int,
    @SerialName("retrun_min") val returnMinimum: Int,
    @SerialName("return_special_falg") val returnSpecialFlag: Int,
)

@Serializable
internal data class SpecialRecommendRequest(
    val withtag: Int,
    val withsong: Int,
    val sort: Int,
    val ugc: Int,
    @SerialName("is_selected") val isSelected: Int,
    val withrecommend: Int,
    @SerialName("area_code") val areaCode: Int,
    val categoryid: Int,
)

@Serializable
internal data class NewSongsRequest(
    @SerialName("rank_id") val rankId: Int,
    val userid: Long,
    val page: Int,
    val pagesize: Int,
    val tags: List<String>,
)

@Serializable
internal data class RadioRecommendationsRequest(
    val appid: Int,
    val clientver: Int,
    val platform: String,
    val clienttime: Long,
    val userid: Long,
    val key: String,
    val fakem: String,
    @SerialName("area_code") val areaCode: Int,
    val mid: String,
    val uuid: String,
    @SerialName("client_playlist") val clientPlaylist: List<String>,
    @SerialName("u_info") val userInfo: String,
)
