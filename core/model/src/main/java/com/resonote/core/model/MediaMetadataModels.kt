package com.resonote.core.model

data class SearchKeyword(val keyword: String, val reason: String)
data class SearchArtist(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val albumCount: Int,
    val songCount: Int,
)
data class SearchAlbum(
    val id: String,
    val name: String,
    val artist: String?,
    val coverUrl: String?,
    val songCount: Int,
    val publishDate: String,
)
data class SearchPlaylist(
    val id: String,
    val name: String,
    val creator: String?,
    val coverUrl: String?,
    val songCount: Int,
    val playCount: Long,
)
data class SearchMv(
    val hash: String,
    val name: String,
    val singer: String?,
    val coverUrl: String?,
    val durationMillis: Long,
)
data class SearchPage<T>(val items: List<T>, val page: Int, val total: Int, val hasMore: Boolean)
data class ComplexSearchResult(
    val artists: List<SearchArtist>,
    val songs: List<OnlineSong>,
    val songsTotal: Int,
    val albums: List<SearchAlbum>,
    val albumsTotal: Int,
    val playlists: List<SearchPlaylist>,
    val playlistsTotal: Int,
    val mvs: List<SearchMv>,
    val mvsTotal: Int,
)
enum class LyricsVocalAlignment { Leading, Trailing, Unspecified }

data class LyricSyllable(
    val text: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val phonetic: String? = null,
)

data class LyricLine(
    val syllables: List<LyricSyllable>,
    val translation: String? = null,
    val alignment: LyricsVocalAlignment = LyricsVocalAlignment.Unspecified,
    val backgroundLines: List<LyricLine> = emptyList(),
) {
    val timeMillis: Long get() = syllables.firstOrNull()?.startTimeMillis ?: 0L
    val endTimeMillis: Long get() = syllables.lastOrNull()?.endTimeMillis ?: timeMillis
    val text: String get() = syllables.joinToString(separator = "", transform = LyricSyllable::text)
    val transliteration: String?
        get() = syllables.mapNotNull(LyricSyllable::phonetic).joinToString(separator = "").takeIf(String::isNotBlank)

    constructor(timeMillis: Long, text: String) : this(
        syllables = listOf(LyricSyllable(text, timeMillis, timeMillis + 1L)),
    )
}

data class LyricsDocument(val lines: List<LyricLine>)

enum class LyricsDisplayMode { Scrolling, SingleLine }
enum class LyricsHighlightMode { Word, Line }
enum class LyricsTextAlignment { Center, Start }
enum class LyricsFontSize { Small, Medium, Large }
enum class LyricsBackgroundMode { Palette, Artwork, Off }

data class LyricsPreferences(
    val translationEnabled: Boolean = true,
    val transliterationEnabled: Boolean = true,
    val displayMode: LyricsDisplayMode = LyricsDisplayMode.Scrolling,
    val highlightMode: LyricsHighlightMode = LyricsHighlightMode.Word,
    val textAlignment: LyricsTextAlignment = LyricsTextAlignment.Center,
    val fontSize: LyricsFontSize = LyricsFontSize.Medium,
    val backgroundMode: LyricsBackgroundMode = LyricsBackgroundMode.Artwork,
)
data class RecognitionMatch(val confidence: Double, val song: OnlineSong)
