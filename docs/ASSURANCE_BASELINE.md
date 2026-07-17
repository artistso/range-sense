# Range Sense High-Assurance Engineering Baseline

## Status and claim boundary

This document defines the engineering baseline used to improve Range Sense toward the rigor expected in advanced research and defense software programs.

It does **not** claim DARPA approval, DoD accreditation, operational suitability, cybersecurity certification, validated sensor performance, or compliance with a specific solicitation. Those determinations require a named program, system boundary, contract, classification guide, test authority, and acceptance criteria.

The baseline is instead organized around four defensible objectives:

1. **Evidence over assertion** — important claims must point to reviewable artifacts.
2. **Fail-closed behavior** — invalid or insufficient inputs must not silently produce trusted outputs.
3. **Determinism and replayability** — game-core behavior must be reproducible from recorded inputs.
4. **Traceability** — requirements, implementation, tests, risks, and release evidence must remain connected.

## System boundary

### Included

- Native Android application and Compose user interface.
- Pure Kotlin deterministic game core.
- Mission generation, sensor-cue simulation, information gating, fusion, scoring, and abstention.
- S Pen interaction path.
- Android build, lint, unit/stress testing, coverage generation, and APK packaging.
- CI configuration and generated assurance evidence.

### Excluded

- Real passive-ranging hardware.
- Camera or other live sensor ingestion.
- Network services, telemetry, analytics, cloud synchronization, or remote command paths.
- Classified, controlled, operational, targeting, or calibration data.
- Claims of real-world accuracy, mission effectiveness, safety, or fitness for use.

## Assurance levels

Range Sense uses internal maturity levels to avoid vague labels.

| Level | Meaning | Minimum evidence |
|---|---|---|
| A0 — Concept | Idea or prototype behavior | Design note only |
| A1 — Repeatable | Deterministic implementation with automated tests | Unit tests and seeded replay |
| A2 — Auditable | Traceable requirements, evidence digests, threat model, CI records | Requirements matrix, canonical mission evidence, artifact hashes |
| A3 — Evaluated | Independent negative testing and physical-device measurements | Emulator/device reports, performance data, adversarial tests |
| A4 — Formally constrained | Critical properties represented in a machine-checkable specification | Model checks or proofs connected to executable code |
| A5 — Program-qualified | Accepted against a named external program or authority | Contract-specific evidence and independent acceptance |

The current target is **A2**, with selected A3 work. A4 and A5 remain future gates.

## Core assurance claims

### C-DET-001 — Deterministic replay

For a fixed software version, mission seed, selected sensor set, and user decision sequence, the core produces the same mission state, readings, fusion result, score, and canonical assurance digest.

Evidence:

- deterministic RNG and mission factory;
- deterministic sensor simulation;
- replay unit tests;
- canonical evidence builder using exact hexadecimal floating-point encoding;
- SHA-256 digest shown in the debrief.

Limit: determinism is asserted for the pure Kotlin core under the defined runtime inputs, not for UI frame timing or device scheduling.

### C-NUM-001 — Numerical validity

Accepted domain objects require finite, positive, or bounded values as appropriate. Invalid fusion weights and empty qualified-cue sets produce an untrusted result instead of a plausible estimate.

Evidence:

- constructor invariants;
- gate and fusion checks;
- stress tests across generated missions and sensor subsets;
- CI test reports.

### C-INF-001 — Information-gated cue use

A cue with a hard failure or insufficient retained conditional information is excluded from fusion.

Evidence:

- `FisherGate` fail-closed logic;
- hard-failure tests;
- stress-test invariants.

Limit: the information model is a game mechanic. It is not a calibrated Fisher-information model of a field sensor.

### C-FUS-001 — Fusion accountability

Every fused estimate identifies its contributors, normalized weights, individual estimates, uncertainties, residual, and trust decision.

Evidence:

- typed `FusionContributor` records;
- normalized-weight computation;
- residual threshold and reason text;
- canonical assurance record.

### C-BUD-001 — Resource-budget enforcement

Sensor selection cannot exceed the mission compute budget.

Evidence:

- reducer guard before sensor activation;
- unit and stress tests.

### C-SCO-001 — Strict scoring boundaries

Boundary values at 0.5, 2, 5, and 15 meters follow strict, tested tier semantics.

Evidence:

- explicit scoring implementation;
- exact boundary tests.

### C-AUD-001 — Tamper-evident mission evidence

A completed round produces a canonical evidence record and SHA-256 digest over the deterministic mission, environment, selected cues, readings, gates, fusion contributors, player decision, and result.

Evidence:

- `AssuranceEvidenceBuilder`;
- digest stability and mutation tests;
- digest displayed in the debrief.

Limit: a hash is not a digital signature. It detects record changes only when compared with a trusted digest. Authenticity requires a future signing and key-management design.

### C-SUP-001 — Constrained build supply chain

CI actions are pinned to immutable commit SHAs, checkout credentials are not persisted, workflow permissions are read-only, and each build emits source and APK digests plus dependency output.

Evidence:

- pinned workflow actions;
- CI evidence artifact;
- dependency and source-hash reports.

### C-CLA-001 — Accurate technical claims

User-facing and engineering documentation state that the formulas are deterministic game models and are not validated field-sensor performance.

Evidence:

- briefing disclaimer;
- source documentation;
- README and security policy.

## Development controls

### Change control

Every nontrivial change should include:

- a requirement or defect identifier;
- affected assurance claims;
- implementation summary;
- tests added or modified;
- risk and threat-model impact;
- compatibility and migration impact;
- evidence produced by CI.

### Review control

No change is ready for merge solely because it compiles. Review must examine:

- claim correctness;
- input-domain handling;
- failure behavior;
- deterministic replay impact;
- dependency and permission changes;
- test adequacy, including negative cases;
- whether documentation overstates what was demonstrated.

### Release control

A release candidate requires:

1. reviewed source commit;
2. green CI on that exact commit;
3. recorded APK SHA-256;
4. lint, test, stress, and coverage reports;
5. dependency evidence;
6. physical-device test record or explicit waiver;
7. known-risk disposition;
8. signing-key custody outside the repository;
9. release notes identifying unresolved limitations.

## Formal-assurance roadmap

The core is intentionally separated from Android UI code so that high-value properties can later be specified independently. Candidate formalization targets are:

- compute cost never exceeds budget;
- normalized fusion weights sum to one within a specified numerical model;
- hard-failed or closed-gate sensors never contribute to fusion;
- scoring tiers are mutually exclusive and complete;
- identical evidence inputs produce identical canonical records;
- invalid numerical states cannot be represented at public boundaries.

A future formal-methods phase must identify the exact semantics used for floating-point arithmetic. Proofs over real numbers are not automatically proofs about IEEE-754 execution.

## Open gaps

- No independent security assessment.
- No emulator instrumentation suite.
- No automated accessibility verification.
- No production signing or key-rotation process.
- No reproducible-build comparison across independent runners.
- No SBOM in a standardized interchange format.
- No measured frame-time, stylus-latency, battery, or thermal acceptance record.
- No formal model or proof connected to the Kotlin implementation.
- No contract-specific controls, CUI boundary, export-control review, or authority-to-operate process.

These gaps are tracked as work, not hidden behind a maturity label.
