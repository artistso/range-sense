# Verification and Validation Plan

## Purpose

This plan defines how Range Sense claims are tested, measured, recorded, and rejected when evidence is insufficient. Verification asks whether the software was built according to its requirements. Validation asks whether the resulting behavior is suitable for the explicitly stated use case.

The present use case is an offline Android game simulation. Real-world passive-ranging performance is outside the validated system boundary.

## Evidence principles

1. Evidence is tied to an exact source commit and artifact digest.
2. Acceptance criteria are written before interpreting results.
3. Negative, boundary, and failure-path tests are first-class evidence.
4. A test pass demonstrates only the property and domain actually tested.
5. Device observations are recorded with hardware, OS, build, configuration, and procedure.
6. Statistical claims include sample size, distribution assumptions, exclusions, and uncertainty.
7. A failed or inconclusive test remains visible; it is not rewritten as success.

## Verification environments

| Environment | Purpose | Authority |
|---|---|---|
| Pure Kotlin/JVM test runner | Core logic, deterministic replay, numerical and scoring invariants | Automated CI evidence |
| Android lint/build runner | Manifest, resources, Compose compilation, packaging, static checks | Automated CI evidence |
| Android emulator | Lifecycle, UI semantics, navigation, state restoration, permission and accessibility tests | Automated instrumentation evidence |
| Galaxy Tab S10+ | S Pen behavior, frame timing, battery, thermal, display and ergonomics | Physical-device engineering evidence |
| Independent build environment | Reproducibility and provenance comparison | Release evidence |

## Automated CI verification

### Core tests

Required checks:

- deterministic mission generation and sensor readings;
- different seeds produce distinct missions over a defined sample;
- compute budget cannot be exceeded;
- invalid numerical states are rejected;
- hard failures close gates;
- retained-information boundary behavior;
- no closed gate contributes to fusion;
- no valid candidates produce an untrusted non-estimate;
- contributor weights are finite, nonnegative, and sum to one within tolerance;
- fused estimate and uncertainty are finite and positive when present;
- residual and trust thresholds behave at exact boundaries;
- scoring tiers behave immediately below, at, and above every threshold;
- abstention cannot be exploited by leaving qualified cues unselected;
- canonical evidence is deterministic, complete, versioned, and mutation-sensitive.

### Stress and property-oriented tests

The stress suite shall sample many mission seeds and every sensor subset allowed by the compute budget. For each execution, assert:

- all mission-domain constraints hold;
- core outputs are finite;
- compute use remains bounded;
- observed and conditional information are nonnegative;
- retained information does not exceed observed information except for a documented floating-point tolerance;
- hard-failed sensors have zero conditional information and closed gates;
- fusion contributor identity is preserved;
- normalized weights sum to one within tolerance;
- score and abstention outputs remain within defined ranges;
- replaying the same test vector produces identical output and evidence digest.

The test report records mission count, subset count, fusion-result count, failures, seed range, runtime, and software commit.

### Coverage

JaCoCo XML and HTML reports are generated for the pure Kotlin core.

Coverage is diagnostic evidence, not proof of correctness. No coverage percentage alone can qualify a release. Uncovered branches in gating, fusion, scoring, evidence, or error handling require written disposition.

### Android static and packaging checks

CI executes Android lint and debug APK assembly. Additional required automated checks are planned for:

- absence of Internet permission;
- absence of WebView dependencies and symbols;
- no unintended exported components;
- minimum and target SDK expectations;
- debuggable status appropriate to artifact type;
- application ID and version metadata;
- native library inventory;
- permission diff from the previous release;
- APK signature and certificate inspection for release builds.

### CI evidence bundle

Each build emits:

- source commit SHA;
- clean-worktree result during evidence generation;
- SHA-256 manifest for app and core source files;
- APK SHA-256;
- dependency report;
- unit and stress reports;
- coverage reports;
- lint reports;
- diagnostic build logs.

## Emulator instrumentation plan

### Lifecycle and state

- launch from cold start;
- background and foreground during briefing, play, and debrief;
- configuration change where supported;
- process recreation with saved state expectations documented;
- repeated mission transitions;
- low-memory process termination simulation.

### UI behavior

- select and deselect each sensor;
- verify disabled controls when budget is insufficient;
- load fused estimate;
- move estimate and interval sliders to exact extrema;
- submit and abstain;
- verify evidence digest appears only after completion;
- verify all states are reachable without timing-dependent races.

