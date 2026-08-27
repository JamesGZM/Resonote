package com.resonote.core.data

import com.resonote.core.datastore.LyricsPreferencesStorage
import com.resonote.core.model.DesktopLyricsControlsTimeout
import com.resonote.core.model.DesktopLyricsDisplayMode
import com.resonote.core.model.DesktopLyricsPosition
import com.resonote.core.model.LyricsBackgroundMode
import com.resonote.core.model.LyricsDisplayMode
import com.resonote.core.model.LyricsFontSize
import com.resonote.core.model.LyricsHighlightMode
import com.resonote.core.model.LyricsPreferences
import com.resonote.core.model.LyricsTextAlignment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import com.resonote.core.datastore.proto.LyricsPreferences as StoredLyricsPreferences

interface LyricsPreferencesRepository {
    val preferences: Flow<LyricsPreferences>
    suspend fun setPreferences(value: LyricsPreferences)
    suspend fun reset()
}

@Singleton
internal class DefaultLyricsPreferencesRepository @Inject constructor(private val storage: LyricsPreferencesStorage) :
    LyricsPreferencesRepository {
    override val preferences = storage.values.map { stored ->
        LyricsPreferences(
            translationEnabled = if (stored.supplementalTextFlagsSet) stored.translationEnabled else true,
            transliterationEnabled = if (stored.supplementalTextFlagsSet) stored.transliterationEnabled else true,
            displayMode = stored.enumOrDefault(LyricsDisplayMode.Scrolling) { displayMode },
            highlightMode = stored.enumOrDefault(LyricsHighlightMode.Word) { highlightMode },
            textAlignment = stored.enumOrDefault(LyricsTextAlignment.Center) { textAlignment },
            fontSize = stored.enumOrDefault(LyricsFontSize.Medium) { fontSize },
            backgroundMode = stored.enumOrDefault(LyricsBackgroundMode.Artwork) { backgroundMode },
            desktopLyricsEnabled = stored.desktopLyricsEnabled,
            desktopLyricsDisplayMode = stored.enumOrDefault(DesktopLyricsDisplayMode.TwoLines) {
                desktopLyricsDisplayMode
            },
            desktopLyricsFontSize = stored.enumOrDefault(LyricsFontSize.Medium) { desktopLyricsFontSize },
            desktopLyricsSurfaceOpacity = if (stored.desktopLyricsSurfaceOpacitySet) {
                stored.desktopLyricsSurfaceOpacity.coerceIn(0, 100)
            } else {
                0
            },
            desktopLyricsAutoHideWhenPaused = false,
            desktopLyricsControlsTimeout = stored.enumOrDefault(DesktopLyricsControlsTimeout.FiveSeconds) {
                desktopLyricsControlsTimeout
            },
            desktopLyricsLocked = stored.desktopLyricsLocked,
            desktopLyricsPosition = if (stored.desktopLyricsPositionSet) {
                DesktopLyricsPosition(stored.desktopLyricsPositionX, stored.desktopLyricsPositionY)
            } else {
                null
            },
        )
    }

    override suspend fun setPreferences(value: LyricsPreferences) = storage.update(
        StoredLyricsPreferences(
            displayMode = value.displayMode.name,
            highlightMode = value.highlightMode.name,
            textAlignment = value.textAlignment.name,
            fontSize = value.fontSize.name,
            backgroundMode = value.backgroundMode.name,
            translationEnabled = value.translationEnabled,
            transliterationEnabled = value.transliterationEnabled,
            supplementalTextFlagsSet = true,
            desktopLyricsEnabled = value.desktopLyricsEnabled,
            desktopLyricsDisplayMode = value.desktopLyricsDisplayMode.name,
            desktopLyricsFontSize = value.desktopLyricsFontSize.name,
            desktopLyricsSurfaceOpacity = value.desktopLyricsSurfaceOpacity.coerceIn(0, 100),
            desktopLyricsSurfaceOpacitySet = true,
            desktopLyricsAutoHideWhenPaused = value.desktopLyricsAutoHideWhenPaused,
            desktopLyricsControlsTimeout = value.desktopLyricsControlsTimeout.name,
            desktopLyricsLocked = value.desktopLyricsLocked,
            desktopLyricsPositionX = value.desktopLyricsPosition?.x ?: 0,
            desktopLyricsPositionY = value.desktopLyricsPosition?.y ?: 0,
            desktopLyricsPositionSet = value.desktopLyricsPosition != null,
            desktopLyricsFlagsSet = true,
        ),
    )

    override suspend fun reset() = storage.reset()
}

private inline fun <reified T : Enum<T>> StoredLyricsPreferences.enumOrDefault(
    default: T,
    value: StoredLyricsPreferences.() -> String,
): T = enumValues<T>().firstOrNull { it.name == value() } ?: default
