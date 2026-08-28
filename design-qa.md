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

# Karaoke Mix Editor Design QA

- Source visual truth: `/Users/gongziming/Android/projects/Resonote/feature/settings/impl/src/test/screenshots/Settings/SettingsCompact_equalizer.png`
- Implementation screenshots: `/Users/gongziming/Android/projects/Resonote/feature/local/impl/src/test/screenshots/LocalMusic/LocalMusicCompact_karaoke_mix_editor.png` and `/Users/gongziming/Android/projects/Resonote/feature/local/impl/src/test/screenshots/LocalMusic/LocalMusicCompact_karaoke_mix_editor_equalizer.png`
- Full-view comparison: `/Users/gongziming/Android/projects/Resonote/feature/local/impl/build/outputs/roborazzi/KaraokeMix_equalizer-baseline_comparison.png`
- Viewport: 390 × 844 dp, Simplified Chinese, light theme
- Source and implementation pixels: 510 × 1105 each at the same Roborazzi density; no scaling or cropping was used
- States: initial mix editor and scrolled custom-EQ state at low +1 dB / mid -2 dB / high +4 dB

## Findings

No actionable P0, P1, or P2 differences remain.

- Fonts and typography: the compact section labels, semibold control titles, small supporting ranges, and right-aligned values reproduce the equalizer hierarchy using the existing Resonote typography.
- Spacing and layout rhythm: project identity, level balance, EQ response, preset tabs, and three bands form a single scrollable sequence. Dividers, preset-card grids, and the oversized bottom action surface were removed; 20 dp horizontal control insets and compact fixed control heights match the baseline rhythm.
- Colors and visual tokens: selected tabs, curves, active tracks, thumbs, and persistent actions use Resonote semantic theme colors. No new hard-coded palette was introduced.
- Image and asset fidelity: the editor retains the existing project artwork treatment. The equalizer design baseline contains no additional raster assets; curves and controls are native interactive Compose elements.
- Copy and content: all visible labels remain localized. Frequency ranges were added in English and Simplified Chinese to match the equalizer information hierarchy.
- Interaction and accessibility: level and EQ sliders expose discrete `SetProgress` semantics; preset changes update all bands; manual adjustment selects Custom; chart and row values share the same state. Preview is attached to the project row, while save remains persistently reachable as the toolbar confirmation action.

## Comparison History

- Pass 1: replaced MD3 sliders, divider-separated sections, two-column preset cards, and the custom-EQ container with the approved curve, horizontal tabs, thin stepped sliders, zero detents, and borderless section structure. The native-density three-panel comparison shows no actionable P0/P1/P2 drift from the selected design language.
- Pass 2: moved preview beside the draft project identity, converted save to the toolbar confirmation icon, and removed the full-width bottom action bar. The revised captures recover vertical space and preserve both actions without overlap.

## Focused Region Evidence

A separate crop was not required: the 1578 × 1145 three-panel comparison keeps the baseline slider geometry, K-song balance controls, response chart, preset selection, frequency labels, and custom band controls readable at native implementation resolution.

## Follow-up Polish

- P3: verify the project-row preview action, toolbar save action, and long English preset row on a narrow physical device with large system font scaling.

final result: passed

---

# Equalizer Design QA

- Source visual truth: `/Users/gongziming/.codex/generated_images/01a046c2-f42f-7722-a69e-106be785c291/exec-5827d150-f9ed-4ef8-8796-0d1efbae46b0.png`
- Implementation screenshot: `/Users/gongziming/Android/projects/Resonote/feature/settings/impl/src/test/screenshots/Settings/SettingsCompact_equalizer.png`
- MiniPlayer-inset screenshot: `/Users/gongziming/Android/projects/Resonote/feature/settings/impl/src/test/screenshots/Settings/SettingsCompact_equalizer-mini-player-inset.png`
- Full-view comparison: `/Users/gongziming/Android/projects/Resonote/feature/settings/impl/build/outputs/roborazzi/SettingsCompact_equalizer_comparison.png`
- Viewport: 390 × 844 dp, light theme, custom preset at low +6 dB / mid 0 dB / high -1 dB
- Source pixels: 852 × 1846; implementation pixels: 510 × 1105 at the Roborazzi test density
- Normalization: source resized to 510 × 1105 and placed beside the implementation without cropping

## Findings

No actionable P0, P1, or P2 differences remain.

- Fonts and typography: the implementation uses the existing Resonote Material typography and locale-aware strings. Chart values, band titles, ranges, and row values were reduced one type step to create the requested finer hierarchy.
- Spacing and layout rhythm: the 132 dp chart starts 8 dp below the toolbar content edge. The chart, preset tabs, and band controls now live in the same `LazyColumn` pattern as the other settings detail screens. The default compact state still shows all controls, while smaller usable heights and the MiniPlayer inset can scroll naturally.
- Colors and visual tokens: background, accent, selected tab, curve fill, and control states use Resonote semantic theme colors rather than hard-coded mock colors.
- Image and asset fidelity: the target contains no raster assets. The curve and interactive control geometry are correctly implemented as native Compose UI.
- Copy and content: labels come from the existing localized resources. The English screenshot is longer than the Chinese target, so the preset row scrolls horizontally as designed.
- Interaction and accessibility: preset selection works; each custom slider exposes discrete `SetProgress` semantics; the curve, chart values, and row values share the same live state.

## Comparison History

- Pass 1: no P0/P1/P2 findings. The smaller response chart and tighter band spacing are intentional changes requested after the source mock was generated. No visual correction was required after the normalized comparison.
- Pass 2: removed every section divider, reduced the response chart to 132 dp, stepped down supporting typography, and refined the custom slider geometry. No actionable P0/P1/P2 differences remain.
- Pass 3: added 8 dp of breathing room between the toolbar and chart labels. Repeated the compact render with the production 120 dp MiniPlayer content inset; the high-band control remains fully visible above the reserved region.
- Pass 4: replaced the fixed-height editor column with a single scrollable list and preserved the production 120 dp MiniPlayer bottom content padding. Both normal and MiniPlayer-inset captures keep the high-band control reachable without overlap.
- Pass 5: reduced each band item from 160 dp to 136 dp after the scroll conversion, removing excess vertical whitespace while preserving slider labels and touch geometry.

## Focused Region Evidence

A separate crop was not required: the normalized 1044 × 1105 side-by-side full-view comparison keeps the chart labels, slider ticks, zero detents, thumbs, and value labels readable at native implementation resolution.

## Follow-up Polish

- P3: validate the same fixed layout on a physical device with unusually large system font scaling before treating large-text behavior as frozen.

final result: passed
