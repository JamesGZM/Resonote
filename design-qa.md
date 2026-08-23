# Recognition results design QA

- Source: `/Users/gongziming/.tmp/codex-clipboard-e45c34b9-3108-4afe-bad2-55e901f14ec3.jpg`
- Implementation: `/Users/gongziming/Android/projects/Resonote/feature/recognition/impl/src/test/screenshots/Recognition/RecognitionCompact_matches.png`
- Side-by-side evidence: `/Users/gongziming/Android/projects/Resonote/build/design-qa/recognition-results-comparison.png`
- Viewport: 390 x 844 dp, light theme

## Findings

No actionable P0, P1, or P2 differences remain.

- Layout: the result uses a fixed-height horizontal pager with a 56 dp viewport inset, 12 dp page spacing, and a visible adjacent card. The indicator now sits 8 dp below the card instead of being pushed toward the bottom edge.
- Immersive shell: the result reuses the accepted recognition gradient, title block, and 40 dp translucent back control. The PCM-responsive acoustic rings and center orb remain exclusive to the listening state. There is no separate white result background or conventional app-bar strip.
- Card: real recognition artwork fills the image region; confidence remains an image overlay. Title, artist, duration, quality, VIP, filled play action, add-to-playlist action, and search action use Resonote components and Material theme roles.
- Motion and interaction: cards swipe horizontally, adjacent cards scale and fade slightly, the active page is represented by the existing Resonote pill indicator pattern, and tests verify navigation between results.
- Retry: the bottom action follows the reference's low-emphasis sentence plus text action and returns to the ready-to-recognize state.
- Localization decisions: the previously rejected result introduction header remains removed. The reference's add button is wired to Resonote's existing authenticated playlist-picker flow, and search is wired to Resonote's search destination. The reference's pale page background and toolbar-title placement are intentionally replaced by the already accepted immersive recognition shell.
- Screenshot fixture: artwork is null by design, so the established Resonote artwork fallback appears in test evidence. Production cards use each returned song's real `coverUrl` through `ResonoteRemoteArtwork`.

## Verification

- `:feature:recognition:impl:recordRoborazziDebug`: passed
- `:app:compileDebugKotlin`: passed
- `:feature:recognition:impl:spotlessCheck`: passed
- `git diff --check`: passed

final result: passed

---

# Home recommendation cards design QA

- Source visual truth: `/Users/gongziming/.tmp/codex-clipboard-67460c30-1cc2-46f1-bccd-014af5617b3c.png`
- Implementation screenshot: `feature/home/impl/src/test/screenshots/Home/HomeCompact_top_zh.png`
- Combined comparison: `/Users/gongziming/.codex/visualizations/2026/08/15/01a0031f-983c-71b2-8791-68197e71a45c/home-audit/reference-vs-corrected-golden.png`
- Viewport: Compact `390 × 844dp`, Light theme, `zh-CN` locale matching the source visual
- Source pixels: `1774 × 887`; component-only concept image with no device density contract
- Implementation pixels: `510 × 1105`; Roborazzi `390 × 844dp`, `420dpi`
- Density normalization: both recommendation-card regions were cropped and normalized to the same comparison height; surrounding app chrome was excluded from fidelity judgments
- State: populated home, first scroll position, radio result present in state but intentionally not rendered

## Full-view comparison evidence

The implementation keeps the approved page order and replaces the former large radio card plus two shortcuts with one equal three-card row. The shorter recommendation region moves Daily recommendations upward without changing the order or hierarchy of later home sections. The recorded top, middle, and bottom scroll states remain reachable; only the top and middle Goldens changed.

## Focused region comparison evidence

The combined comparison verifies the recommendation-card region at readable scale. All three cards use the same strict `1:1` ratio, radius, spacing, typography hierarchy, and color emphasis. The radio card contains fixed product copy plus a Compact Overlay Filled Icon Button with a `48dp` target, `28dp` visible container, and `16dp` glyph; the visible container is inset `8dp` from the card's right and bottom edges. No radio song title, artist, or artwork is rendered. The waveform, five-column ranking, and concentric-disc artwork are automatically traced from the approved source visual into retained SVG sources and Android VectorDrawables, then tinted through the active semantic content color.

## Required fidelity surfaces

- Fonts and typography: Android system sans through `MaterialTheme.typography`; bold `titleSmall` and regular-weight `labelSmall` preserve the hierarchy at the reduced sizes requested during visual review. Chinese strings preserve the approved exact copy.
- Spacing and layout rhythm: 16dp page margin, 8dp card gaps, 12dp top/horizontal and 8dp bottom card padding, strict `1:1` card ratio, equal row sizing, 16dp card radius, and no static shadow.
- Colors and visual tokens: the three cards remain based on `primary/onPrimary`, `secondary/onSecondary`, and `tertiary/onTertiary`; each background adds only a subtle light-to-dark gradient derived from its semantic container color so Light, Dark, AMOLED, and Dynamic Color retain their role mapping.
- Image quality and asset fidelity: source-traced SVG/VectorDrawable assets preserve the approved line shapes instead of approximating them with unrelated stock icons. Their Compose slots use the visually reviewed proportions: `54 × 48dp`, `64 × 43dp`, and `47 × 50dp`.
- Copy and content: radio uses fixed functional copy and never exposes dynamic song or artist metadata. Ranking and playlist copy matches the approved Chinese concept through localized resources.

