package com.resonote.core.data

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.parser.KugouKrcParser
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.LyricLine
import com.resonote.core.model.LyricSyllable
import com.resonote.core.model.LyricsDocument
import com.resonote.core.model.LyricsVocalAlignment
import com.resonote.core.network.LyricsNetworkDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
internal class DefaultLyricsRepository @Inject constructor(
    private val network: LyricsNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
) : LyricsRepository {
    override suspend fun loadLyrics(hash: String, albumAudioId: String?) = try {
        loadCollection(riskChallenges) {
            require(hash.isNotBlank()) { "hash must not be blank" }
            val candidate = network.searchLyric(hash, albumAudioId)
                ?: return@loadCollection LyricsDocument(emptyList())
            val source = network.downloadLyric(candidate)
                ?: return@loadCollection LyricsDocument(emptyList())
            source.parseLyrics().takeIf { it.lines.isNotEmpty() }
                ?: throw MalformedLyricsException
        }
    } catch (_: MalformedLyricsException) {
        CollectionLoadResult.Failed(ContentFailure.Protocol)
    }

    private suspend fun String.parseLyrics(): LyricsDocument = withContext(Dispatchers.Default) {
        try {
            val krc = parseKrc()
            LyricsDocument(if (krc.isNotEmpty()) krc else parseLrc())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            throw MalformedLyricsException
        }
    }

    private fun String.parseLrc(): List<LyricLine> = buildList {
        lineSequence().forEach { raw ->
            val text = raw.replace(LRC_PATTERN, "").trim()
            if (text.isEmpty()) return@forEach
            LRC_PATTERN.findAll(raw).forEach { match ->
                val fraction = match.groupValues[3].ifEmpty { "0" }
                val fractionMillis = fraction.toLong() * 10.0.pow(3 - fraction.length).toLong()
                val millis =
                    match.groupValues[1].toLong() * 60_000 + match.groupValues[2].toLong() * 1_000 + fractionMillis
                add(lineTimedLyric(millis, text))
            }
        }
    }.sortedBy(LyricLine::timeMillis).distinctBy { it.timeMillis to it.text }

    private fun String.parseKrc(): List<LyricLine> {
        if (!KugouKrcParser.canParse(this)) return emptyList()
        return KugouKrcParser.parse(normalizeKrcLanguageMetadata()).lines
            .mapNotNull { (it as? KaraokeLine.MainKaraokeLine)?.toDomainOrNull() }
            .sortedBy(LyricLine::timeMillis)
            .distinctBy { it.timeMillis to it.text }
    }

    private fun String.normalizeKrcLanguageMetadata(): String {
        val match = KRC_LANGUAGE_PATTERN.find(this) ?: return this
        val normalized = runCatching {
            val decoded = Base64.getDecoder().decode(match.groupValues[1]).decodeToString()
            val root = Json.parseToJsonElement(decoded) as? JsonObject ?: return@runCatching null
            val content = root["content"] as? JsonArray ?: return@runCatching null
            val normalizedContent = content.mapNotNull { element ->
                val block = element as? JsonObject ?: return@mapNotNull null
                if (block["type"]?.jsonPrimitive?.intOrNull == PHONETIC_METADATA_TYPE) {
                    block.normalizeKrcPhonetics()
                } else {
                    block
                }
            }
            JsonObject(root + ("content" to JsonArray(normalizedContent)))
        }.getOrNull() ?: return this
        val encoded = Base64.getEncoder().encodeToString(normalized.toString().encodeToByteArray())
        return replaceRange(match.range, "[language:$encoded]")
    }

    private fun JsonObject.normalizeKrcPhonetics(): JsonObject? {
        // Kugou emits both ["phonetic"] and [["phonetic"]], while the parser accepts only the latter.
        val rows = get("lyricContent") as? JsonArray ?: return null
        val normalizedRows = buildList {
            rows.forEach { rowElement ->
                val row = rowElement as? JsonArray ?: return null
                val normalizedSyllables = buildList {
                    row.forEach { syllable ->
                        when (syllable) {
                            is JsonPrimitive -> add(JsonArray(listOf(syllable)))
                            is JsonArray -> {
                                if (syllable.any { it !is JsonPrimitive }) return null
                                add(syllable)
                            }
                            else -> return null
                        }
                    }
                }
                add(JsonArray(normalizedSyllables))
            }
        }
        return JsonObject(this + ("lyricContent" to JsonArray(normalizedRows)))
    }

    private fun KaraokeLine.MainKaraokeLine.toDomainOrNull(): LyricLine? {
        val mapped = syllables.mapNotNull { it.toDomainOrNull() }
        if (mapped.joinToString(separator = "", transform = LyricSyllable::text).isBlank()) return null
        return LyricLine(
            syllables = mapped,
            translation = translation?.trim()?.takeIf(String::isNotBlank),
            alignment = alignment.toDomain(),
            backgroundLines = accompanimentLines.orEmpty().mapNotNull { line ->
                val syllables = line.syllables.mapNotNull { it.toDomainOrNull() }
                syllables.takeIf(List<LyricSyllable>::isNotEmpty)?.let {
                    LyricLine(it, line.translation?.trim()?.takeIf(String::isNotBlank), line.alignment.toDomain())
                }
            },
        )
    }

    private fun KaraokeSyllable.toDomainOrNull(): LyricSyllable? =
        takeIf { content.isNotEmpty() && start >= 0 && end > start }?.let {
            LyricSyllable(
                text = content,
                startTimeMillis = start.toLong(),
                endTimeMillis = end.toLong(),
                phonetic = phonetic?.trim()?.takeIf(String::isNotBlank),
            )
        }

    private fun KaraokeAlignment.toDomain() = when (this) {
        KaraokeAlignment.Start -> LyricsVocalAlignment.Leading
        KaraokeAlignment.End -> LyricsVocalAlignment.Trailing
        KaraokeAlignment.Unspecified -> LyricsVocalAlignment.Unspecified
    }

    private companion object {
        data object MalformedLyricsException : RuntimeException()
        val LRC_PATTERN = Regex("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")
        val KRC_LANGUAGE_PATTERN = Regex("(?m)^\\[language:([^]]+)]\\r?$")
        const val PHONETIC_METADATA_TYPE = 0
        fun lineTimedLyric(timeMillis: Long, text: String) = LyricLine(
            syllables = listOf(LyricSyllable(text, timeMillis, timeMillis + 1L)),
        )
    }
}
