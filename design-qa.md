# Full Player Design QA

## Comparison target

- Source visual truth:
  - `design/approved/player/player-cover-page.png`
  - `design/approved/player/player-lyrics-page.png`
- Rendered implementation:
  - `feature/player/impl/src/test/screenshots/Player/PlayerCompact_cover.png`
  - `feature/player/impl/src/test/screenshots/Player/PlayerCompact_lyrics.png`
- Full-view comparison evidence:
  - `build/design-qa/player-cover-comparison-final.png`
  - `build/design-qa/player-lyrics-comparison-final.png`
- Focused control-region evidence:
  - `build/design-qa/player-controls-comparison-final.png`
  - `build/design-qa/player-lyrics-controls-comparison-final-2.png`
- Viewport: Android phone portrait, `390 x 844 dp`, Roborazzi density `420 dpi`.
- Source pixels: cover `853 x 1844`, lyrics `852 x 1846`.
- Implementation pixels: `511 x 1107` for both states.
- Normalization: sources were aspect-fit to `511 x 1107`; implementation captures remained at native Roborazzi pixels. Side-by-side comparisons are `1022 x 1107`.
- State: playing at `1:42 / 4:08`, lossless online song, cover page and translated word-highlight lyrics page.

The source includes iOS status/home chrome and a concrete artwork asset. The Roborazzi fixture captures Android app content only and deliberately has no artwork URL. Status chrome and artwork subject are therefore excluded from pixel-level findings; production uses the live square-cropped artwork and an Android down-arrow collapse affordance as required by the locked product decisions.

## Findings

No actionable P0/P1/P2 visual differences remain.

- Fonts and typography: title, artist, active lyric, adjacent lyrics, translation, timestamps and tool labels preserve the intended hierarchy. The active lyric is intentionally larger than the source because the locked requirement makes the main lyric dominant.
- Spacing and layout rhythm: the centered top identity, cover/lyrics Pager, indicator, progress, primary controls and tool row maintain stable anchors on both pages. Persistent controls fit the compact portrait viewport without clipping.
- Colors and visual tokens: the deterministic QA palette preserves the dark teal background, orange accent and readable primary/secondary contrast. Production substitutes the frozen per-cover palette.
- Image quality and asset fidelity: production uses Coil artwork with square crop, rounded mask and blurred background copy; the fixture's missing-artwork surface is a test-state limitation, not a production substitute.
- Copy and content: dynamic song/lyric content is realistic; English Roborazzi labels correspond to localized Chinese runtime strings. Format displays the real `Lossless` value rather than a switch.
- Icons and interaction affordances: Material icons consistently cover collapse, overflow, like, transport, mode, format, speed and Queue; primary tap targets remain at least 48 dp.

## Comparison history

### Iteration 1 — blocked

- P1: the lyrics Pager painted outside its bounds, obscuring the fixed song information, progress and controls.
- P1: the thin seek bar had no width constraint, leaving only the thumb visible.
- P2: the page indicator appeared before song information instead of below it.

Fixes: constrained and clipped the Pager, reserved compact-height Pager space, supplied full-width seek geometry, and moved the indicator below song information.

### Iteration 2 — passed

- Post-fix evidence shows both Cover and Lyrics states retaining the complete identity, three-layer seek bar, transport controls and tool row.
- The control-region comparison confirms the same anchors and interaction density as the source.
- No actionable P0/P1/P2 findings remain.

## Follow-up polish

- P3: add a deterministic licensed artwork fixture later so screenshot QA can also compare artwork crop and blur subject fidelity, rather than only production behavior.
- Predictive Back timing still needs an Android 13+ device. The available `ELE-AL00` runs Android 10 and therefore cannot expose that platform behavior.

## Real-device verification

- Device: Huawei `ELE-AL00`, Android 10 (API 29), portrait `1080 x 2340 px`.
- Evidence:
  - `build/design-qa/device-resonote-launch-fixed.png`
  - `build/design-qa/device-full-player-cover.png`
  - `build/design-qa/device-full-player-lyrics.png`
  - `build/design-qa/device-after-hero-gestures.png`
  - `build/design-qa/device-miniplayer-position-fixed.png`
  - `build/design-qa/device-full-player-refined.png`
  - `build/design-qa/device-format-bottom-sheet.png`
  - `build/design-qa/device-speed-bottom-sheet.png`
  - `build/design-qa/device-lyrics-full-controls.png`
- Passed: cold launch, live-cover palette extraction, MiniPlayer expansion, down-arrow collapse, Cover/Lyrics Pager, persistent controls on Lyrics, and system Back from Lyrics directly to MiniPlayer.
- Follow-up verification fixed the MiniPlayer animation host so its bottom alignment is applied by a full-screen `Box`; the MiniPlayer now remains directly above the 64 dp tab bar instead of defaulting to the top of the screen.
- Full Player draws its palette background behind the transparent status bar while reserving status/navigation insets from the Pager budget. The centered title, tool labels and complete control shell remain visible on the Android 10 compact-height device.
- Current format and playback speed both open as bottom sheets. The format sheet remains read-only and reports the actual playback format.
- Word highlighting now interpolates through Unicode code points over the playback position sampling interval, retaining translation/transliteration, alignment preferences and the 3.5 second manual-scroll follow pause.
- A cold-launch crash found during this pass was fixed by converting Coil hardware bitmaps to software bitmaps before AndroidX Palette pixel access; palette conversion/generation failures now safely fall back instead of escaping the background task.
- Recording artifact unavailable: this Huawei build does not provide the Android `screenrecord` binary. Motion was exercised on-device and the endpoint states were captured, but no video file could be exported from this device.

## Final result

final result: passed