## Findings

No actionable P0, P1, or P2 differences remain.

Accepted implementation adaptation:

- The source's fixed raster colors are represented by gradients derived from active theme semantic colors instead of hard-coded RGB values.

## Comparison history

1. Initial verification compared the previous Golden against the new implementation and correctly reported the approved structural change in the top recommendation region.
2. User true-device review identified three P1 fidelity issues: non-square cards, block-like decorative icons, and an oversized visible radio play circle.
3. A second source-to-Golden review found that the first correction still used undersized, semantically different stock icons. That review was incorrectly marked passed and was reopened.
4. The cards keep the strict `1:1` ratio and the artwork uses the approved source shapes at measured per-card proportions.
5. User review requested smaller title/supporting copy and a standards-based playback control. Typography moved to bold `titleSmall` plus regular `labelSmall`.
6. The first standards-based button pass used the regular `48/40/24dp` Icon Button stack; user review found its visible container about one-third too large for this cover overlay.
7. A dedicated Compact Overlay variant now preserves the `48dp` target while using a `28dp` visible container, `16dp` glyph, and `8dp` right/bottom inset.
8. User review identified slight stair-stepping on the source-extracted PNG artwork; all three assets are now automatically traced SVG sources with Android VectorDrawable runtime assets, eliminating bitmap scaling edges without manually redrawing the forms.
9. User review found the ranking and playlist artwork slightly too large; their slots were reduced by roughly 10% while the radio artwork and all card geometry remained unchanged.
10. The corrected Chinese top and English scroll-state screenshots were inspected and recorded. Later sections retained their content order and layout; no unrelated bottom-state change was recorded.
11. `:feature:home:impl:verifyRoborazziDebug` passed against the corrected baseline.

## Residual verification gaps

- Real-device TalkBack focus order, pressed feedback, Dark/AMOLED appearance, Dynamic Color, and 200% font scaling were not captured in this pass.

final result: passed

---

# My Profile Header Design QA

- Source visual truth: `/Users/gongziming/.codex/generated_images/01a02f2e-5add-7a63-828c-012da25be585/exec-39c01a84-3db2-4cb3-ac8a-505de9c37ce9.png`
- Implementation screenshot: `/Users/gongziming/Android/projects/Resonote/feature/library/impl/src/test/screenshots/My/MyCompact_profile.png`
- Full-view comparison: `/Users/gongziming/Android/projects/Resonote/build/design-qa/my-profile-comparison.png`
- Focused header comparison: `/Users/gongziming/Android/projects/Resonote/build/design-qa/my-profile-header-comparison.png`
- Viewport: 390 x 844 dp, authenticated profile, light theme
- Source pixels: 852 x 1846
- Implementation pixels: 510 x 1105
- Normalization: both inputs were Lanczos-scaled to 390 x 844 and placed side by side at 1:1 comparison size

## Findings

No actionable P0, P1, or P2 differences remain.

- Fonts and typography: the implementation uses Resonote Material typography and optical weights. The nickname uses a 20 sp `titleLarge` treatment matching the selected visual; ID, signature, statistic values, labels, and truncation behavior preserve the product type hierarchy.
- Spacing and layout rhythm: the normalized comparison aligns the 96 dp avatar, nickname/ID/signature baselines, four full-width statistic columns, four quick-entry columns, playlist header, filters, and artwork rows. The compact check-in control opts out of Material's default 48 dp layout minimum so its invisible touch sizing no longer stretches the three-line identity block.
- Colors and visual tokens: the implementation uses existing semantic primary, primary-container, surface-container, and on-surface colors. The avatar ring and statistic separators match the source emphasis without introducing a new palette.
- Image quality and asset fidelity: production avatars continue to use `AsyncImage` with a circular crop. The screenshot fixture intentionally uses the initial fallback. Existing Material icons and playlist artwork are retained at native quality.
- Copy and content: nickname, VIP status, ID, check-in, signature, follows, fans, listening time, music age, quick actions, playlist filters, and playlist metadata match the selected state. Music age is backed by the real `rtime` registration timestamp instead of fixture-only text.

## Interaction Evidence

- Daily VIP check-in remains clickable beside the user ID.
- Settings remains available and is vertically aligned with the avatar region at the right edge.
- Four profile statistics and four quick entries occupy measured full-page columns.
- Playlist group switching and primary content alignment remain covered by Compose tests.

## Comparison History

1. Initial implementation: blocked by a P1 settings-button placement error and a P2 overly compact vertical rhythm.
2. First fix: placed settings at the top right through a reliable parent row, but the review was incorrectly marked passed while the identity block and statistics were still visibly misaligned.
3. User review reopened QA. Measurement found nickname at 52 dp versus 68 dp in the source, the compact check-in surface reserving a 48 dp minimum, outer statistic columns inset by roughly 15 dp, and downstream sections carrying accumulated vertical error.
4. The identity block now uses explicit line spacing, the avatar and settings control use measured vertical offsets, and the statistic and quick-entry rows use full-page four-column geometry.
5. The final pass aligns quick-entry labels, playlist artwork offset and aspect ratio, and the nickname width. The normalized full and focused comparisons show no remaining P0/P1/P2 mismatch.

## Follow-up Polish

No blocking follow-up. Real-device review may confirm the status-bar inset and real avatar crop with live account data.

final result: passed
