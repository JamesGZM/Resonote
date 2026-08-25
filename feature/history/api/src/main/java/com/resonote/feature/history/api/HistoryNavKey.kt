package com.resonote.feature.history.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class HistoryNavKey(val initialTab: HistoryTab = HistoryTab.Online) : NavKey

@Serializable
enum class HistoryTab {
    Online,
    Device,
}
