# Recognition screen design QA

- Source visual truth: `/Users/gongziming/.tmp/codex-clipboard-88901f82-efd3-494f-955b-aaa50fb6cae7.png`
- Audible implementation screenshot: `/Users/gongziming/Android/projects/Resonote/feature/recognition/impl/src/test/screenshots/Recognition/RecognitionCompact_recording.png`
- Silent implementation screenshot: `/Users/gongziming/Android/projects/Resonote/feature/recognition/impl/src/test/screenshots/Recognition/RecognitionCompact_recording_silent.png`
- Full-view comparison: `/Users/gongziming/Android/projects/Resonote/build/design-qa/recognition-idle-comparison.png`
- Waveform focused comparison: `/Users/gongziming/Android/projects/Resonote/build/design-qa/recognition-waveform-source-signed.png`
- Action focused comparison: `/Users/gongziming/Android/projects/Resonote/build/design-qa/recognition-action-comparison.png`
- Viewport: 390 x 844 dp, light theme, idle and recording states
- Source pixels: 853 x 1844; normalized to 510 x 1105 for comparison
- Implementation pixels: 510 x 1105; CSS-equivalent size 390 x 844 dp; capture density 1.3077

## Findings

No actionable P0, P1, or P2 differences remain.

- Fonts and typography: both views use a clean system sans hierarchy. The implementation intentionally uses the project Material 3 typography tokens; title size, weight, wrapping, and supporting-copy rhythm now match the source closely.
- Spacing and layout rhythm: the translucent 40 dp back control, 24 dp left alignment, upper title block, 90%-of-width record field, and compact bottom action follow the same composition. The complete ripple field remains framed and visually centered.
- Colors and visual tokens: the implementation maps the source rose, blush, and warm-peach field to Resonote's `primary`, `primaryContainer`, and `tertiaryContainer` tokens. The result is slightly flatter than the generated source texture but remains within the accepted project language.
- Image and visual fidelity: the record/acoustic visualization is implemented as a state-aware Compose canvas rather than a static raster. Live PCM RMS is treated as an energy source rather than direct geometry: asymmetric attack/release smoothing feeds a damped center-driver simulation and independently travelling wave fronts whose radius advances continuously with frame time and whose opacity, stroke, and glow decay over their lifetime. A noise gate prevents synthetic motion in silence. The center behaves as a bounded speaker diaphragm: audible transients provide its impulse, spring and damping provide its rebound, and emitted pressure rings begin at its edge.
- Copy and content: `听歌识曲`, `让旋律靠近一点`, and `开始识别` match the selected source. Permission, recording, recognition, error, and result states retain the existing product contracts. `开始识别`, `停止识别`, permission recovery, and `重新听一次` now share the same circular-action-plus-label component within the immersive status screen.

The current reviewed direction replaces the source waveform curve with full-circle PCM-driven ripples. `RecognitionCompact_recording.png` is the audible-state evidence and `RecognitionCompact_recording_silent.png` verifies that silence returns the field to calm concentric rings.

## Comparison history

