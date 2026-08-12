package com.resonote.core.network.api.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/** Common service and risk fields exposed to the request executor. */
internal interface MusicApiResponse {
    val status: String?
    val errorCode: String?
    val ssaCode: String?
    val legacySsaCode: String?
    val sid: String?
    val edt: String?
    val risk: ApiRiskFields?
}

@Serializable
internal data class ApiResponse<T>(
    @Serializable(with = FlexibleStringSerializer::class)
    override val status: String? = null,
    @SerialName("error_code")
    @Serializable(with = FlexibleStringSerializer::class)
    override val errorCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    override val ssaCode: String? = null,
    @SerialName("ssa_code")
    @Serializable(with = FlexibleStringSerializer::class)
    override val legacySsaCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    override val sid: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    override val edt: String? = null,
    val data: T? = null,
    @Serializable(with = FlexibleLongSerializer::class)
    val total: Long? = null,
    @SerialName("risk")
    val directRisk: ApiRiskFields? = null,
) : MusicApiResponse {
    override val risk: ApiRiskFields?
        get() = directRisk ?: (data as? ApiRiskFieldsContract)?.toRiskFields()
}

@Serializable
internal data class ApiRiskFields(
    @Serializable(with = FlexibleStringSerializer::class)
    val status: String? = null,
    @SerialName("error_code")
    @Serializable(with = FlexibleStringSerializer::class)
    val errorCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val ssaCode: String? = null,
    @SerialName("ssa_code")
    @Serializable(with = FlexibleStringSerializer::class)
    val legacySsaCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val sid: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val edt: String? = null,
)

internal typealias SongListResponse = ApiResponse<SongListData>
internal typealias NewSongsResponse = ApiResponse<List<MusicSongDto>>
internal typealias PlaylistRecommendationsResponse = ApiResponse<PlaylistRecommendationsData>
internal typealias RankingsResponse = ApiResponse<RankingsData>
internal typealias RankingSongsResponse = ApiResponse<RankingSongsData>
internal typealias PlaylistSongsResponse = ApiResponse<PlaylistSongsData>
internal typealias SearchSongsResponse = ApiResponse<SearchSongsData>

@Serializable
internal data class SongListData(
    @SerialName("song_list") val songs: List<MusicSongDto>? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    override val status: String? = null,
    @SerialName("error_code")
    @Serializable(with = FlexibleStringSerializer::class)
    override val errorCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    override val ssaCode: String? = null,
    @SerialName("ssa_code")
    @Serializable(with = FlexibleStringSerializer::class)
    override val legacySsaCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    override val sid: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    override val edt: String? = null,
) : ApiRiskFieldsContract

private interface ApiRiskFieldsContract {
    val status: String?
    val errorCode: String?
    val ssaCode: String?
    val legacySsaCode: String?
    val sid: String?
    val edt: String?
}

private fun ApiRiskFieldsContract.toRiskFields() = ApiRiskFields(status, errorCode, ssaCode, legacySsaCode, sid, edt)

@Serializable
internal data class PlaylistRecommendationsData(
    @SerialName("special_list") val playlists: List<MusicPlaylistDto>? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val status: String? = null,
    @SerialName("error_code") @Serializable(with = FlexibleStringSerializer::class) override val errorCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val ssaCode: String? = null,
    @SerialName("ssa_code") @Serializable(with = FlexibleStringSerializer::class) override val legacySsaCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val sid: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val edt: String? = null,
) : ApiRiskFieldsContract

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
internal data class SearchSongsData(
    @SerialName("lists") val songs: List<MusicSongDto>? = null,
    @Serializable(with = FlexibleLongSerializer::class) val total: Long? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val status: String? = null,
    @SerialName("error_code") @Serializable(with = FlexibleStringSerializer::class) override val errorCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val ssaCode: String? = null,
    @SerialName("ssa_code") @Serializable(with = FlexibleStringSerializer::class) override val legacySsaCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val sid: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val edt: String? = null,
) : ApiRiskFieldsContract

@Serializable
internal data class SongSourceResponse(
    @Serializable(with = FlexibleStringSerializer::class) override val status: String? = null,
    @SerialName("error_code") @Serializable(with = FlexibleStringSerializer::class) override val errorCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val ssaCode: String? = null,
    @SerialName("ssa_code") @Serializable(with = FlexibleStringSerializer::class) override val legacySsaCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val sid: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) override val edt: String? = null,
    @Serializable(with = StringListSerializer::class) val url: List<String> = emptyList(),
    @SerialName("backupUrl") @Serializable(with = StringListSerializer::class) val backupUrl: List<String> = emptyList(),
    @SerialName("backup_url") @Serializable(with = StringListSerializer::class) val legacyBackupUrl: List<String> = emptyList(),
    @SerialName("timeLength") @Serializable(with = FlexibleLongSerializer::class) val timeLength: Long? = null,
    @SerialName("extName") val extension: String? = null,
    override val risk: ApiRiskFields? = null,
) : MusicApiResponse

