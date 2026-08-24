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

data class PlaybackPreferences(
    val playbackSpeed: PlaybackSpeed = PlaybackSpeed.Normal,
    val onlinePlaybackQuality: OnlinePlaybackQuality = OnlinePlaybackQuality.Standard,
    val playbackMode: PlaybackMode = PlaybackMode.ListLoop,
    val gaplessEnabled: Boolean = true,
    val crossfadeDuration: CrossfadeDuration = CrossfadeDuration.Off,
    val loudnessNormalizationEnabled: Boolean = false,
    val audioFocusPolicy: AudioFocusPolicy = AudioFocusPolicy.Disallow,
)
