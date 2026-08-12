# Resonote Design System

Resonote is an open-source music product design system built on the Material 3
Baseline and aligned with `androidx.compose.material3:material3:1.4.0`.

Markdown is the normative source for tokens, values, behavior, accessibility,
and component contracts. PNG and SVG assets are supporting visual evidence and
must not be used as the only implementation source.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Dependency matrix](docs/DEPENDENCY_MATRIX.md)
- [ADR-0001: Now in Android reference baseline](docs/adr/0001-now-in-android-reference-baseline.md)
- [ADR-0002: MoeKoe functional reference](docs/adr/0002-moekoe-functional-reference.md)
- [Design system plan](design/DESIGN_SYSTEM_PLAN.md)
- [Product requirements and page design contracts](design/PRODUCT_REQUIREMENTS.md)
- [Home implementation baseline](design/HOME_IMPLEMENTATION_BASELINE.md)
- [Foundation](design/FOUNDATION.md)
- [Component system](design/COMPONENT_SYSTEM.md)
- [Validation matrix](design/VALIDATION.md)

Before creating or revising a product page, read the page contract in
`design/PRODUCT_REQUIREMENTS.md` and the Compact canvas, layout, and Insets
rules in `design/FOUNDATION.md` section 03D. Markdown is normative; approved
PNG/SVG files are supporting review evidence only.

Before reusing the frozen Music Item, Playlist Item, Mini Player, or Bottom
Navigation, read `design/COMPONENT_SYSTEM.md` sections 08B, 08C, 09A, and 09B,
including the frozen-component quick index. Those sections define measurement,
single-line truncation, trailing-space reservation, loading/missing artwork,
spacing, actions, and accessibility; screenshots must not be reinterpreted.

Before implementing Home, also read `design/HOME_IMPLEMENTATION_BASELINE.md`.
Its three screenshots are scroll states of one page and freeze structure and
density only; the component Markdown and 08/09 baselines remain authoritative
for pixel-level behavior.

## Scope

- Foundation: brand, color, typography, shape, elevation, layout, icons,
  artwork, motion, interaction states, and accessibility.
- Components: Material 3 core components, adaptive navigation, feedback, and
  Resonote music browsing extensions.
- Validation: themes, font scaling, window size classes, locale, input,
  interaction states, motion scale, and content extremes.

Player-specific layouts and playback components are retained as approved
product references but remain outside the current Foundation and Component
System specification.

## Status

Foundation 00–05, Component System 06–09, and the Validation specification are
frozen. The Android foundation now includes the Gradle build, Material3 1.4.0
theme tokens, a minimal app, and the standalone Catalog. Component System 06A
Buttons & Actions and 06B-1 Text Field are implemented with behavior tests and
Roborazzi baselines; 06B-2–08 remain to be implemented. Validation V-04 and V-05
have partial automated coverage, but V-01–V-10 are not marked as passed.

## Build

JDK 17 and Android SDK 36 are required.

```bash
./gradlew :app:assembleDebug :app-resonote-catalog:assembleDebug
./gradlew :core:designsystem:testDebugUnitTest verifyRoborazziDebug
```

Install or run `app` for the minimal product shell, and
`app-resonote-catalog` for Light, Dark, AMOLED, and token inspection.
Frozen color specifications use `#RRGGBB`; the Catalog displays runtime colors
as `#AARRGGBB` so their alpha channel remains visible during inspection.
The component screenshot baselines cover Light, Dark, AMOLED, font scaling, RTL,
and representative window classes under `core/designsystem/src/test/screenshots/`.

## License

Licensed under the [MIT License](LICENSE).
