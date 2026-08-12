package com.resonote.core.network.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Common service and risk fields consumed inside the network boundary. */
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

internal interface ApiRiskFieldsContract {
    val status: String?
    val errorCode: String?
    val ssaCode: String?
    val legacySsaCode: String?
    val sid: String?
    val edt: String?
}

private fun ApiRiskFieldsContract.toRiskFields() = ApiRiskFields(status, errorCode, ssaCode, legacySsaCode, sid, edt)
