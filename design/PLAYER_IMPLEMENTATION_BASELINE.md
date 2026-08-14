# Player implementation baseline

Updated: 2026-08-13

## Scope

This baseline covers the compact portrait Full Player, synchronized lyrics, Mini Player entry points, and the authoritative playback queue. Historical mobile screenshots preserve only the cover/lyrics relationship and control hierarchy; the implementation follows the current Resonote design system, Material 3, and the project's Navigation3 api/impl module boundaries.

## Visual direction: Night Signal Deck

- The player is a quiet signal deck rather than a literal recreation of the historical dark mockup.
- Artwork is the dominant spatial anchor. A deterministic concentric signal field is used when an API response has no cover URL; real cover URLs are rendered by Coil.
- A narrow NOW PLAYING rail, asymmetric signal artwork, time ruler, and high-contrast active lyric provide the recognizable visual language.
- Colors come from `MaterialTheme.colorScheme`; controls retain Material 3 state, shape, and minimum touch-target behavior in light and dark themes.

## Navigation and state ownership

- `PlayerNavKey` is a global Navigation3 destination. Opening it replaces the tabs surface, so the navigation suite and Mini Player are not duplicated.
- Tapping the Mini Player body opens Full Player. Its queue icon opens the same Modal Bottom Sheet queue directly.
- Full Player, Mini Player, media session, and Queue all read the singleton `PlaybackController.state`. There is no UI-owned shadow queue.

## Player behavior

- The first pager page is artwork; the second is lyrics. Horizontal swiping is the only page transition.
- Play/pause, previous, next, seek, playback mode, and Queue commands dispatch to `PlaybackController`.
- Share remains visible as an explicit unavailable action. It does not start an Intent or make a network call.
- Empty playback state has a recoverable back action.

## Lyrics behavior and data boundary

- `PlayerViewModel` loads the current song through the real `LyricsRepository` using hash and album audio ID.
- Switching songs cancels the previous lyrics request. Loading, empty, authentication, network, risk, service, and protocol states remain local to the lyrics page and never change playback.
- The active line follows playback position. Tapping a line seeks to its timestamp.
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
