# Range Sense Engineering Rules

## Product boundary

Range Sense is a native, offline, single-player Android game for the Samsung Galaxy Tab S10+.

- Kotlin and Jetpack Compose only for the application layer.
- No WebView, browser runtime, JavaScript, or network requirement.
- The sensor equations are game simulations, not claims of demonstrated field performance.
- Simulation state must remain deterministic from the mission seed.

## Architecture

- `game-core` owns missions, simulation, information gates, fusion, scoring, and saveable state.
- `app` owns Android lifecycle, Compose UI, S Pen input, and presentation.
- Android or Compose types must not enter `game-core`.
- Rendering must not become the source of truth for gameplay state.

## Mathematical requirements

- Scalar Gaussian information is represented as inverse variance with units `m^-2`.
- Strict score thresholds remain strict: equality at 0.5, 2, 5, or 15 meters falls into the next lower tier.
- Every fusion weight must remain attached to its originating sensor.
- Invalid, non-finite, or non-positive variances must be rejected.
- All randomized behavior must use `DeterministicRng`; do not use default random generators.

## Verification

Before merging:

1. Run `:game-core:test`.
2. Build `:app:assembleDebug`.
3. Keep CI green.
4. Add tests for scoring boundaries, deterministic replay, compute budgets, and numerical invariants when those systems change.
