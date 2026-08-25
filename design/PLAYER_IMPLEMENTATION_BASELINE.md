# Player implementation baseline

Updated: 2026-08-24

## Scope

This baseline covers the compact portrait Full Player, synchronized lyrics, Mini Player entry points, and the authoritative playback queue. Historical mobile screenshots preserve only the cover/lyrics relationship and control hierarchy; the implementation follows the current Resonote design system, Material 3, and the project's Navigation3 api/impl module boundaries.

## Visual direction: Night Signal Deck

- The player is a quiet signal deck rather than a literal recreation of the historical dark mockup.
- Artwork is the dominant spatial anchor. A deterministic concentric signal field is used when an API response has no cover URL; real cover URLs are rendered by Coil.
- A narrow NOW PLAYING rail, asymmetric signal artwork, time ruler, and high-contrast active lyric provide the recognizable visual language.
- The current Compose implementation still takes colors directly from `MaterialTheme.colorScheme`; controls retain Material 3 state, shape, and minimum touch-target behavior in light and dark themes.

## Approved artwork palette contract (implementation pending)

This section records the user-approved behavior for the next Full Player implementation pass. It is not a claim that the current Compose implementation or its existing screenshots already satisfy the contract.

- The cover page and lyrics page share one immutable `PlayerPalette` for the current media item. Backgrounds, text, progress, controls, page indicators, overlays, and system-bar appearance consume semantic roles from that palette rather than sampling artwork independently.
- Player components expose a default semantic palette derived from the active Resonote theme. The default is also the required fallback when artwork is missing or no valid artwork palette can be prepared. Controls retain Material 3 state, shape, contrast, and minimum touch-target behavior.

- Artwork color extraction belongs to the playback presentation/data boundary, not to Player composables. The palette is keyed by the stable media identity and artwork cache identity so cover and lyrics pages cannot produce different colors for the same item.
- For a media item with artwork, palette preparation starts when that item becomes the current playback item or otherwise becomes the concrete target that can open Full Player. It must finish before Full Player navigation renders its first frame. Entering Full Player must not show the default palette and then recolor the page after extraction completes.
- The extraction source is the decoded original artwork bitmap before decorative blur, scrim, scaling, or other Player background treatment. Raw sampled colors are converted into semantic roles such as background, elevated/background variant, accent, primary content, muted content, and content on accent.
- Sampled colors are not used verbatim when they fail legibility. Palette generation may adjust tone and chroma to preserve text, icon, progress, state-layer, and system-bar contrast while retaining the artwork's recognizable color direction.
- Missing artwork, failed decode, invalid or unusable samples, cancellation, and inability to prepare before entry all resolve to the default Player palette. These cases do not block playback or Player navigation and do not expose a separate palette error state.
- The prepared palette is immutable for the current media identity. Both pager pages and every shared control use the same instance. Recomposition, page swiping, lyrics loading, and artwork display loading must not restart extraction or change the palette.
- When the current item changes while Full Player is already visible, the next item's palette must be prepared as part of the item transition and applied atomically with the visible metadata/artwork transition. A late result from the previous item must never recolor the new item.
- Palette extraction and caching are local image-processing behavior. They add no network request, analytics, telemetry, or persisted user profile data.

Required implementation evidence:

- A first-frame test or deterministic state test proves Full Player never renders an intermediate unprepared palette: entry receives either the prepared artwork palette or the completed default fallback.
- Palette tests cover missing artwork, decode failure, unusable samples, cancellation, contrast correction, and cache reuse without extra network work.
- A rapid-song-change test proves an older palette result cannot overwrite the current item; reviewed cover-page and lyrics-page screenshots prove both pages and shared controls consume the same prepared palette.

## Approved pager shell and lyrics hierarchy (implementation pending)

This section records the user-approved layout and lyrics behavior for the next Full Player implementation pass. The current Compose implementation and screenshots remain implementation evidence only until they are intentionally updated and reviewed.

