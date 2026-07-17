# Build and Verification Status

## Completed

- Uploaded archive inspected and extracted.
- Rust/Bevy architecture classified as a web reference, not a native runtime.
- Pure Kotlin game core implemented.
- Deterministic Gray Tower mission implemented.
- Five cue simulations implemented with explicit nuisance coupling.
- Fisher information stored as inverse variance (`m^-2`).
- Fusion candidate indexing defect removed.
- Strict scoring boundaries implemented.
- Native Compose landscape shell implemented.
- Direct S Pen hover, pressure, drag, eraser, and stylus-first event consumption implemented.
- No WebView and no network permission.
- Core compiled with Kotlin/JVM and self-test executed successfully.
- GitHub Actions Android API 36 toolchain configured successfully.
- Pure Kotlin unit and stress tests pass in CI.
- Android resource processing and Kotlin compilation pass in CI.
- Debug APK builds successfully and is published as a workflow artifact.

## Stress-test result

The pure Kotlin core was executed across 2,000 deterministic missions and all 25 sensor subsets that fit the compute budget. The run checked 42,773 finite fusion results, information bounds, weight normalization, scoring boundaries, budget invariants, and abstention exploit resistance. No invariant failed.

## Current build artifact

The CI-built development APK is produced from pull request branch `build/native-kotlin-bootstrap`. It is a debug-signed engineering build, not a production release.

## Not yet verified

- Installation and gameplay acceptance on the physical Samsung Galaxy Tab S10+.
- Physical-device S Pen latency, palm rejection, hover behavior, and eraser behavior.
- 60/120 Hz frame timing, battery use, and thermal stability.
- Android instrumentation and UI automation tests.
- Release signing, Play App Signing, and reproducible release bundle generation.

These items remain explicit release gates and are not claimed as complete.