1. Initial implementation: P2 findings were an oversized and low title, record center positioned too low, insufficient pulse variation, and a primary action about 36 dp too low.
2. Fixes: moved the title block to 112 dp and changed it to `headlineMedium`; moved the record center to 48% screen height; increased pulse modulation; reduced the action to 56 dp and moved it upward; constrained the recording progress indicator to 180 dp.
3. Post-fix evidence: the normalized side-by-side image at `build/design-qa/recognition-idle-comparison.png` shows aligned hierarchy and proportions. The recording screenshot confirms the progress, stop action, and automatic-stop copy no longer overlap.
4. Live-field refinement: the visualization adopted periodic harmonic fields across every ring, driven by normalized live PCM amplitude. Periodic fields removed the visible path-closing seam.
5. Current user-review iteration: P1 findings were an oversized ring field, missing ECG waveform, an undefined center disc, and inconsistent retry/permission controls. The field was reduced from 98% to 90% of screen width; a distinct multi-harmonic waveform and path-bound mist were restored; the center became a bounded gradient orb; and immersive status actions now reuse `RecognitionAction`. Updated full and focused comparisons show those mismatches resolved.
6. Ring-continuity refinement: user review identified that the restored waveform still read as a thick overlay outside the record. The waveform now replaces a segment of the third ring from the outside, uses a 1.6 dp stroke against 1.2–2 dp neighboring grooves, and rejoins the unchanged circular path at both ends. The glow was reduced from 30/12 dp to low-opacity 10/4 dp diffusion confined to the transformed segment.
7. Voice-driven refinement: user review identified that the field still behaved like an always-on decorative animation. The recorder extracts signed PCM peak samples per frame, the ViewModel retains a 24-frame amplitude history, and the canvas applies a 0.06 noise gate. Audible recording shows the real sample contour with a line-local glow and delayed center-to-edge ripples; idle and silent recording show clean concentric grooves with no waveform or glow.
8. Source-image recalibration: the newly attached source showed that the waveform is not a separate visible center baseline. The active segment is one groove with a small number of broad lobes that rejoins the original groove. The previous full-circle baseline was removed; the recorder was reduced to 12 bins; the renderer adopted a -72° start, a 145° sweep, and a continuous rounded path. The implementation intentionally retains the smaller record field previously requested and accepted by the user.
9. Dynamic contact refinement: the source softens neighboring grooves only where the active waveform reaches them. The two outer grooves are now evaluated in 2° arcs against the live waveform radius. Within a 6 dp contact field, the crisp groove fades continuously and is replaced by a wider low-opacity segment; outside that field the original groove is restored. `RecognitionCompact_recording.png` covers active contact and `RecognitionCompact_recording_clearance.png` covers an audible waveform that remains clear of neighboring grooves.
10. Signed-shape correction: the previous iteration incorrectly took the absolute value of every sample, converting the waveform into one smooth outward bulge. The renderer now removes the per-frame mean, normalizes the remaining signed peaks, and maps them to inward/outward radial displacement with continuous cubic interpolation. The updated focused comparison shows distinct alternating lobes in the same right-side arc instead of the earlier envelope shape.
11. PCM-shape correction: selecting one absolute peak per bucket still allowed a single sample to decide the bucket's sign, so the shape could jump without representing the recorded oscillation. Each PCM frame now has its DC offset removed, then every time window emits both its positive and negative extrema in their original chronological order. The unscaled extrema drive curve shape while RMS independently drives visual energy, glow, and ripple strength. Pure analyzer tests cover silence, ordered extrema, a sine wave, and changing input volume.
12. Motion and results refinement: the live extrema are polarity-aligned, temporally filtered with separate attack and release responses, then interpolated over 90 ms on the render clock. This keeps microphone energy changes responsive without exposing PCM carrier-phase flips as visual jumps. Recognition matches now reuse `ResonoteMusicItem`, adding only confidence beside the standard duration slot. The result toolbar microphone action clears the result and returns to the ready state.
13. Full-ring refinement: the active groove now maps the microphone-driven extrema periodically across all 360 degrees instead of reserving a right-side segment. Wrapped neighbors and cubic tangents keep the 0/360-degree join continuous, while contact diffusion evaluates both inner and outer neighboring grooves around the full circumference. The redundant result introduction block was removed so matches begin directly beneath the toolbar.
14. Ripple-only refinement: user review rejected the waveform curve in favor of the existing concentric field. The curve and contact-collision renderer were removed. Ten independently smoothed rings now read delayed samples from the live RMS history, so audible energy travels from the center toward the edge while silence restores the undisturbed field.
15. Fluid-motion refinement: per-ring springs tied directly to roughly 32 ms recorder callbacks still felt mechanical. The renderer now decouples PCM input from a frame-time ripple simulation: fast attack, slow release, transient-aware emission, continuous radial travel, and lifetime-based decay create water-like propagation even when audio callback timing varies. Animation state is consumed in the Canvas draw phase. Recording duration is shown only once beneath the title; the duplicate progress bar and automatic-stop copy were removed.
16. Speaker-driver refinement: the center orb now uses a damped displacement-and-velocity model rather than scaling directly with RMS. Real PCM attacks strike the virtual diaphragm, which briefly overshoots and settles; input modulation emits pressure fronts from the diaphragm edge. Static guide rings are strongly faded in silence, while audible energy restores local rim light, driver halo, and independently travelling crests without introducing an autonomous beat.

## Implementation checklist

- [x] Match the selected full-screen immersive composition.
- [x] Reuse the accepted immersive back control.
- [x] Preserve recognition state behavior and primary callbacks.
- [x] Verify idle, recording, permission, error, and match screenshots.
- [x] Keep the visualization and colors bound to Resonote theme tokens.
- [x] Reuse one immersive status action for start, stop, permission recovery, and retry.
- [x] Inject ripple energy from live microphone PCM RMS while advancing motion on frame time.
- [x] Drive the center diaphragm with PCM transients and emit pressure waves from its edge.
- [x] Verify that silence removes dynamic ring energy and glow.
- [x] Keep listening-only acoustic visuals out of the result background.

final result: passed