- Full Player is one stable page shell. Only the middle content region is a two-page horizontal Pager: page 0 is artwork and page 1 is lyrics.
- The top bar and the complete area below the Pager are shared shell content rather than duplicated page content. Page indicator, song identity, progress, primary playback controls, and the agreed playback-tool row keep the same position and behavior on artwork and lyrics pages.
- The lyrics page always retains the same complete playback controls as the artwork page. Swiping the Pager must not hide, collapse, simplify, replace, or vertically relocate those controls.
- The shared Full Player progress control follows the current MV `VideoSeekBar` interaction and geometry: a `3dp` round-capped track inside a `48dp`-high interactive area, with played, buffered, and remaining layers. The thumb is `8dp` at rest and animates to `14dp` while dragging.
- Full Player does not inherit MV's fixed black-overlay colors. Played progress and the thumb use the prepared `PlayerPalette` accent; buffered and remaining progress use contrast-safe reduced-emphasis roles from the same palette. Both Pager pages display the identical progress control.
- The time row remains directly below the thin track, with current time at the start and total duration at the end. During scrubbing, current time previews the pending target and the visible thumb follows it; the playback seek command is committed when the gesture finishes so player progress cannot fight the user's drag.
- The visible track is anchored near the bottom of its `48dp` touch target so the time row reads as one compact progress unit; accessibility height must not become visible whitespace between track and time.
- The full `48dp` interaction area supports tap-to-position, horizontal drag, and accessibility `setProgress` even though the visible track stays thin. Duration and all progress inputs are clamped, and buffered progress cannot render behind played progress or beyond duration.
- Lyrics loading, empty, pure-music, error, and retry states replace only the lyrics content inside the Pager viewport. They do not change the shared shell geometry or interrupt playback.
- The original lyric is always the primary text layer. The active line is distinguished with the strongest palette content/accent color, larger type, and stronger weight; nearby inactive lines use smaller type and reduced contrast. The active state does not require a card, filled container, or other competing surface.
- When syllable or character timing exists, the primary lyric advances through its timed units with color emphasis. Translation and transliteration do not compete for per-character emphasis; they remain supplemental line-level text.
- Translation and transliteration are independently persisted supplemental-text switches and both default to enabled. When both values exist, translation is shown first and transliteration second; either missing or disabled value is omitted without reserving empty space. Supplemental rows remain visually subordinate to the primary lyric.
- Supplemental text uses smaller type, lighter weight, and lower contrast than the original lyric. It is shown only when real non-blank data exists and must remain visually subordinate even on the active line.
- The active line follows playback, tapping a line seeks, and user scrolling pauses automatic follow for 3.5 seconds as defined below. These interactions remain confined to the Pager viewport so horizontal page swiping and the shared controls stay available.
- Automatic follow centers the target from the actual viewport and measured line height after a frame. Active/inactive hierarchy is rendered with a visual scale rather than changing measured text size, preventing list remeasurement from fighting the scroll animation and producing vertical jitter.

Required implementation evidence:

- Cover-page and lyrics-page screenshots at the same viewport prove the shell below the Pager has identical anchors and complete controls on both pages.
- A focused progress screenshot proves played/buffered/remaining layering and resting thumb geometry; interaction tests cover tap, drag preview, release-to-seek, clamping, drag thumb expansion, and accessibility progress changes.
- Lyrics screenshots cover active word timing, original plus translation and transliteration, each independently disabled state, no supplemental data, and inactive-line hierarchy.
- Interaction tests cover horizontal page switching, lyric seeking, user-scroll follow suspension, and use of every shared primary playback control while the lyrics page is selected.

## Navigation and state ownership

- `PlayerNavKey` is a global Navigation3 destination. Opening it replaces the tabs surface, so the navigation suite and Mini Player are not duplicated.
- Tapping the Mini Player body opens Full Player. Its queue icon opens the same Modal Bottom Sheet queue directly.
- Full Player, Mini Player, media session, and Queue all read the singleton `PlaybackController.state`. There is no UI-owned shadow queue.

