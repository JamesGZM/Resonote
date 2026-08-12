package com.resonote.core.network.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal typealias RankingsResponse = ApiResponse<RankingsData>
internal typealias RankingSongsResponse = ApiResponse<RankingSongsData>
internal typealias PlaylistSongsResponse = ApiResponse<PlaylistSongsData>

@Serializable
internal data class RankingsData(
    @SerialName("info") val rankings: List<MusicRankingDto>? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val status: String? = null,
    @SerialName("error_code") @Serializable(with = FlexibleStringSerializer::class) override val errorCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val ssaCode: String? = null,
    @SerialName("ssa_code") @Serializable(with = FlexibleStringSerializer::class) override val legacySsaCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val sid: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val edt: String? = null,
) : ApiRiskFieldsContract

@Serializable
internal data class RankingSongsData(
    @SerialName("songlist") val songs: List<MusicSongDto>? = null,
    @Serializable(with = FlexibleLongSerializer::class) val total: Long? = null,
    @SerialName("total_count") @Serializable(with = FlexibleLongSerializer::class) val totalCount: Long? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val status: String? = null,
    @SerialName("error_code") @Serializable(with = FlexibleStringSerializer::class) override val errorCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val ssaCode: String? = null,
    @SerialName("ssa_code") @Serializable(with = FlexibleStringSerializer::class) override val legacySsaCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val sid: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val edt: String? = null,
) : ApiRiskFieldsContract

@Serializable
internal data class PlaylistSongsData(
    val songs: List<MusicSongDto>? = null,
    @Serializable(with = FlexibleLongSerializer::class) val count: Long? = null,
    @SerialName("list_info") val info: MusicPlaylistInfoDto? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val status: String? = null,
    @SerialName("error_code") @Serializable(with = FlexibleStringSerializer::class) override val errorCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val ssaCode: String? = null,
    @SerialName("ssa_code") @Serializable(with = FlexibleStringSerializer::class) override val legacySsaCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val sid: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val edt: String? = null,
) : ApiRiskFieldsContract

@Serializable
internal data class MusicRankingDto(
    @Serializable(with = FlexibleStringSerializer::class) val rankid: String? = null,
    @SerialName("rank_id") @Serializable(with = FlexibleStringSerializer::class) val rankId: String? = null,
    val rankname: String? = null,
    val imgurl: String? = null,
    @SerialName("img_9") val image9: String? = null,
    @SerialName("banner_9") val banner9: String? = null,
)

@Serializable
internal data class MusicPlaylistInfoDto(
    val name: String? = null,
    val intro: String? = null,
    val pic: String? = null,
    @Serializable(with = FlexibleLongSerializer::class) val count: Long? = null,
)

@Serializable
internal data class RankingSongsRequest(
    @SerialName("show_portrait_mv") val showPortraitMv: Int,
    @SerialName("show_type_total") val showTypeTotal: Int,
    @SerialName("filter_original_remarks") val filterOriginalRemarks: Int,
    @SerialName("area_code") val areaCode: Int,
    val pagesize: Int,
    @SerialName("rank_cid") val rankCid: Int,
    val type: Int,
    val page: Int,
    @SerialName("rank_id") val rankId: String,
)
