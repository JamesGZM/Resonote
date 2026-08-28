package com.resonote.core.playback.service

import com.resonote.core.model.LyricLine
import com.resonote.core.model.LyricsDocument

internal data class DesktopLyricsContent(val primary: String, val primaryHighlightTextOffset: Float)

internal object DesktopLyricsRenderer {
    fun render(document: LyricsDocument, positionMillis: Long): DesktopLyricsContent? {
        val lines = document.lines
        if (lines.isEmpty()) return null
        val activeIndex = lines.indexOfLast { it.timeMillis <= positionMillis }.coerceAtLeast(0)
        val active = lines[activeIndex]
        val nextLineStartMillis = lines.getOrNull(activeIndex + 1)?.timeMillis
        return DesktopLyricsContent(
            primary = active.text.replace('\n', ' '),
            primaryHighlightTextOffset = active.highlightTextOffset(positionMillis, nextLineStartMillis),
        )
    }

    private fun LyricLine.highlightTextOffset(positionMillis: Long, nextLineStartMillis: Long?): Float {
        val onlySyllable = syllables.singleOrNull()
        if (onlySyllable != null && onlySyllable.endTimeMillis - onlySyllable.startTimeMillis <= 1L) {
            val endMillis = nextLineStartMillis?.takeIf { it > timeMillis } ?: (timeMillis + 5_000L)
            val progress = (positionMillis - timeMillis).toFloat() / (endMillis - timeMillis).coerceAtLeast(1L)
            return (text.length * progress.coerceIn(0f, 1f)).coerceIn(0f, text.length.toFloat())
        }

        return syllables.sumOf { syllable ->
            when {
                positionMillis <= syllable.startTimeMillis -> 0.0
                positionMillis >= syllable.endTimeMillis -> syllable.text.length.toDouble()
                else -> {
                    val duration = (syllable.endTimeMillis - syllable.startTimeMillis).coerceAtLeast(1L)
                    val progress = (positionMillis - syllable.startTimeMillis).toDouble() / duration
                    syllable.text.length * progress
                }
            }
        }.toFloat().coerceIn(0f, text.length.toFloat())
    }
}
