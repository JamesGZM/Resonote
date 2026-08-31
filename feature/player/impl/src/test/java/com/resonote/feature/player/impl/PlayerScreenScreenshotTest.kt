package com.resonote.feature.player.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.karaoke.KaraokeSessionState
import com.resonote.core.karaoke.KaraokeSessionStatus
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.KaraokeProjectId
import com.resonote.core.model.KaraokeSourceMode
import com.resonote.core.model.LyricLine
import com.resonote.core.model.LyricSyllable
import com.resonote.core.model.LyricsDocument
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackState
import com.resonote.core.playback.PlaybackStatus
import com.resonote.core.screenshottesting.DefaultRoborazziOptions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-420dpi")
@OptIn(ExperimentalRoborazziApi::class)
class PlayerScreenScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun player_compactCover() {
        setPlayerContent()

        composeRule.onNodeWithTag("player-cover-artwork")
            .assertWidthIsEqualTo(342.dp)
            .assertLeftPositionInRootIsEqualTo(24.dp)

        captureScreenRoboImage(
            filePath = "src/test/screenshots/Player/PlayerCompact_cover.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun player_compactOfflineProgress() {
        val initialState = screenshotState()
        val downloadedItem = initialState.playback.queue.first().copy(
            resolvedSource = ResolvedSongSource(
                uri = "https://media.example/downloaded.flac",
                durationMillis = 248_000,
                extension = "flac",
                cacheKey = "download:current",
                isOffline = true,
            ),
        )
        setPlayerContent(
            initialState = initialState.copy(
                playback = initialState.playback.copy(
                    queue = listOf(downloadedItem) + initialState.playback.queue.drop(1),
                ),
            ),
        )

        captureScreenRoboImage(
            filePath = "src/test/screenshots/Player/PlayerCompact_offline-progress.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun player_compactLyrics() {
        setPlayerContent(initialPage = 1)

        composeRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Queue").assertIsDisplayed()
        composeRule.onNodeWithText("SQ").assertIsDisplayed()
        composeRule.onNodeWithText("飞的宝贝 请接受我的心意 我是你的专属").assertIsDisplayed()
        composeRule.onNodeWithText("cháo xībǎ jì yìtuī huí àn biān").assertIsDisplayed()
        val lyricsBounds = composeRule.onNodeWithTag("player-lyrics").fetchSemanticsNode().boundsInRoot
        val activeBounds = composeRule.onNodeWithTag("player-active-lyric").fetchSemanticsNode().boundsInRoot
        assertEquals(lyricsBounds.center.y.toDouble(), activeBounds.center.y.toDouble(), 1.0)

        captureScreenRoboImage(
            filePath = "src/test/screenshots/Player/PlayerCompact_lyrics.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun player_compactKaraokeReady() {
        setPlayerContent(
            initialState = screenshotState().copy(
                playback = screenshotState().playback.copy(positionMillis = 10_000),
                karaoke = KaraokeSessionState(
                    enabled = true,
                    availableSourceModes = setOf(KaraokeSourceMode.Accompaniment, KaraokeSourceMode.Original),
                    selectedSourceMode = KaraokeSourceMode.Accompaniment,
                ),
            ),
        )
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Backing").assertIsDisplayed()
        composeRule.onNodeWithText("Original").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Skip intro").assertIsDisplayed()
        captureScreenRoboImage(
            filePath = "src/test/screenshots/Player/PlayerCompact_karaoke_ready.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun player_karaokePreparingShowsSkipAction() {
        setPlayerContent(
            initialState = screenshotState().copy(
                playback = screenshotState().playback.copy(positionMillis = 0),
                karaoke = KaraokeSessionState(
                    enabled = true,
                    status = KaraokeSessionStatus.Preparing,
                ),
            ),
        )

        composeRule.onNodeWithContentDescription("Skip intro").assertIsDisplayed()
    }

    @Test
    fun karaokeIntroTargetSkipsCreditLines() {
        val lyrics = LyricsUiState.Content(
            LyricsDocument(
                listOf(
                    LyricLine(1_000, "词：虎二"),
                    LyricLine(2_000, "曲：虎二"),
                    LyricLine(3_000, "原唱：虎二"),
                    LyricLine(18_000, "第一句实际演唱歌词"),
                ),
            ),
        )

        assertEquals(16_000L, karaokeIntroTargetMillis(lyrics))
    }

    @Test
    fun karaokeIntroTargetKeepsShortIntroActionVisible() {
        val lyrics = LyricsUiState.Content(
            LyricsDocument(
                listOf(
                    lyricLine(218, 265, "制作：DJ 阿卓"),
                    lyricLine(315, 387, "统筹：小西/叶杨朔"),
                    lyricLine(598, 4_278, "我明明还是会突然想起你"),
                ),
            ),
        )

        assertEquals(598L, karaokeIntroTargetMillis(lyrics))
    }

    @Test
    fun player_compactKaraokeRecording() {
        setPlayerContent(
            initialState = screenshotState().copy(
                playback = screenshotState().playback.copy(positionMillis = 10_000),
                karaoke = KaraokeSessionState(
                    enabled = true,
                    continuousRecordingArmed = true,
                    status = KaraokeSessionStatus.Recording(
                        KaraokeProjectId("project"),
                        elapsedMillis = 102_000,
                        hasOfficialAccompaniment = true,
                    ),
                    availableSourceModes = setOf(KaraokeSourceMode.Accompaniment, KaraokeSourceMode.Original),
                    selectedSourceMode = KaraokeSourceMode.Accompaniment,
                ),
            ),
        )
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Finish and save").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Skip intro").assertIsDisplayed()
        captureScreenRoboImage(
            filePath = "src/test/screenshots/Player/PlayerCompact_karaoke_recording.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun player_karaokeRecordingPastIntroHidesSkipAction() {
        setPlayerContent(
            initialState = screenshotState().copy(
                playback = screenshotState().playback.copy(positionMillis = 40_000),
                karaoke = KaraokeSessionState(
                    enabled = true,
                    continuousRecordingArmed = true,
                    status = KaraokeSessionStatus.Recording(
                        KaraokeProjectId("project"),
                        elapsedMillis = 40_000,
                        hasOfficialAccompaniment = true,
                    ),
                    availableSourceModes = setOf(KaraokeSourceMode.Accompaniment, KaraokeSourceMode.Original),
                    selectedSourceMode = KaraokeSourceMode.Accompaniment,
                ),
            ),
        )

        composeRule.onNodeWithContentDescription("Skip intro").assertDoesNotExist()
    }

    @Test
    fun tappingLyricAfterManualScrollResumesCenteredFollowing() {
        var seekPositionMillis = -1L
        setPlayerContent(initialPage = 1, onSeek = { seekPositionMillis = it })

        composeRule.onNodeWithTag("player-lyrics").performTouchInput { swipeUp() }
        composeRule.onNodeWithText("微光沿着波纹醒来").performClick()
        composeRule.waitForIdle()

        assertEquals(38_000L, seekPositionMillis)
        val lyricsBounds = composeRule.onNodeWithTag("player-lyrics").fetchSemanticsNode().boundsInRoot
        val activeBounds = composeRule.onNodeWithTag("player-active-lyric").fetchSemanticsNode().boundsInRoot
        assertEquals(lyricsBounds.center.y.toDouble(), activeBounds.center.y.toDouble(), 1.0)
    }

    @Test
    fun player_compactSpeedDialog() {
        setPlayerContent()
        composeRule.onNodeWithContentDescription("Playback speed")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "1×"))
        composeRule.onNodeWithContentDescription("Playback speed").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("1.5×").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()

        captureScreenRoboImage(
            filePath = "src/test/screenshots/Player/PlayerCompact_speed.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun primaryControlsDispatchIndependentActions() {
        var toggleCount = 0
        var nextCount = 0
        setPlayerContent(onTogglePlay = { toggleCount++ }, onNext = { nextCount++ })

        composeRule.onNodeWithContentDescription("Pause").performClick()
        composeRule.onNodeWithContentDescription("Next").performClick()

        assertEquals(1, toggleCount)
        assertEquals(1, nextCount)
    }

    @Test
    fun playbackSpeedSelectionDispatchesFromToolRow() {
        var selected: PlaybackSpeed? = null
        setPlayerContent(onPlaybackSpeedChange = { selected = it })

        composeRule.onNodeWithContentDescription("Playback speed").performClick()
        composeRule.onNodeWithText("1.5×").performClick()

        assertEquals(PlaybackSpeed.OneAndHalf, selected)
    }

    @Test
    fun currentFormatOpensBottomSheet() {
        setPlayerContent()

        composeRule.onNodeWithText("Lossless").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Current format").performClick()

        composeRule.onNodeWithText("Viper Atmos").assertIsDisplayed()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        captureScreenRoboImage(
            filePath = "src/test/screenshots/Player/PlayerCompact_format.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun onlineQualitySelectionIsScopedThroughPlayerCallback() {
        var selected: OnlinePlaybackQuality? = null
        setPlayerContent(onOnlineQualityChange = { selected = it })

        composeRule.onNodeWithContentDescription("Current format").performClick()
        composeRule.onNodeWithText("Hi-Res").performClick()

        assertEquals(OnlinePlaybackQuality.HighResolution, selected)
    }

    @Test
    fun onlineSongActionsDispatchDirectlyFromOverflow() {
        var playNextCount = 0
        var appendCount = 0
        var addToPlaylistCount = 0
        var downloadCount = 0
        var infoCount = 0
        setPlayerContent(
            onPlayNextClick = { playNextCount++ },
            onAppendToQueueClick = { appendCount++ },
            onAddToPlaylistClick = { addToPlaylistCount++ },
            onDownloadClick = { downloadCount++ },
            onSongInfoClick = { infoCount++ },
        )

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Play next").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        captureScreenRoboImage(
            filePath = "src/test/screenshots/Player/PlayerCompact_actions.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
        composeRule.onNodeWithText("Play next").performClick()
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Add to queue").performClick()
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Add to playlist").performClick()
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Download").performClick()
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Song information").performClick()

        assertEquals(1, playNextCount)
        assertEquals(1, appendCount)
        assertEquals(1, addToPlaylistCount)
        assertEquals(1, downloadCount)
        assertEquals(1, infoCount)
    }

    @Test
    fun playerActionsDoNotExposeShare() {
        var playbackSettingsClicks = 0
        setPlayerContent(onPlaybackSettingsClick = { playbackSettingsClicks++ })

        composeRule.onNodeWithContentDescription("More options").performClick()

        composeRule.onNodeWithText("Playback settings").assertIsDisplayed().performClick()
        assertEquals(1, playbackSettingsClicks)
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Lyrics settings").assertIsDisplayed()
        composeRule.onNodeWithText("Share").assertDoesNotExist()
    }

    private fun setPlayerContent(
        initialState: PlayerUiState = screenshotState(),
        onTogglePlay: () -> Unit = {},
        onNext: () -> Unit = {},
        onSeek: (Long) -> Unit = {},
        onPlaybackSpeedChange: (PlaybackSpeed) -> Unit = {},
        onOnlineQualityChange: (OnlinePlaybackQuality) -> Unit = {},
        onPlayNextClick: (() -> Unit)? = null,
        onAppendToQueueClick: (() -> Unit)? = null,
        onAddToPlaylistClick: (() -> Unit)? = null,
        onDownloadClick: (() -> Unit)? = null,
        onSongInfoClick: (() -> Unit)? = null,
        onPlaybackSettingsClick: () -> Unit = {},
        initialPage: Int = 0,
    ) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp)),
            ) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    var playerState by remember { mutableStateOf(initialState) }
                    PlayerScreen(
                        state = playerState,
                        onBack = {},
                        onTogglePlay = onTogglePlay,
                        onPrevious = {},
                        onNext = onNext,
                        onSeek = { positionMillis ->
                            playerState = playerState.copy(
                                playback = playerState.playback.copy(positionMillis = positionMillis),
                            )
                            onSeek(positionMillis)
                        },
                        onModeChange = {},
                        onPlaybackSpeedChange = onPlaybackSpeedChange,
                        onOnlineQualityChange = onOnlineQualityChange,
                        onRetryLyrics = {},
                        onSelectQueueItem = {},
                        onRemoveQueueItem = {},
                        onClearQueue = {},
                        karaokeEnabled = playerState.karaoke.enabled,
                        initialPage = initialPage,
                        onPlayNextClick = onPlayNextClick,
                        onAppendToQueueClick = onAppendToQueueClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                        onDownloadClick = onDownloadClick,
                        onSongInfoClick = onSongInfoClick,
                        onPlaybackSettingsClick = onPlaybackSettingsClick,
                        paletteSeed = PlayerPaletteSeed(
                            mediaId = "current",
                            artworkUri = "test-artwork",
                            backgroundArgb = 0xFF071315.toInt(),
                            accentArgb = 0xFFFFA45B.toInt(),
                        ),
                    )
                }
            }
        }
    }

    private companion object {
        fun screenshotState(): PlayerUiState {
            val current = song("current", "潮汐记忆", "林澈")
            val queue = listOf(current, song("next", "失重来信", "沉岛乐队"), song("third", "凌晨四点", "周遥"))
            return PlayerUiState(
                playback = PlaybackState(
                    queue = queue.map(::PlaybackItem),
                    currentIndex = 0,
                    status = PlaybackStatus.Playing,
                    positionMillis = 102_000,
                    bufferedPositionMillis = 158_000,
                    durationMillis = 248_000,
                    mode = PlaybackMode.ListLoop,
                ),
                lyrics = LyricsUiState.Content(
                    LyricsDocument(
                        listOf(
                            LyricLine(0, "夜色落进无声的海"),
                            LyricLine(38_000, "微光沿着波纹醒来"),
                            LyricLine(
                                syllables = listOf(
                                    LyricSyllable("潮汐", 92_000, 99_000, "cháo xī"),
                                    LyricSyllable("把记忆", 99_000, 107_000, "bǎ jì yì"),
                                    LyricSyllable("推回岸边", 107_000, 116_000, "tuī huí àn biān"),
                                ),
                                translation = "飞的宝贝 请接受我的心意 我是你的专属",
                            ),
                            LyricLine(126_000, "我们在微光里重逢"),
                        ),
                    ),
                ),
            )
        }

        fun song(hash: String, title: String, artist: String) = OnlineSong(
            hash = hash,
            title = title,
            artist = artist,
            coverUrl = null,
            albumId = "night-signal",
            albumAudioId = "audio-$hash",
            durationMillis = 248_000,
            quality = AudioQuality.Lossless,
            vip = false,
        )

        fun lyricLine(startMillis: Long, endMillis: Long, text: String) = LyricLine(
            syllables = listOf(LyricSyllable(text, startMillis, endMillis)),
        )
    }
}
