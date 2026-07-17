# Requirements Traceability Matrix

## Purpose

This matrix connects each high-value requirement to implementation and verification evidence. A requirement is not considered complete merely because code exists; it must have an identified verification method and an explicit status.

Status values:

- **Implemented** — code exists and automated evidence is present.
- **Partial** — some evidence exists, but one or more acceptance gates remain.
- **Planned** — requirement is accepted but not implemented.
- **Deferred** — intentionally outside the current system boundary.

## Functional and assurance requirements

| ID | Requirement | Implementation | Verification evidence | Status |
|---|---|---|---|---|
| RS-DET-001 | Given the same software version, mission seed, sensor selections, and game actions, the pure Kotlin core shall produce the same result. | `DeterministicRng`, mission factory, sensor simulation, reducer | Deterministic mission/readings test; assurance-digest stability test | Implemented |
| RS-BUD-001 | Selected sensor compute cost shall never exceed the mission compute budget. | `GameEngine.reduce` selection guard; `GameState.computeUsed` | Unit test and multi-mission stress invariant | Implemented |
| RS-NUM-001 | Public core domain objects shall reject non-finite and invalid numerical values. | `init` validation in core models; fusion numerical guards | Constructor tests to be expanded; stress tests for finite outputs | Partial |
| RS-GAT-001 | A sensor with a hard failure shall not pass the information gate. | `FisherGate.evaluate` | `hardFailureClosesGate` | Implemented |
| RS-GAT-002 | A sensor retaining less than the configured conditional-information fraction shall fail closed. | `FisherGate.evaluate` | Stress invariant; dedicated boundary test planned | Partial |
| RS-FUS-001 | Only selected sensors with open gates and valid effective variance shall contribute to fusion. | `SensorFusion.fuse` candidate construction | Stress tests; contributor evidence in canonical record | Implemented |
| RS-FUS-002 | Fusion shall return an untrusted, non-estimate result when no qualified candidate exists or weights are invalid. | `SensorFusion.fuse` fail-closed branches | Existing stress checks; dedicated negative tests planned | Partial |
| RS-FUS-003 | Contributor weights shall be normalized against the total valid weight. | `SensorFusion.fuse` | Stress-test weight normalization | Implemented |
| RS-FUS-004 | Fusion shall expose contributor identity, estimate, uncertainty, normalized weight, residual, trust decision, and reason. | `FusionContributor`, `FusionResult` | Canonical assurance evidence and UI fusion state | Implemented |
| RS-SCO-001 | Scoring thresholds at 0.5, 2, 5, and 15 meters shall use strict documented boundary semantics. | `CidarInspiredTierScorer` | Exact boundary unit test | Implemented |
| RS-ABS-001 | Abstention scoring shall evaluate all cue gates so that a player cannot earn abstention credit by declining to inspect an available qualified cue. | `GameEngine` abstention path and scorer | Stress-test exploit-resistance checks | Implemented |
| RS-AUD-001 | Every completed mission shall produce a canonical evidence record and SHA-256 digest. | `AssuranceEvidenceBuilder` | Determinism, mutation, and incomplete-state tests | Implemented |
| RS-AUD-002 | The evidence record shall identify mission, environment, selected cues, readings, gates, fusion contributors, decision, and result. | `AssuranceEvidenceBuilder` schema v1 | Source review; schema-content test planned | Partial |
| RS-AUD-003 | User debrief shall display the evidence digest and schema identifier. | Compose debrief | Android build; UI instrumentation test planned | Partial |
| RS-CLA-001 | Product and engineering materials shall state that cue models are simulations and not validated field-sensor performance. | Briefing, source comments, README, assurance baseline | Review checklist and text search planned | Partial |
| RS-SEC-001 | The Android application shall not request Internet access in the current offline system boundary. | Android manifest | Manifest inspection in review; automated permission test planned | Partial |
| RS-SEC-002 | The application shall not use a WebView in the current system boundary. | Native Compose architecture | Repository search in review; automated forbidden-symbol check planned | Partial |
| RS-SEC-003 | Secrets, signing keys, private tokens, controlled data, and operational data shall not be committed. | `SECURITY.md`, `.gitignore`, process controls | Secret scanning and release review planned | Planned |
| RS-SUP-001 | Third-party GitHub Actions shall be pinned to immutable commit SHAs. | Android CI workflow | Workflow review | Implemented |
| RS-SUP-002 | CI checkout shall not persist credentials and default workflow permissions shall be read-only. | Android CI workflow | Workflow review | Implemented |
| RS-SUP-003 | Every CI APK build shall emit commit identity, source digest manifest, APK digest, dependency report, and verification artifacts. | Android CI evidence bundle | Workflow artifact inspection | Implemented, pending current run |
| RS-VER-001 | CI shall execute core tests, stress tests included in the test task, Android lint, coverage generation, and debug APK assembly. | Gradle and Android CI | CI job result and uploaded reports | Implemented, pending current run |
| RS-VER-002 | Physical Galaxy Tab S10+ verification shall measure S Pen hover, pressure, eraser, palm rejection, frame time, battery, and thermal behavior. | Verification plan | Signed or versioned device report | Planned |
| RS-VER-003 | Release candidates shall be traceable to a reviewed source commit and cryptographic artifact digest. | CI evidence and release process | Release record | Partial |
| RS-REL-001 | Production releases shall use controlled signing keys stored outside source control and ordinary CI artifacts. | Security policy | Signing procedure and key-custody record | Planned |
| RS-REL-002 | Independently built release candidates shall be compared for reproducibility or documented nondeterminism. | Future release pipeline | Independent-build digest comparison | Planned |
| RS-FRM-001 | Critical invariants shall have machine-checkable specifications before any formal-assurance claim is made. | Future formal model | Proof artifacts and implementation correspondence argument | Planned |
| RS-HMI-001 | Stylus input shall support hover, pressure, drag, and eraser paths without requiring a WebView. | `TacticalViewport` | Physical-device tests; instrumentation tests planned | Partial |
| RS-ACC-001 | Essential controls and state shall remain usable with Android accessibility services. | Compose semantics work pending | TalkBack and automated accessibility report | Planned |

## Traceability rules

1. Each new requirement receives a stable ID before or with implementation.
2. Code comments should reference requirement IDs only when the link adds durable value; comments must not become a substitute for this matrix.
3. Every pull request identifies the requirements it changes.
4. A requirement may move to **Implemented** only when its acceptance evidence is reviewable.
5. Deleted or replaced requirements remain in version history and are not silently renumbered.
6. Requirement changes that affect security, data, permissions, or technical claims require a threat-model review.
7. Formal claims require a separate correspondence argument between the model and the executable implementation.

## Immediate closure work

The next verification increment should close the current **Partial** requirements by adding:

- exact Fisher-gate threshold tests;
- fusion empty-set, invalid-variance, residual, and weight-sum tests;
- canonical-evidence schema-content assertions;
- automated manifest permission and WebView absence checks;
- Compose UI instrumentation for the debrief evidence digest;
- a versioned physical-device test report template.
