package com.resonote.core.network.protocol

import com.resonote.core.network.BuildConfig

internal object ApiSessionOriginPolicy {
    fun isAllowed(host: String): Boolean = host == KUGOU_ROOT ||
        host.endsWith(".$KUGOU_ROOT") ||
        (BuildConfig.DEBUG && host in DEBUG_LOOPBACK_HOSTS)

    private const val KUGOU_ROOT = "kugou.com"
    private val DEBUG_LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1", "::1")
}