### Accessibility

- TalkBack announces control purpose, state, and result;
- focus order follows operational sequence;
- touch targets meet defined minimum dimensions;
- color is not the sole indicator of gate, trust, or error state;
- text remains legible at supported font scaling;
- landscape layout remains usable with accessibility services active.

### Permission and isolation tests

- verify no runtime permission prompt appears;
- verify network-security paths are absent in the current build;
- verify no file, clipboard, camera, microphone, location, or external-storage data path exists unless separately required;
- verify exported-component behavior against the manifest.

## Galaxy Tab S10+ physical-device plan

Record:

- tablet model and hardware revision;
- Android and One UI versions;
- display refresh-rate setting;
- S Pen model and connection state;
- power mode, battery percentage, ambient temperature, and case configuration;
- APK digest and source commit;
- screen recording or event log where useful.

### S Pen acceptance tests

1. Hover enters, moves, and exits without accidental selection.
2. Pen-down drag updates only the intended interaction.
3. Pressure values remain finite and within the Android-reported domain.
4. Eraser input follows the documented branch.
5. Palm contact does not activate mission controls during pen use.
6. Rapid alternation between pen and touch does not corrupt game state.
7. Edge-of-screen strokes and interrupted gestures fail safely.
8. Repeated 10-minute use produces no accumulating input lag or state drift.

Until measured baselines exist, results are reported descriptively rather than forced into invented thresholds.

### Performance characterization

Collect frame timing using Android frame metrics or Macrobenchmark-compatible tooling.

Initial research targets, subject to measurement revision:

- no application-not-responding event;
- no crash during a 30-minute continuous session;
- 99th-percentile frame time reported separately for 60 Hz and 120 Hz modes;
- memory growth after repeated missions remains bounded and explained;
- no sustained severe thermal status attributable to the app;
- battery consumption recorded over a fixed-duration, fixed-brightness procedure.

These are characterization targets until enough data exists to establish acceptance limits.

## Release verification

### Engineering build

An engineering build may be distributed for testing when:

- CI is green on the exact commit;
- APK digest is published with the test instruction;
- artifact is clearly labeled debug/engineering;
- unresolved known issues are listed;
- no production or operational claim is made.

### Release candidate

A release candidate additionally requires:

- controlled release signing;
- dependency and license review;
- standardized SBOM;
- vulnerability scan with documented disposition;
- emulator suite pass;
- Galaxy Tab S10+ acceptance record;
- threat model and traceability review;
- release notes and rollback procedure;
- independent build comparison or nondeterminism analysis.

### Release acceptance record

The record contains:

- version and commit;
- artifact filenames, sizes, signatures, and SHA-256 values;
- CI run identity;
- requirement status summary;
- test and coverage links;
- device report links;
- accepted risks and waivers;
- reviewer and approver identities;
- date and release decision.

## Validation boundary for future real-sensor work

Introducing real sensor data creates a different system and requires a new validation plan. At minimum:

1. define the physical measurement problem and reference truth system;
2. document sensor hardware, calibration, synchronization, geometry, and environmental range;
3. preregister datasets, exclusions, metrics, and confidence intervals;
4. separate training, tuning, and blind evaluation data;
5. analyze bias, covariance, drift, missingness, saturation, and nuisance factors;
6. compare against explicit baselines;
7. retain raw data and metadata under an approved data-management plan;
8. conduct independent replication;
9. prohibit operational claims outside the demonstrated domain.

The deterministic game models cannot be promoted to field models by changing labels.

## Formal verification plan

Candidate properties will be ordered by value and tractability:

1. state-machine phase transitions;
2. compute-budget invariant;
3. gate exclusion from fusion;
4. scoring partition boundaries;
5. evidence canonicalization determinism;
6. fusion algebra under an explicitly chosen arithmetic model.

For floating-point properties, the proof obligation must state whether it covers real arithmetic, bounded error, or IEEE-754 execution. A formal model is accepted only with a correspondence argument explaining how the Kotlin implementation maps to it.

## Defect handling

A failed verification item produces:

- defect ID and affected requirement IDs;
- exact reproduction vector and environment;
- expected and observed result;
- severity and user impact;
- containment or rollback decision;
- regression test before closure;
- updated threat model or assurance claim when applicable.

No defect is closed solely because it could not be reproduced on a different configuration.