## Approved Mini Player hero transition (implementation pending)

- Full Player keeps the current downward `KeyboardArrowDown` action at the top start position. Its semantic action is collapse player, not generic navigate back; the historical mockup's left-facing back arrow is not restored.
- Tapping the collapse action, Android system Back, and predictive Back all request the same `PlayerNavKey` pop path. Playback, Queue, current position, selected Pager page, and the underlying tab/page state are not mutated by the transition.
- The primary shared-bounds hero key is derived from the stable current media identity and connects the Mini Player outer container to the Full Player root container. This root transition must remain available while either artwork or lyrics is the selected Pager page.
- On entry, the Mini Player artwork also participates as a secondary shared element and expands from its `56dp` slot to the Full Player artwork slot. Full Player metadata, Pager indicator, progress, playback controls, tools, top actions, and lyrics are destination content that enter with Resonote motion tokens rather than becoming independent shared elements.
- Returning while the lyrics page is selected does not force the Pager back to artwork and does not compose a hidden fake artwork solely for animation. The always-present root-container transition supplies the reverse hero; an artwork shared element participates only when a real matching artwork element is present.
- The hero key includes media identity so a late or stale item cannot animate into another song. If a matching Mini Player destination is unavailable, the media identity changes during the transition, artwork is missing from either endpoint, or shared transition infrastructure is unavailable, navigation falls back to the standard destination transition without blocking Back.
- Reduced-motion behavior removes the large spatial transform and uses the platform/Resonote reduced-motion fallback. Transition completion or cancellation cannot issue playback commands, reload artwork/lyrics, recompute `PlayerPalette`, or duplicate navigation events.
- Implementation must place both Mini Player and Full Player endpoints inside the existing app-level shared-transition scope while preserving the Mini Player overlay's current z-order, safe drawing insets, tab-bar avoidance, Snackbar avoidance, and independent play/Queue touch targets. Feature Player must not create a second app navigation host.

Required implementation evidence:

- An entry recording proves Mini Player container expansion, artwork continuity, and destination-control fade-in for the same media item; a reverse recording proves the downward collapse action returns to the same underlying page and Mini Player.
- A lyrics-page Back recording proves the root hero reverses without first switching to the cover page. Tests cover the top collapse action, system Back, predictive Back where supported, repeated taps, transition cancellation, mismatched media identity, missing Mini Player destination, and reduced motion.
- Static screenshots continue to verify the resting Mini Player and Full Player layouts; screenshots alone do not count as motion verification.

## Player behavior

