package com.resonote.core.data

import com.resonote.core.datastore.LyricsPreferencesStorage
import com.resonote.core.model.LyricsBackgroundMode
import com.resonote.core.model.LyricsDisplayMode
import com.resonote.core.model.LyricsFontSize
import com.resonote.core.model.LyricsHighlightMode
import com.resonote.core.model.LyricsPreferences
import com.resonote.core.model.LyricsSupplementalText
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
            supplementalText = stored.enumOrDefault(LyricsSupplementalText.Translation) { supplementalText },
            displayMode = stored.enumOrDefault(LyricsDisplayMode.Scrolling) { displayMode },
            highlightMode = stored.enumOrDefault(LyricsHighlightMode.Word) { highlightMode },
            textAlignment = stored.enumOrDefault(LyricsTextAlignment.Center) { textAlignment },
            fontSize = stored.enumOrDefault(LyricsFontSize.Medium) { fontSize },
            backgroundMode = stored.enumOrDefault(LyricsBackgroundMode.Artwork) { backgroundMode },
        )
    }

    override suspend fun setPreferences(value: LyricsPreferences) = storage.update(
        StoredLyricsPreferences(
            value.supplementalText.name,
            value.displayMode.name,
            value.highlightMode.name,
            value.textAlignment.name,
            value.fontSize.name,
            value.backgroundMode.name,
        ),
    )

    override suspend fun reset() = storage.reset()
}

private inline fun <reified T : Enum<T>> StoredLyricsPreferences.enumOrDefault(
    default: T,
    value: StoredLyricsPreferences.() -> String,
): T = enumValues<T>().firstOrNull { it.name == value() } ?: default
