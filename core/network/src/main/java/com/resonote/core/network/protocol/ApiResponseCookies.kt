package com.resonote.core.network.protocol

import com.resonote.core.network.retrofit.ApiRawResponse

internal fun ApiRawResponse.responseCookies(): Map<String, String> =
    headers.entries
        .firstOrNull { it.key.equals("set-cookie", ignoreCase = true) }
        ?.value
        .orEmpty()
        .mapNotNull { raw ->
            val pair = raw.substringBefore(';')
            val separator = pair.indexOf('=')
            if (separator <= 0) {
                null
            } else {
                pair.substring(0, separator).trim() to pair.substring(separator + 1).trim()
            }
        }.toMap()
