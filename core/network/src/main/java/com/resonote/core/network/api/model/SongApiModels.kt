package com.resonote.core.network.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SongPrivilegeRequest(
    val appid: Int,
    @SerialName("area_code") val areaCode: Int = 1,
    val behavior: String = "play",
    val clientver: Int,
    @SerialName("need_hash_offset") val needHashOffset: Int = 1,
    val relate: Int = 1,
    @SerialName("support_verify") val supportVerify: Int = 1,
    val resource: List<SongPrivilegeResource>,
    val qualities: List<String> = SONG_PRIVILEGE_QUALITIES,
)

@Serializable
internal data class SongPrivilegeResource(
    val type: String = "audio",
    @SerialName("page_id") val pageId: Int = 0,
    val hash: String,
    @SerialName("album_id") val albumId: Long = 0,
)

internal val SONG_PRIVILEGE_QUALITIES =
    listOf("128", "320", "flac", "high", "viper_atmos", "viper_tape", "viper_clear", "super", "multitrack")

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
