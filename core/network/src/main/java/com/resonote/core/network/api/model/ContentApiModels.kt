package com.resonote.core.network.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class BannerRequest(
    val plat: Int,
    val channel: Int,
    val operator: Int,
    val networktype: Int,
    val userid: Long,
    @SerialName("vip_type") val vipType: Int,
    @SerialName("m_type") val mobileType: Int,
    val tags: List<String>,
    val apiver: Int,
    val ability: Int,
    val mode: String,
)

@Serializable
internal data class PlaylistTagsRequest(
    @SerialName("tag_type") val tagType: String,
    @SerialName("tag_id") val tagId: Int,
    val source: Int,
)

@Serializable
internal data class TopAlbumsRequest(
    val apiver: Int,
    val token: String,
    val page: Int,
    val pagesize: Int,
    val withpriv: Int,
)

@Serializable
internal data class AlbumSongsRequest(
    @SerialName("album_id") val albumId: String,
    @SerialName("is_buy") val isBuy: String,
    val page: Int,
    val pagesize: Int,
)

@Serializable
internal data class ArtistDetailRequest(
    @SerialName("author_id") val artistId: String,
)

@Serializable
internal data class ArtistAudiosRequest(
    val appid: Int,
    val clientver: Int,
    val mid: String,
    val clienttime: Long,
    val key: String,
    @SerialName("author_id") val artistId: String,
    val pagesize: Int,
    val page: Int,
    val sort: Int,
    @SerialName("area_code") val areaCode: String,
)
