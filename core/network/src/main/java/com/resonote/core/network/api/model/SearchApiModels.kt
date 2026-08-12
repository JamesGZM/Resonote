package com.resonote.core.network.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal typealias SearchSongsResponse = ApiResponse<SearchSongsData>

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
