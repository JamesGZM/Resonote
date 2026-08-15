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
