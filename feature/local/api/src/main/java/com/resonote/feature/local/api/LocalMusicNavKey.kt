package com.resonote.feature.local.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class LocalMusicNavKey(
    val finishTaskOnBack: Boolean = false,
) : NavKey
