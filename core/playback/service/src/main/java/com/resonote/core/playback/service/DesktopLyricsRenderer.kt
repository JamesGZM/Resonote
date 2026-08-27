package com.resonote.core.playback.service

import com.resonote.core.model.DesktopLyricsDisplayMode
import com.resonote.core.model.LyricLine
import com.resonote.core.model.LyricsDocument
import com.resonote.core.model.LyricsPreferences

internal data class DesktopLyricsContent(
    val primary: String,
    val primaryHighlightTextOffset: Float,
    val supplemental: String?,
    val next: String?,
    val layoutReference: String,
)

internal object DesktopLyricsRenderer {
    fun render(document: LyricsDocument, positionMillis: Long, preferences: LyricsPreferences): DesktopLyricsContent? {
        val lines = document.lines
        if (lines.isEmpty()) return null
        val activeIndex = lines.indexOfLast { it.timeMillis <= positionMillis }.coerceAtLeast(0)
        val active = lines[activeIndex]
        val layoutReference = lines
            .flatMap { line -> listOfNotNull(line.text, line.supplementalText(preferences)) }
            .maxByOrNull(String::length)
            ?: active.text
        return DesktopLyricsContent(
            primary = active.text,
            primaryHighlightTextOffset = active.highlightTextOffset(positionMillis),
            supplemental = active.supplementalText(preferences),
            next = if (preferences.desktopLyricsDisplayMode == DesktopLyricsDisplayMode.TwoLines) {
                lines.getOrNull(activeIndex + 1)?.text
            } else {
                null
            },
            layoutReference = layoutReference,
        )
    }

    private fun LyricLine.highlightTextOffset(positionMillis: Long): Float = syllables.sumOf { syllable ->
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

    private fun LyricLine.supplementalText(preferences: LyricsPreferences): String? = buildList {
        if (preferences.translationEnabled) translation?.takeIf(String::isNotBlank)?.let(::add)
        if (preferences.transliterationEnabled) transliteration?.takeIf(String::isNotBlank)?.let(::add)
    }.joinToString(" · ").takeIf(String::isNotBlank)
}
