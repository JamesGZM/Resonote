package com.resonote.core.network.protocol

import com.resonote.core.network.session.ApiSession
import okhttp3.Request

internal object ApiSessionRequestDecorator {
    fun defaultQuery(
        session: ApiSession,
        clientTimeSeconds: Long,
        propagation: ApiSessionPropagation,
    ): Map<String, String> = linkedMapOf<String, String>().apply {
        put("dfid", session.dfid.orEmpty().ifBlank { "-" })
        put("mid", session.mid)
        put("uuid", ApiProtocolConfig.UUID)
        put("appid", ApiProtocolConfig.APP_ID)
        put("clientver", ApiProtocolConfig.CLIENT_VERSION)
        put("clienttime", clientTimeSeconds.toString())
        if (propagation == ApiSessionPropagation.Full) {
            session.token?.takeIf(String::isNotBlank)?.let { put("token", it) }
            session.userId?.takeIf { it.isNotBlank() && it != "0" }?.let { put("userid", it) }
        }
    }

    fun applySessionHeaders(
        builder: Request.Builder,
        session: ApiSession,
        clientTime: String,
        propagation: ApiSessionPropagation,
    ) {
        if (propagation == ApiSessionPropagation.None) return
        builder.header("dfid", session.dfid.orEmpty().ifBlank { "-" })
        builder.header("mid", session.mid)
        builder.header("clienttime", clientTime)
        builder.header("kg-rc", "1")
        builder.header("kg-thash", "5d816a0")
        builder.header("kg-rec", "1")
        builder.header("kg-rf", "B9EDA08A64250DEFFBCADDEE00F8F25F")
        cookieHeader(propagation, session)?.let { builder.header("Cookie", it) }
    }

    private fun cookieHeader(propagation: ApiSessionPropagation, session: ApiSession): String? {
        val cookies = when (propagation) {
            ApiSessionPropagation.None -> emptyMap()
            ApiSessionPropagation.DeviceOnly -> mapOf("mid" to session.mid)
            ApiSessionPropagation.Full -> session.cookies
        }
        return cookies.entries.joinToString("; ") { (name, value) -> "$name=$value" }.takeIf(String::isNotEmpty)
    }
}
