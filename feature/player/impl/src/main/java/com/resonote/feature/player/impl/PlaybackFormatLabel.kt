package com.resonote.feature.player.impl

import com.resonote.core.designsystem.component.compactBadgeLabel
import com.resonote.core.playback.PlaybackFormat
import java.util.Locale

fun PlaybackFormat.badgeLabel(): String? = when (this) {
    is PlaybackFormat.Online -> quality.compactBadgeLabel()
    is PlaybackFormat.Cloud -> extension?.uppercase()
    is PlaybackFormat.Local -> buildList {
        (extension ?: mimeType?.substringAfterLast('/'))?.uppercase()?.let(::add)
        sampleRateHz?.let { add(it.sampleRateLabel()) }
        bitDepth?.let { add("$it-bit") }
        bitrateBitsPerSecond?.let { add("${it / 1_000} kbps") }
    }.takeIf(List<String>::isNotEmpty)?.joinToString(" · ")
}

private fun Int.sampleRateLabel(): String = if (this % 1_000 == 0) {
    "${this / 1_000} kHz"
} else {
    "${String.format(Locale.ROOT, "%.1f", this / 1_000.0)} kHz"
}
