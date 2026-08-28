package com.resonote.core.datastore

import androidx.datastore.core.DataStoreFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProtoPlaybackPreferencesStorageTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun playbackSpeedPersistsAcrossStoreInstances() = runTest {
        val file = File(temporaryFolder.newFolder("preferences"), "playback_preferences.pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val firstStore = DataStoreFactory.create(
            serializer = PlaybackPreferencesSerializer,
            scope = firstScope,
            produceFile = { file },
        )

        try {
            ProtoPlaybackPreferencesStorage(firstStore).apply {
                setPlaybackSpeedPercent(150)
                setOnlinePlaybackQuality("HighResolution")
                setPlaybackMode("Shuffle")
                setGaplessEnabled(false)
                setCrossfadeDuration("FiveSeconds")
                setLoudnessNormalizationEnabled(true)
                setAudioFocusPolicy("AllowMedia")
                setEqualizerEnabled(true)
                setEqualizerGains(6, -3, 9)
            }
        } finally {
            firstScope.cancel()
        }

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val secondStore = DataStoreFactory.create(
            serializer = PlaybackPreferencesSerializer,
            scope = secondScope,
            produceFile = { file },
        )
        try {
            val storage = ProtoPlaybackPreferencesStorage(secondStore)
            assertThat(storage.playbackSpeedPercent.first()).isEqualTo(150)
            assertThat(storage.onlinePlaybackQuality.first()).isEqualTo("HighResolution")
            assertThat(storage.playbackMode.first()).isEqualTo("Shuffle")
            assertThat(storage.gaplessEnabled.first()).isFalse()
            assertThat(storage.crossfadeDuration.first()).isEqualTo("FiveSeconds")
            assertThat(storage.loudnessNormalizationEnabled.first()).isTrue()
            assertThat(storage.audioFocusPolicy.first()).isEqualTo("AllowMedia")
            assertThat(storage.equalizerEnabled.first()).isTrue()
            assertThat(storage.equalizerLowDb.first()).isEqualTo(6)
            assertThat(storage.equalizerMidDb.first()).isEqualTo(-3)
            assertThat(storage.equalizerHighDb.first()).isEqualTo(9)
            assertThat(storage.equalizerCustom.first()).isTrue()

            storage.setEqualizerPreset(true, 5, -2, 4)
            assertThat(storage.equalizerCustom.first()).isFalse()
        } finally {
            secondScope.cancel()
        }
    }
}
