package com.resonote.core.network.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

internal typealias UserDetailResponse = ApiResponse<UserDetailData>
internal typealias UserVipResponse = ApiResponse<UserVipData>
internal typealias UserPlaylistsResponse = ApiResponse<UserPlaylistsData>
internal typealias PlaylistCreateResponse = ApiResponse<PlaylistCreateData>

@Serializable
internal data class UserDetailRequest(
    @SerialName("visit_time") val visitTime: Long,
    val usertype: Int,
    val p: String,
    val userid: Long,
)

@Serializable
internal data class UserPlaylistsRequest(
    val userid: String,
    val token: String,
    @SerialName("total_ver") val totalVersion: Int,
    val type: Int,
    val page: Int,
    val pagesize: Int,
)

@Serializable
internal data class UserDetailData(
    @Serializable(with = FlexibleStringSerializer::class) val userid: String? = null,
    val nickname: String? = null,
    val pic: String? = null,
    @SerialName("bg_pic") val backgroundPic: String? = null,
    val descri: String? = null,
    @Serializable(with = FlexibleLongSerializer::class) val fans: Long? = null,
    @Serializable(with = FlexibleLongSerializer::class) val follows: Long? = null,
    @Serializable(with = FlexibleLongSerializer::class) val duration: Long? = null,
)

@Serializable
internal data class UserVipData(@SerialName("busi_vip") val businessVip: List<UserVipItemDto>? = null)

@Serializable
internal data class UserVipItemDto(
    @SerialName("is_vip") @Serializable(with = FlexibleLongSerializer::class) val isVip: Long? = null,
    @SerialName("product_type") @Serializable(with = FlexibleStringSerializer::class) val productType: String? = null,
)

@Serializable
internal data class UserPlaylistsData(val info: List<UserPlaylistDto>? = null)

@Serializable
internal data class UserPlaylistDto(
    @Serializable(with = FlexibleStringSerializer::class) val listid: String? = null,
    @SerialName("list_create_gid") @Serializable(with = FlexibleStringSerializer::class) val globalId: String? = null,
    val name: String? = null,
    val pic: String? = null,
    @Serializable(with = FlexibleLongSerializer::class) val count: Long? = null,
    @SerialName(
        "list_create_userid",
    ) @Serializable(with = FlexibleStringSerializer::class) val ownerUserId: String? = null,
    val authors: JsonElement? = null,
)

@Serializable
internal data class PlaylistCreateRequest(
    val userid: String,
    val token: String,
    @SerialName("total_ver") val totalVersion: Int,
    val name: String,
    val type: Int,
    val source: Int,
    @SerialName("is_pri") val isPrivate: Int,
    @SerialName("list_create_userid") val creatorUserId: String,
    @SerialName("list_create_gid") val creatorGlobalId: String,
    @SerialName("from_shupinmv") val fromShortVideo: Int,
)

@Serializable
internal data class PlaylistCreateData(val info: PlaylistCreatedInfo? = null)

@Serializable
internal data class PlaylistCreatedInfo(
    @Serializable(with = FlexibleStringSerializer::class) val listid: String? = null,
)

@Serializable
internal data class PlaylistTracksAddRequest(
    val userid: String,
    val token: String,
    val listid: String,
    @SerialName("list_ver") val listVersion: Int,
    val type: Int,
    @SerialName("slow_upload") val slowUpload: Int,
    val scene: String,
    val data: List<PlaylistTrackResource>,
)

@Serializable
internal data class PlaylistTrackResource(
    val number: Int,
    val name: String,
    val hash: String,
    val size: Int,
    val sort: Int,
    val timelen: Int,
    val bitrate: Int,
    @SerialName("album_id") val albumId: Long,
    val mixsongid: Long,
)

@Serializable
internal data class PlaylistTracksDeleteRequest(
    val listid: String,
    val userid: String,
    val data: List<PlaylistFileResource>,
    val type: Int,
    val token: String,
    @SerialName("list_ver") val listVersion: Int,
)

@Serializable
internal data class PlaylistFileResource(val fileid: Long)
