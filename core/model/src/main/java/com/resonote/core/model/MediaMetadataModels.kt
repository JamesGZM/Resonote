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
enum class DesktopLyricsControlsTimeout(val seconds: Int) { ThreeSeconds(3), FiveSeconds(5), EightSeconds(8) }

data class DesktopLyricsPosition(val x: Int, val y: Int)

object DesktopLyricsDefaults {
    val BACKGROUND_COLOR_ARGB: Int = 0xFFFFFFFF.toInt()
    val FOREGROUND_COLOR_ARGB: Int = 0xFFAE2A4B.toInt()
    val SHADOW_COLOR_ARGB: Int = 0xFF000000.toInt()
    val OUTLINE_COLOR_ARGB: Int = 0xFF000000.toInt()
    const val SURFACE_OPACITY = 100
    const val WIDTH_PERCENT = 100
    const val FONT_SIZE_SP = 24
    const val SHADOW_OFFSET_X_DP = 0f
    const val SHADOW_OFFSET_Y_DP = 1f
    const val SHADOW_BLUR_RADIUS_DP = 2f
    const val OUTLINE_WIDTH_DP = 0f
}

data class LyricsPreferences(
    val translationEnabled: Boolean = true,
    val transliterationEnabled: Boolean = true,
    val displayMode: LyricsDisplayMode = LyricsDisplayMode.Scrolling,
    val highlightMode: LyricsHighlightMode = LyricsHighlightMode.Word,
    val textAlignment: LyricsTextAlignment = LyricsTextAlignment.Center,
    val fontSize: LyricsFontSize = LyricsFontSize.Medium,
    val backgroundMode: LyricsBackgroundMode = LyricsBackgroundMode.Artwork,
    val desktopLyricsEnabled: Boolean = false,
    val desktopLyricsSurfaceOpacity: Int = DesktopLyricsDefaults.SURFACE_OPACITY,
    val desktopLyricsBackgroundColorArgb: Int = DesktopLyricsDefaults.BACKGROUND_COLOR_ARGB,
    val desktopLyricsForegroundColorArgb: Int = DesktopLyricsDefaults.FOREGROUND_COLOR_ARGB,
    val desktopLyricsShadowColorArgb: Int = DesktopLyricsDefaults.SHADOW_COLOR_ARGB,
    val desktopLyricsShadowOffsetXDp: Float = DesktopLyricsDefaults.SHADOW_OFFSET_X_DP,
    val desktopLyricsShadowOffsetYDp: Float = DesktopLyricsDefaults.SHADOW_OFFSET_Y_DP,
    val desktopLyricsShadowBlurRadiusDp: Float = DesktopLyricsDefaults.SHADOW_BLUR_RADIUS_DP,
    val desktopLyricsWidthPercent: Int = DesktopLyricsDefaults.WIDTH_PERCENT,
    val desktopLyricsFontSizeSp: Int = DesktopLyricsDefaults.FONT_SIZE_SP,
    val desktopLyricsOutlineColorArgb: Int = DesktopLyricsDefaults.OUTLINE_COLOR_ARGB,
    val desktopLyricsOutlineWidthDp: Float = DesktopLyricsDefaults.OUTLINE_WIDTH_DP,
    val desktopLyricsAutoHideWhenPaused: Boolean = false,
    val desktopLyricsControlsTimeout: DesktopLyricsControlsTimeout = DesktopLyricsControlsTimeout.FiveSeconds,
    val desktopLyricsLocked: Boolean = false,
    val desktopLyricsPosition: DesktopLyricsPosition? = null,
)
data class RecognitionMatch(val confidence: Double, val song: OnlineSong)
