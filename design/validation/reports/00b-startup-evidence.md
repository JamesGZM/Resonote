# 00B Startup Identity Evidence

- Date: 2026-08-11
- Build: `0.1.0-debug` (`com.resonote.app.debug`)
- Device: Android Emulator `MoeKoe_API_32`, API 32, `1080 × 2400`, 420 dpi
- Environment: System Light, Window / Transition / Animator scale `1×`
- Launch path: Pixel Launcher app drawer tap after `am force-stop`; shell `am start` is not used because it suppresses the launcher-provided splash icon on this API.

## Evidence

- Recording: `design/validation/recordings/v-10_resonote-splash_api32_light_1x.mp4`
- Terminal frame: `design/validation/screenshots/v-10_resonote-splash_api32_light_1x_frame.png`

## Result

- Main App AVD starts from the waveform, draws the open R bowl, then completes the lower stem and leg.
- The `0.72×` Splash-only derivation remains fully visible inside the system circular mask.
- Splash and first Compose frame both use Light background `#FFFBFF`; no mismatched white or dark flash was observed.
- This is partial V-01 / V-10 evidence only. Dark, Motion Scale `0× / 10×`, API 26 / 30 static fallback, latest API, warm start and hot start remain Not Run.
