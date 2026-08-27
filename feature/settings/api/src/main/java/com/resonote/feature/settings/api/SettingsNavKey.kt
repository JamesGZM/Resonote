package com.resonote.feature.settings.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SettingsNavKey : NavKey

@Serializable
data object PlaybackSettingsNavKey : NavKey

@Serializable
data object EqualizerSettingsNavKey : NavKey

@Serializable
data object LyricsSettingsNavKey : NavKey

@Serializable
data object DesktopLyricsSettingsNavKey : NavKey

@Serializable
data object PermissionsSettingsNavKey : NavKey

@Serializable
data object AboutSettingsNavKey : NavKey

@Serializable
data object PrivacySettingsNavKey : NavKey

@Serializable
data object LicenseSettingsNavKey : NavKey

@Serializable
data object OpenSourceLibrariesNavKey : NavKey
