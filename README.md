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
- [Foundation](design/FOUNDATION.md)
- [Component system](design/COMPONENT_SYSTEM.md)
- [Validation matrix](design/VALIDATION.md)

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

Foundation 00–05, Component System 06–08, and the Validation specification are
frozen. The Android foundation now includes the Gradle build, Material3 1.4.0
theme tokens, a minimal app, and the standalone Foundation Catalog. Components
06–08 and Validation cases V-01–V-10 are not implemented or marked as passed.

## Build

JDK 17 and Android SDK 36 are required.

```bash
./gradlew :app:assembleDebug :app-resonote-catalog:assembleDebug
```

Install or run `app` for the minimal product shell, and
`app-resonote-catalog` for Light, Dark, AMOLED, and token inspection.
Frozen color specifications use `#RRGGBB`; the Catalog displays runtime colors
as `#AARRGGBB` so their alpha channel remains visible during inspection.

## License

Licensed under the [MIT License](LICENSE).
