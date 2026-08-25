package com.resonote.feature.player.impl

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteArtwork
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.resonoteHeroElement
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.LyricLine
import com.resonote.core.model.LyricsDisplayMode
import com.resonote.core.model.LyricsFontSize
import com.resonote.core.model.LyricsHighlightMode
import com.resonote.core.model.LyricsPreferences
import com.resonote.core.model.LyricsTextAlignment
import com.resonote.core.playback.PlaybackMetadata
import kotlinx.coroutines.delay

@Composable
internal fun PlayerPager(
    song: PlaybackMetadata,
    lyrics: LyricsUiState,
    preferences: LyricsPreferences,
    positionMillis: Long,
    palette: PlayerPalette,
    onSeek: (Long) -> Unit,
    onRetryLyrics: () -> Unit,
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, 1), pageCount = { 2 })
    LaunchedEffect(pagerState.currentPage) { onPageChanged(pagerState.currentPage) }
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth().clipToBounds(),
        verticalAlignment = Alignment.CenterVertically,
    ) { page ->
        if (page == 0) {
            CoverPage(song, palette)
        } else {
            LyricsPage(lyrics, preferences, positionMillis, palette, onSeek, onRetryLyrics)
        }
    }
}

@Composable
internal fun PlayerPageIndicator(currentPage: Int, palette: PlayerPalette) {
    Row(
        Modifier.padding(top = 10.dp, bottom = 17.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        repeat(2) { index ->
            Box(
                Modifier
                    .width(if (currentPage == index) 24.dp else 7.dp)
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(if (currentPage == index) palette.accent else palette.contentMuted),
            )
        }
    }
}

@Composable
private fun CoverPage(song: PlaybackMetadata, palette: PlayerPalette) {
    val shape = RoundedCornerShape(22.dp)
    val artworkSize = Modifier.widthIn(max = 342.dp).fillMaxWidth().aspectRatio(1f)
    Box(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            artworkSize
                .graphicsLayer {
                    scaleX = 0.99f
                    scaleY = 0.99f
                    alpha = 0.24f
                }
                .offset(y = 5.dp)
                .blur(20.dp, BlurredEdgeTreatment.Unbounded)
                .background(Color.Black, shape),
        )
        if (song.artworkUri.isNullOrBlank()) {
            Box(
                artworkSize
                    .graphicsLayer {
                        scaleX = 1.018f
                        scaleY = 1.018f
                        alpha = 0.12f
                    }
                    .blur(22.dp, BlurredEdgeTreatment.Unbounded)
                    .background(palette.accent, shape),
            )
        } else {
            AsyncImage(
                song.artworkUri,
                null,
                artworkSize.graphicsLayer {
                    scaleX = 1.018f
                    scaleY = 1.018f
                    alpha = 0.12f
                }.blur(22.dp, BlurredEdgeTreatment.Unbounded).clip(shape),
                contentScale = ContentScale.Crop,
            )
        }
        ResonoteArtwork(
            state = if (song.artworkUri.isNullOrBlank()) ResonoteArtworkState.MISSING else ResonoteArtworkState.LOADED,
            contentDescription = stringResource(R.string.feature_player_impl_artwork, song.title),
            modifier = artworkSize
                .testTag("player-cover-artwork")
                .shadow(
                    elevation = ResonoteTokens.elevation.level3.maximumShadow,
                    shape = shape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.16f),
                    spotColor = Color.Black.copy(alpha = 0.26f),
                )
                .resonoteHeroElement(ResonotePlayerHeroKeys.artwork(song.mediaId)),
            shape = shape,
        ) {
            if (!song.artworkUri.isNullOrBlank()) {
                AsyncImage(song.artworkUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
    }
}

@Composable
private fun LyricsPage(
    lyrics: LyricsUiState,
    preferences: LyricsPreferences,
    positionMillis: Long,
    palette: PlayerPalette,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (lyrics) {
            LyricsUiState.Idle, LyricsUiState.Loading -> CircularProgressIndicator(
                Modifier.size(32.dp),
                color = palette.accent,
                strokeWidth = 3.dp,
            )
            LyricsUiState.Empty, LyricsUiState.Unavailable -> LyricsMessage(
                stringResource(R.string.feature_player_impl_lyrics_empty),
                palette,
            )
            is LyricsUiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LyricsMessage(lyrics.failure.lyricsMessage(), palette)
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.feature_player_impl_retry))
                }
            }
            is LyricsUiState.Content -> SyncedLyrics(
                lyrics.document.lines,
                preferences,
                positionMillis,
                palette,
                onSeek,
            )
        }
    }
}

