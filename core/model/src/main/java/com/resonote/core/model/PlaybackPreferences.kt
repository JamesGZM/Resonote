package com.resonote.core.model

enum class PlaybackMode {
    ListLoop,
    Shuffle,
    SingleLoop,
    Sequential,
}

enum class CrossfadeDuration(val millis: Int) {
    Off(0),
    ThreeSeconds(3_000),
    FiveSeconds(5_000),
    EightSeconds(8_000),
}

enum class AudioFocusPolicy {
    AllowAll,
    AllowMedia,
    Disallow,
}

enum class EqualizerPreset(val enabled: Boolean, val lowDb: Int, val midDb: Int, val highDb: Int) {
    Off(false, 0, 0, 0),
    Flat(true, 0, 0, 0),
    BassBoost(true, 6, 0, -1),
    Pop(true, 3, 1, 4),
    Rock(true, 5, -2, 4),
    Jazz(true, 3, 1, 3),
    Classical(true, 2, -1, 3),
    Vocal(true, -2, 5, 2),
    Custom(true, 0, 0, 0),
    ;

    companion object {
        fun from(enabled: Boolean, lowDb: Int, midDb: Int, highDb: Int, custom: Boolean = false): EqualizerPreset {
            if (!enabled) return Off
            if (custom) return Custom
            return entries.firstOrNull { preset ->
                preset != Off &&
                    preset != Custom &&
                    preset.lowDb == lowDb &&
                    preset.midDb == midDb &&
                    preset.highDb == highDb
            } ?: Custom
        }
    }
}

data class PlaybackPreferences(
    val playbackSpeed: PlaybackSpeed = PlaybackSpeed.Normal,
    val onlinePlaybackQuality: OnlinePlaybackQuality = OnlinePlaybackQuality.Standard,
    val playbackMode: PlaybackMode = PlaybackMode.ListLoop,
    val gaplessEnabled: Boolean = true,
    val crossfadeDuration: CrossfadeDuration = CrossfadeDuration.Off,
    val loudnessNormalizationEnabled: Boolean = false,
    val audioFocusPolicy: AudioFocusPolicy = AudioFocusPolicy.Disallow,
    val equalizerEnabled: Boolean = false,
    val equalizerLowDb: Int = 0,
    val equalizerMidDb: Int = 0,
    val equalizerHighDb: Int = 0,
    val equalizerCustom: Boolean = false,
) {
    val equalizerPreset: EqualizerPreset
        get() = EqualizerPreset.from(
            equalizerEnabled,
            equalizerLowDb,
            equalizerMidDb,
            equalizerHighDb,
            equalizerCustom,
        )
}
