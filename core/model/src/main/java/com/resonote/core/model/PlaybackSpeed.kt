package com.resonote.core.model

enum class PlaybackSpeed(val factor: Float, val percent: Int) {
    Half(0.5f, 50),
    ThreeQuarters(0.75f, 75),
    Normal(1f, 100),
    OneAndQuarter(1.25f, 125),
    OneAndHalf(1.5f, 150),
    Double(2f, 200),
    ;

    companion object {
        fun fromPercent(percent: Int): PlaybackSpeed = entries.firstOrNull { it.percent == percent } ?: Normal
    }
}