@Composable
private fun SyncedLyrics(
    lines: List<LyricLine>,
    preferences: LyricsPreferences,
    positionMillis: Long,
    palette: PlayerPalette,
    onSeek: (Long) -> Unit,
) {
    val activeIndex = lines.indexOfLast { it.timeMillis <= positionMillis }.coerceAtLeast(0)
    if (preferences.displayMode == LyricsDisplayMode.SingleLine) {
        LyricLineContent(lines[activeIndex], true, preferences, positionMillis, palette, onSeek)
        return
    }
    val listState = rememberLazyListState()
    val dragged by listState.interactionSource.collectIsDraggedAsState()
    var follow by remember { mutableStateOf(true) }
    LaunchedEffect(activeIndex, follow) {
        if (follow) {
            withFrameNanos {}
            if (listState.layoutInfo.visibleItemsInfo.none { it.index == activeIndex }) {
                val layoutInfo = listState.layoutInfo
                val estimatedItemHeight = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
                val centeredOffset = -((layoutInfo.viewportSize.height - estimatedItemHeight).coerceAtLeast(0) / 2)
                listState.animateScrollToItem(activeIndex, centeredOffset)
                withFrameNanos {}
            }
            val layoutInfo = listState.layoutInfo
            val target = layoutInfo.visibleItemsInfo.firstOrNull { it.index == activeIndex }
            if (target != null) {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                val targetCenter = target.offset + target.size / 2f
                listState.animateScrollBy(targetCenter - viewportCenter)
            }
        }
    }
    LaunchedEffect(dragged) {
        if (dragged) {
            follow = false
        } else if (!follow) {
            delay(3_500)
            follow = true
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().testTag("player-lyrics"),
            contentPadding = PaddingValues(vertical = maxHeight / 2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(lines, key = { index, line -> "${line.timeMillis}-$index" }) { index, line ->
                LyricLineContent(
                    line,
                    index == activeIndex,
                    preferences,
                    positionMillis,
                    palette,
                ) { targetPositionMillis ->
                    onSeek(targetPositionMillis)
                    follow = true
                }
            }
        }
    }
}

@Composable
private fun LyricLineContent(
    line: LyricLine,
    active: Boolean,
    preferences: LyricsPreferences,
    positionMillis: Long,
    palette: PlayerPalette,
    onSeek: (Long) -> Unit,
) {
    val alignment = if (preferences.textAlignment == LyricsTextAlignment.Center) TextAlign.Center else TextAlign.Start
    val activeSize = preferences.fontSize.activeSize()
    val inactiveSize = preferences.fontSize.inactiveSize()
    val emphasis by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "lyric line emphasis",
    )
    val baseColor = lerp(palette.contentMuted, palette.contentPrimary, emphasis)
    val lineScale = inactiveSize.value / activeSize.value +
        (1f - inactiveSize.value / activeSize.value) * emphasis
    val syllableProgress = line.syllables.map { syllable ->
        val target = when {
            !active -> 0f
            preferences.highlightMode == LyricsHighlightMode.Line -> 1f
            positionMillis <= syllable.startTimeMillis -> 0f
            positionMillis >= syllable.endTimeMillis -> 1f
            else -> (positionMillis - syllable.startTimeMillis).toFloat() /
                (syllable.endTimeMillis - syllable.startTimeMillis).coerceAtLeast(1L)
        }
        animateFloatAsState(
            targetValue = target.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 520, easing = LinearEasing),
            label = "lyric syllable progress",
        ).value
    }
    val annotated = buildAnnotatedString {
        line.syllables.forEachIndexed { index, syllable ->
            val characters = syllable.text.codePointStrings()
            characters.forEachIndexed { characterIndex, character ->
                val characterProgress = (
                    syllableProgress[index] * characters.size.coerceAtLeast(1) - characterIndex
                    ).coerceIn(0f, 1f)
                pushStyle(SpanStyle(color = lerp(baseColor, palette.accent, characterProgress)))
                append(character)
                pop()
            }
        }
    }
    Column(
        Modifier.fillMaxWidth()
            .then(if (active) Modifier.testTag("player-active-lyric") else Modifier)
            .graphicsLayer {
                scaleX = lineScale
                scaleY = lineScale
                transformOrigin = if (preferences.textAlignment == LyricsTextAlignment.Center) {
                    TransformOrigin.Center
                } else {
                    TransformOrigin(0f, 0.5f)
                }
            }
            .clip(CircleShape)
            .clickable { onSeek(line.timeMillis) }
            .padding(vertical = 6.dp),
        horizontalAlignment = if (preferences.textAlignment ==
            LyricsTextAlignment.Center
        ) {
            Alignment.CenterHorizontally
        } else {
            Alignment.Start
        },
    ) {
        Text(
            annotated,
            Modifier.fillMaxWidth(),
            textAlign = alignment,
            fontSize = activeSize,
            lineHeight = activeSize * 1.35f,
            fontWeight = FontWeight.Bold,
        )
        line.supplemental(preferences).forEachIndexed { index, supplemental ->
            Text(
                supplemental.text,
                Modifier.fillMaxWidth().padding(top = if (index == 0) 5.dp else 2.dp),
                color = lerp(
                    palette.contentMuted,
                    palette.contentSecondary,
                    emphasis * if (supplemental.isTransliteration) 0.72f else 1f,
                ),
                textAlign = alignment,
                fontSize = activeSize * if (supplemental.isTransliteration) 0.60f else 0.68f,
                lineHeight = activeSize * if (supplemental.isTransliteration) 0.82f else 0.9f,
            )
        }
    }
}

private fun String.codePointStrings(): List<String> = buildList {
    var offset = 0
    while (offset < length) {
        val next = offset + Character.charCount(Character.codePointAt(this@codePointStrings, offset))
        add(substring(offset, next))
        offset = next
    }
}

private data class SupplementalLyric(val text: String, val isTransliteration: Boolean)

private fun LyricLine.supplemental(preferences: LyricsPreferences): List<SupplementalLyric> = buildList {
    if (preferences.translationEnabled) {
        translation?.takeIf(String::isNotBlank)?.let { add(SupplementalLyric(it, false)) }
    }
    if (preferences.transliterationEnabled) {
        transliteration?.takeIf(String::isNotBlank)?.let { value ->
            if (none { it.text == value }) add(SupplementalLyric(value, true))
        }
    }
}

private fun LyricsFontSize.activeSize(): TextUnit = when (this) {
    LyricsFontSize.Small -> 21.sp
    LyricsFontSize.Medium -> 24.sp
    LyricsFontSize.Large -> 28.sp
}
private fun LyricsFontSize.inactiveSize(): TextUnit = when (this) {
    LyricsFontSize.Small -> 15.sp
    LyricsFontSize.Medium -> 17.sp
    LyricsFontSize.Large -> 20.sp
}