@Serializable
internal data class MusicSongDto(
    val hash: String? = null,
    @SerialName("FileHash") val fileHash: String? = null,
    val deprecated: DeprecatedSongDto? = null,
    @SerialName("trans_param") val transform: SongTransformDto? = null,
    @SerialName("ori_audio_name") val originalAudioName: String? = null,
    val songname: String? = null,
    @SerialName("OriSongName") val originalSongName: String? = null,
    @SerialName("SongName") val songName: String? = null,
    val filename: String? = null,
    @SerialName("FileName") val fileName: String? = null,
    val name: String? = null,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("SingerName") val singerName: String? = null,
    @SerialName("sizable_cover") val sizableCover: String? = null,
    @SerialName("album_sizable_cover") val albumSizableCover: String? = null,
    @SerialName("Image") val image: String? = null,
    val cover: String? = null,
    @SerialName("album_id") @Serializable(with = FlexibleStringSerializer::class) val albumId: String? = null,
    @SerialName("album_audio_id") @Serializable(with = FlexibleStringSerializer::class) val albumAudioId: String? = null,
    @SerialName("audio_id") @Serializable(with = FlexibleStringSerializer::class) val audioId: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val mixsongid: String? = null,
    @SerialName("time_length") @Serializable(with = FlexibleLongSerializer::class) val timeLength: Long? = null,
    @Serializable(with = FlexibleLongSerializer::class) val duration: Long? = null,
    @SerialName("Duration") @Serializable(with = FlexibleLongSerializer::class) val searchDuration: Long? = null,
    @Serializable(with = FlexibleLongSerializer::class) val timelength: Long? = null,
    @Serializable(with = FlexibleLongSerializer::class) val timelen: Long? = null,
    @SerialName("HQFileHash") val highQualityFileHash: String? = null,
    @SerialName("hash_320") val highQualityHash: String? = null,
    @SerialName("SQFileHash") val losslessFileHash: String? = null,
    val sqhash: String? = null,
    @SerialName("hash_flac") val losslessHash: String? = null,
    @Serializable(with = FlexibleLongSerializer::class) val privilege: Long? = null,
    @SerialName("relate_goods")
    @Serializable(with = RelatedGoodsSerializer::class)
    val relatedGoods: List<RelatedGoodDto> = emptyList(),
    val albuminfo: AlbumInfoDto? = null,
    @SerialName("album_name") val albumName: String? = null,
    val albumname: String? = null,
    val remark: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val fileid: String? = null,
)

@Serializable internal class RelatedGoodDto

@Serializable
internal data class DeprecatedSongDto(
    val hash: String? = null,
    @Serializable(with = FlexibleLongSerializer::class) val duration: Long? = null,
    @SerialName("pay_type") @Serializable(with = FlexibleLongSerializer::class) val payType: Long? = null,
)

@Serializable
internal data class SongTransformDto(
    @SerialName("union_cover") val unionCover: String? = null,
)

@Serializable
internal data class AlbumInfoDto(
    val name: String? = null,
)

@Serializable
internal data class MusicPlaylistDto(
    @SerialName("global_collection_id") @Serializable(with = FlexibleStringSerializer::class) val globalCollectionId: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val specialid: String? = null,
    val specialname: String? = null,
    @SerialName("flexible_cover") val flexibleCover: String? = null,
    val cover: String? = null,
    val imgurl: String? = null,
    @SerialName("play_count") @Serializable(with = FlexibleLongSerializer::class) val playCount: Long? = null,
)

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

internal object FlexibleStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        val primitive = element as? JsonPrimitive ?: return null
        return if (element === JsonNull || (!primitive.isString && primitive.booleanOrNull != null)) null else primitive.contentOrNull
    }

    override fun serialize(encoder: Encoder, value: String?) {
        (encoder as JsonEncoder).encodeJsonElement(value?.let(::JsonPrimitive) ?: JsonNull)
    }
}

internal object FlexibleLongSerializer : KSerializer<Long?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long? {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        return (element as? JsonPrimitive)?.let { it.longOrNull ?: it.contentOrNull?.toDoubleOrNull()?.toLong() }
    }

    override fun serialize(encoder: Encoder, value: Long?) {
        (encoder as JsonEncoder).encodeJsonElement(value?.let(::JsonPrimitive) ?: JsonNull)
    }
}

internal object StringListSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StringList", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): List<String> {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        val values = if (element is JsonArray) element else JsonArray(listOf(element))
        return values.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty) }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        (encoder as JsonEncoder).encodeJsonElement(JsonArray(value.map(::JsonPrimitive)))
    }
}

internal object RelatedGoodsSerializer : KSerializer<List<RelatedGoodDto>> {
    override val descriptor: SerialDescriptor = ListSerializer(RelatedGoodDto.serializer()).descriptor

    override fun deserialize(decoder: Decoder): List<RelatedGoodDto> {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        return if (element is JsonArray) List(element.size) { RelatedGoodDto() } else emptyList()
    }

    override fun serialize(encoder: Encoder, value: List<RelatedGoodDto>) {
        (encoder as JsonEncoder).encodeJsonElement(JsonArray(List(value.size) { JsonObject(emptyMap()) }))
    }
}
