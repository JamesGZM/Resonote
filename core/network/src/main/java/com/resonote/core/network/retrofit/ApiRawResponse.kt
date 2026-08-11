package com.resonote.core.network.retrofit

import kotlinx.serialization.json.JsonObject

internal data class ApiRawResponse(
    val statusCode: Int,
    val headers: Map<String, List<String>>,
    val bytes: ByteArray,
    val body: JsonObject?,
)
