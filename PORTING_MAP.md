# Rust/Bevy to Kotlin/Android Porting Map

| Uploaded Rust source | Native Kotlin destination | Status |
|---|---|---|
| `src/ecs/components.rs` | `game-core/.../Models.kt` | Ported and simplified into immutable domain models |
| `src/ecs/resources.rs` | `GameState` in `Models.kt` | Ported as serializable renderer-independent state boundary |
| `src/ecs/systems.rs` | `GameEngine.kt`, `SensorSimulation.kt` | Ported into deterministic reducer and pure functions |
| `src/sensors/gate.rs` | `FisherGate.kt` | Ported; corrected information units and hard-failure handling |
| `src/sensors/fusion.rs` | `SensorFusion.kt` | Ported; corrected candidate/weight alignment and covariance inflation |
| `src/game/scoring.rs` | `Scoring.kt` | Ported; strict threshold boundary behavior tested |
| `src/game/state_machine.rs` | `GameEngine.kt` | Ported to `BRIEFING → PLAYING → DEBRIEF` reducer |
| `src/rendering/*` | `RangeSenseApp.kt` | Replaced with native Compose tactical viewport |
| `src/ui/*` | `RangeSenseApp.kt` | Replaced with tablet landscape UI and direct MotionEvent stylus handling |
| Browser/WASM files | None | Intentionally discarded |

## Deliberate non-ports

The uploaded medals, ghost replay, audio, save system, and large canvas renderer are not copied blindly. They will be reintroduced after the first native mission is profiled on the physical tablet. This prevents the web architecture from defining the Android runtime.
