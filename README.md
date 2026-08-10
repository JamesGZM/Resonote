# Resonote Design System

Resonote is an open-source music product design system built on the Material 3
Baseline and aligned with `androidx.compose.material3:material3:1.4.0`.

Markdown is the normative source for tokens, values, behavior, accessibility,
and component contracts. PNG and SVG assets are supporting visual evidence and
must not be used as the only implementation source.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
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
frozen. Android implementation evidence for the Validation matrix will be
added when the app implementation is available.

## License

Licensed under the [MIT License](LICENSE).
