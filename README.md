# RANGE SENSE — Android Native Vertical Slice

Native Kotlin conversion of the uploaded Rust/Bevy web prototype. The Rust project is treated as a design reference, not as a runtime dependency.

## Current playable slice

- Single-player **Gray Tower** mission
- Deterministic mission generation from a seed
- Five passive-ranging cue families
- Nuisance-aware conditional-information gate
- Inverse-variance fusion with residual diagnostics
- Strict CIDAR-inspired scoring thresholds
- Estimate, uncertainty interval, submission, and abstention
- Landscape tablet UI
- S Pen hover, pressure, eraser, and drag handling in the tactical viewport
- No WebView and no network permission

## Scientific boundary

This is a game simulation. Its cue models are dimensionally consistent and explicitly labeled, but they are not validated field sensor models and must never be represented as demonstrated real-world ranging performance.

## Project structure

```text
app/        Android and Jetpack Compose UI
game-core/  Pure Kotlin deterministic simulation and scoring
tools/      Standalone core self-test
```

## Build

Open the root folder in a current Android Studio installation with Android SDK 36 installed. The project is configured for AGP 9.3.0, Gradle 9.5.0, Kotlin 2.4.0, and the 2026.06 Compose BOM.

GitHub Actions installs the pinned Gradle 9.5.0 distribution and Android API 36 toolchain, runs the pure-Kotlin tests, builds `app-debug.apk`, and publishes the APK and test reports as workflow artifacts. The pipeline does not depend on a committed Gradle wrapper JAR.

## Verified

- Pure Kotlin deterministic simulation and stress tests pass.
- Android resources compile and merge successfully in GitHub Actions.
- The debug APK packages successfully with D8.
- The generated APK remains a development build; physical Galaxy Tab S10+ stylus latency, frame timing, thermal behavior, and gameplay acceptance testing are still required.