- The first pager page is artwork; the second is lyrics. Horizontal swiping is the only page transition.
- Play/pause, previous, next, seek, playback mode, and Queue commands dispatch to `PlaybackController`.
- The format, playback-speed, and Queue tools use dedicated vector icons in circular `64dp` bounded interaction surfaces, so their pressed state is a circular ripple instead of a row-shaped highlight. All three render in a common `34dp` box and share rounded `2.2dp` outline strokes, comparable optical bounds, and centered visual weight; resource geometry, rather than unrelated nominal sizes, compensates for each irregular silhouette. The speed gauge is optically raised within its canvas. The tool row uses `8dp` top padding and no additional bottom padding. The resting tool row has no separate title text or overlaid badge. Playback speed selects one of six complete vector assets (`0.5×`, `0.75×`, `1×`, `1.25×`, `1.5×`, `2×`); quality remains visible in the title Tag and Queue remains a plain action icon. Content and state descriptions retain complete accessibility semantics.
- The current item's compact quality label appears as a small palette-aware tag immediately to the right of the centered song title. The per-item online override wins; otherwise the tag reflects the resolved metadata format.
- The artwork page uses a centered square cover up to `342dp` with at least `24dp` symmetric horizontal margins. Beneath it, a `24%` black layer at `0.99×`, offset downward by `5dp` and blurred by `20dp`, provides the soft directional shadow visible in the approved design. A separate artwork layer at `1.018×`, `12%` opacity, and `22dp` Gaussian blur adds only a broad, restrained color diffusion without forming a visible halo frame. The card itself retains a light Level 3 edge shadow so the two effects remain distinct. The page background uses a restrained artwork layer at `1.14×`, `60%` maximum alpha, and `36dp` blur under a contrast scrim.
- The page indicator uses `10dp` top and `17dp` bottom padding before the single `48dp` progress interaction region. This makes its visible gap to the seek track match the visible gap from the time labels to the primary playback row. The seek track is drawn near the top of that region, and the time labels begin `16dp` from the same region's top so their visible glyphs sit roughly `6dp` below the track instead of being pinned to the region's lower edge. The primary playback-control row uses `4dp` top and `8dp` bottom padding to keep it visually connected to the progress group without compressing the lower tool row. Playback mode uses the same `contentPrimary` color as Previous and Next; only its glyph communicates the selected order.
- For an online item, the format tool opens a bottom-sheet quality selector matching the seven choices in playback settings. Its selection is an override owned by that queue item for the current playback session; it does not write the global playback-quality preference. Items without an override continue to use the global preference.
- Applying a per-item quality override resolves a replacement source while preserving the current position and play/pause intent. The queue item is updated only after resolution succeeds; failure leaves the prior source and override intact. Local and cloud formats remain informational.
- Mini Player expansion and Full Player collapse use the reduced-motion-aware `spatialSlow` shared-bounds transform. Mini Player endpoint visibility uses the corresponding slow effects token, making the container's growth and contraction—not a fast page fade—the primary navigation signal.
- Full Player draws its animated artwork backdrop and palette behind both transparent system bars. While the song changes, the previous palette remains visible during preparation, a stale result cannot recolor the current song, and missing artwork falls back to the theme palette after the bounded preparation window.
- The top-right sheet directly exposes Play next, Add to queue, Add to playlist, Song information, and Lyrics settings without a nested song-actions sheet. Song information uses a Material 3 bottom sheet; its song, artist, and album rows open an auto-submitted search directly in the Songs, Artists, and Albums tabs respectively, while Back restores the same Full Player state.
- Full Player and song-action overlays do not expose a share action.
- Empty playback state has a recoverable back action.

## Lyrics behavior and data boundary

- `PlayerViewModel` loads the current song through the real `LyricsRepository` using hash and album audio ID.
- Switching songs cancels the previous lyrics request. Loading, empty, authentication, network, risk, service, and protocol states remain local to the lyrics page and never change playback.
- The active line follows playback position and centers against the lyrics viewport itself with no whole-page offset or additional vertical bias. The list reserves half of the current lyrics viewport at both content edges, so its first and last items can reach the same center anchor instead of being clamped near an edge. Compressing controls below the pager returns the released height to both the artwork and lyrics pages. Tapping a line seeks to its timestamp, immediately restores centered following after manual scrolling, and uses a full-width capsule ripple. A pending seek retains its optimistic target until the player acknowledges a nearby position or the bounded acknowledgement window expires, preventing stale controller progress from making the active lyric jump backward and forward. Off-screen active lines animate directly toward a centered offset instead of first snapping to the top of the viewport.
- User dragging pauses automatic follow for 3.5 seconds. Programmatic scrolling does not trigger the pause.
- Translation and transliteration are not fabricated because the current model exposes only one timed text field.

## Queue behavior

- The Modal Bottom Sheet supports jump, remove, clear, playback-mode cycling, and long-press drag reorder.
- Move up/down are also exposed as accessibility custom actions.
- Removing the current item selects the following item, or the new final item when the removed item was last. Removing the only item clears playback.
- Reordering preserves the selected song rather than its former numeric index.

## Regression evidence

- `PlaybackQueueTest` covers removal and reorder invariants.
- `PlayerViewModelTest` covers lyric loading, song changes, failure isolation, seeking, modes, and Queue delegation.
- `PlayerScreenScreenshotTest` records compact portrait cover and lyrics baselines under `feature/player/impl/src/test/screenshots/Player/`.
