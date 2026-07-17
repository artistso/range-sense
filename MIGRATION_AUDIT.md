# Uploaded Rust/Bevy Workspace — Migration Audit

## Disposition

The uploaded `range_sense` workspace is a useful game-design reference, but it is not an acceptable base for the requested native Tab S10+ product. It targets Rust/Bevy and WASM, carries browser scaffolding, and contains build and mathematical defects. The Android project in this package ports the valid gameplay concepts into Kotlin rather than wrapping the web build.

## Major defects found in the uploaded source

1. `src/main.rs` calls `crate::app::run()` although the binary crate does not declare `mod app`; the library export should be invoked through the package crate name.
2. `src/lib.rs` uses `#[wasm_bindgen(start)]` and `web_sys` without visible imports or target guards around the function, making native compilation structurally suspect.
3. The Bevy dependency declares feature names such as `bevy_app`, `bevy_ecs`, `bevy_entity`, and `bevy_canvas` as top-level Bevy features; several are crates rather than valid `bevy` feature flags.
4. Observed Fisher information is represented as `1 / uncertainty` in portions of the simulation. For a scalar Gaussian range observation it must scale as `1 / variance`, with units of inverse-square meters.
5. The gate uses a relative threshold derived from the same observed information, reducing the decision largely to a fixed nuisance-retention cutoff and hiding absolute observability.
6. The fusion residual indexes the original selected-sensor list against a shorter weight list after candidates are skipped. This can pair weights with the wrong sensors.
7. The README presents the game as a web build, directly contradicting the native Kotlin / no-WebView requirement.
8. No Android S Pen input path, Android lifecycle, release APK path, or tablet layout exists.

## Corrective architecture

- `game-core`: deterministic, renderer-independent Kotlin simulation.
- `app`: native Compose landscape UI and direct Android stylus events.
- Strict scoring thresholds are unit-tested.
- Fisher information is stored in `m^-2` as `1/sigma^2`.
- Fusion candidates preserve sensor/weight identity.
- The game labels all sensor formulas as simulation mechanics rather than empirical sensor performance.
