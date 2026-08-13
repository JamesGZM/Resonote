package com.resonote.core.data

import com.resonote.core.model.LyricLine
import com.resonote.core.network.LyricsNetworkDataSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
internal class DefaultLyricsRepository @Inject constructor(
    private val network: LyricsNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
) : LyricsRepository {
    override suspend fun loadLyrics(hash: String, albumAudioId: String?) = loadCollection(riskChallenges) {
        require(hash.isNotBlank()) { "hash must not be blank" }
        val candidate = network.searchLyric(hash, albumAudioId) ?: return@loadCollection emptyList()
        network.downloadLyric(candidate)?.parseLyrics().orEmpty()
    }

    private fun String.parseLyrics(): List<LyricLine> = parseLrc().ifEmpty { parseKrc() }

    private fun String.parseLrc(): List<LyricLine> = buildList {
        lineSequence().forEach { raw ->
            val text = raw.replace(LRC_PATTERN, "").trim()
            if (text.isEmpty()) return@forEach
            LRC_PATTERN.findAll(raw).forEach { match ->
                val fraction = match.groupValues[3].ifEmpty { "0" }
                val fractionMillis = fraction.toLong() * 10.0.pow(3 - fraction.length).toLong()
                val millis = match.groupValues[1].toLong() * 60_000 + match.groupValues[2].toLong() * 1_000 + fractionMillis
                add(LyricLine(millis, text))
            }
        }
    }.sortedBy(LyricLine::timeMillis).distinctBy { it.timeMillis to it.text }

    private fun String.parseKrc(): List<LyricLine> = lineSequence().mapNotNull { raw ->
        val match = KRC_LINE_PATTERN.matchEntire(raw.trim()) ?: return@mapNotNull null
        val text = match.groupValues[2].replace(KRC_SYLLABLE_PATTERN, "").trim()
        text.takeIf(String::isNotEmpty)?.let { LyricLine(match.groupValues[1].toLong(), it) }
    }.toList().sortedBy(LyricLine::timeMillis).distinctBy { it.timeMillis to it.text }

    private companion object {
        val LRC_PATTERN = Regex("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")
        val KRC_LINE_PATTERN = Regex("\\[(\\d+),\\d+](.*)")
        val KRC_SYLLABLE_PATTERN = Regex("<\\d+,\\d+,\\d+>")
    }
}
