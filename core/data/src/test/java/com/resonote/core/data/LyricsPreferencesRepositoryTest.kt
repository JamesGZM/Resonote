package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.datastore.LyricsPreferencesStorage
import com.resonote.core.model.DesktopLyricsControlsTimeout
import com.resonote.core.model.DesktopLyricsPosition
import com.resonote.core.model.LyricsBackgroundMode
import com.resonote.core.model.LyricsDisplayMode
import com.resonote.core.model.LyricsFontSize
import com.resonote.core.model.LyricsHighlightMode
import com.resonote.core.model.LyricsPreferences
import com.resonote.core.model.LyricsTextAlignment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import com.resonote.core.datastore.proto.LyricsPreferences as StoredLyricsPreferences

class LyricsPreferencesRepositoryTest {
    @Test
    fun missingOrUnknownStoredValuesUseLockedDefaults() = runTest {
        val storage = FakeLyricsPreferencesStorage(
            StoredLyricsPreferences(displayMode = "FutureMode", fontSize = "Unknown"),
        )

        assertThat(DefaultLyricsPreferencesRepository(storage).preferences.first())
            .isEqualTo(LyricsPreferences())
    }

    @Test
    fun everyPreferencePersistsAndResetRestoresDefaults() = runTest {
        val storage = FakeLyricsPreferencesStorage()
        val repository = DefaultLyricsPreferencesRepository(storage)
        val selected = LyricsPreferences(
            translationEnabled = false,
            transliterationEnabled = true,
            displayMode = LyricsDisplayMode.SingleLine,
            highlightMode = LyricsHighlightMode.Line,
            textAlignment = LyricsTextAlignment.Start,
            fontSize = LyricsFontSize.Large,
            backgroundMode = LyricsBackgroundMode.Off,
            desktopLyricsEnabled = true,
            desktopLyricsSurfaceOpacity = 70,
            desktopLyricsBackgroundColorArgb = 0xFF112233.toInt(),
            desktopLyricsForegroundColorArgb = 0xFFCC8844.toInt(),
            desktopLyricsShadowColorArgb = 0xFF245B83.toInt(),
            desktopLyricsShadowOffsetXDp = -3f,
            desktopLyricsShadowOffsetYDp = 4f,
            desktopLyricsShadowBlurRadiusDp = 8f,
            desktopLyricsWidthPercent = 70,
            desktopLyricsFontSizeSp = 31,
            desktopLyricsOutlineColorArgb = 0xFFEEEEDD.toInt(),
            desktopLyricsOutlineWidthDp = 2.5f,
            desktopLyricsAutoHideWhenPaused = false,
            desktopLyricsControlsTimeout = DesktopLyricsControlsTimeout.ThreeSeconds,
            desktopLyricsLocked = true,
            desktopLyricsPosition = DesktopLyricsPosition(-120, 480),
        )

        repository.setPreferences(selected)
        assertThat(repository.preferences.first()).isEqualTo(selected)

        repository.reset()
        assertThat(repository.preferences.first()).isEqualTo(LyricsPreferences())
    }

    @Test
    fun legacyDesktopStyleUsesNewWhiteAndBrandDefaults() = runTest {
        val storage = FakeLyricsPreferencesStorage(
            StoredLyricsPreferences(desktopLyricsSurfaceOpacity = 0, desktopLyricsSurfaceOpacitySet = true),
        )

        val restored = DefaultLyricsPreferencesRepository(storage).preferences.first()

        assertThat(restored.desktopLyricsSurfaceOpacity).isEqualTo(100)
        assertThat(restored.desktopLyricsBackgroundColorArgb).isEqualTo(0xFFFFFFFF.toInt())
        assertThat(restored.desktopLyricsForegroundColorArgb).isEqualTo(0xFFAE2A4B.toInt())
        assertThat(restored.desktopLyricsWidthPercent).isEqualTo(100)
    }

    @Test
    fun legacySingleChoicePreferenceMigratesToBothSupplementalTextsEnabled() = runTest {
        val storage = FakeLyricsPreferencesStorage(
            StoredLyricsPreferences(supplementalText = "Transliteration"),
        )

        assertThat(DefaultLyricsPreferencesRepository(storage).preferences.first())
            .isEqualTo(LyricsPreferences())
    }
}

private class FakeLyricsPreferencesStorage(
    initial: StoredLyricsPreferences = StoredLyricsPreferences.getDefaultInstance(),
) : LyricsPreferencesStorage {
    private val state = MutableStateFlow(initial)
    override val values = state

    override suspend fun update(value: StoredLyricsPreferences) {
        state.value = value
    }

    override suspend fun reset() {
        state.value = StoredLyricsPreferences.getDefaultInstance()
    }
}
