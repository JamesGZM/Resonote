package com.resonote.core.karaoke

import com.resonote.core.model.KaraokeProjectId
import com.resonote.core.model.KaraokeSourceMode
import com.resonote.core.playback.PlaybackItem
import kotlinx.coroutines.flow.StateFlow

sealed interface KaraokeSessionStatus {
    data object Off : KaraokeSessionStatus

    data object Preparing : KaraokeSessionStatus

    data class Countdown(val secondsRemaining: Int) : KaraokeSessionStatus

    data class Recording(
        val projectId: KaraokeProjectId,
        val elapsedMillis: Long,
        val hasOfficialAccompaniment: Boolean,
    ) : KaraokeSessionStatus

    data class Paused(val projectId: KaraokeProjectId) : KaraokeSessionStatus

    data class Failed(val reason: KaraokeSessionFailure) : KaraokeSessionStatus
}

enum class KaraokeSessionFailure {
    UnsupportedSource,
    SourceUnavailable,
    MicrophoneUnavailable,
    InsufficientStorage,
    StorageUnavailable,
}

data class KaraokeSessionState(
    val enabled: Boolean = false,
    val continuousRecordingArmed: Boolean = false,
    val status: KaraokeSessionStatus = KaraokeSessionStatus.Off,
    val availableSourceModes: Set<KaraokeSourceMode> = emptySet(),
    val selectedSourceMode: KaraokeSourceMode = KaraokeSourceMode.Original,
    val sourceChangeInProgress: Boolean = false,
    val savingInProgress: Boolean = false,
    val failure: KaraokeSessionFailure? = null,
)

interface KaraokeController {
    val state: StateFlow<KaraokeSessionState>

    fun enable(item: PlaybackItem)

    fun disable()

    fun start()

    fun selectSource(sourceMode: KaraokeSourceMode)

    fun pause()

    fun resume()

    fun previous()

    fun next()

    fun stopAndSave()

    fun acknowledgeFailure()
}
