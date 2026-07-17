# Threat Model

## Document status

This threat model covers the current offline Android vertical slice and its public build pipeline. It must be reviewed whenever permissions, data flows, dependencies, signing, storage, import/export, networking, sensors, or external integrations change.

This is an engineering analysis, not an authorization to process classified information, controlled unclassified information, export-controlled data, or operational data.

## Security objectives

1. Preserve integrity of the deterministic game core and scoring logic.
2. Prevent unreviewed code or dependencies from entering release artifacts.
3. Preserve traceability from source commit to APK and verification evidence.
4. Prevent secrets, signing material, and controlled data from entering the public repository or build artifacts.
5. Keep the current application offline and least-privileged.
6. Prevent simulated performance from being misrepresented as validated field capability.
7. Detect invalid numerical states and fail closed rather than present a trusted estimate.
8. Preserve availability and usability on the supported tablet without uncontrolled resource consumption.

## Assets

| Asset | Security property |
|---|---|
| Game-core source and invariants | Integrity, traceability |
| Scoring and abstention logic | Integrity, replayability |
| Assurance evidence schema and digest | Integrity, determinism |
| GitHub repository and branch history | Integrity, accountability |
| CI workflow and dependencies | Integrity, least privilege |
| APK and future release bundles | Integrity, provenance |
| Release signing keys | Confidentiality, integrity, availability |
| Verification reports and device measurements | Integrity, traceability |
| User device resources and local state | Availability, privacy |
| Technical claims and disclaimers | Accuracy, non-deception |

## Trust boundaries

1. **Developer workstation to GitHub** — source, commits, credentials, and review metadata cross into the hosted repository.
2. **Repository to GitHub Actions runner** — source and workflow instructions enter an ephemeral third-party execution environment.
3. **Dependency repositories to build** — Gradle plugins, Android components, JVM artifacts, and actions enter the build environment.
4. **CI output to user device** — an APK leaves CI and is installed on Android.
5. **Android framework to application** — stylus and touch events cross from device drivers and the OS into application code.
6. **Game state to assurance record** — internal values are serialized into a canonical evidence record and digest.
7. **Public documentation to external interpretation** — readers may infer capabilities beyond what was actually demonstrated.

## Adversaries and failure sources

- A malicious contributor or compromised maintainer account.
- A compromised third-party dependency, action, package repository, or build runner.
- A user installing a substituted or modified APK.
- Accidental developer error that weakens invariants or permissions.
- Malformed, extreme, or non-finite numerical values introduced by future input paths.
- Device or OS behavior that delivers unexpected stylus/touch sequences.
- A party intentionally or accidentally overstating simulation results.
- Loss, theft, or exposure of future release signing keys.
- Resource-exhaustion conditions caused by excessive rendering, allocations, or event processing.

## Current attack surface

### Repository and CI

- Pull requests and direct branch writes.
- GitHub Actions workflow definitions.
- Mutable dependency registries.
- Build logs and uploaded artifacts.
- Repository secrets and future signing integrations.

### Android application

- Android manifest and exported components.
- Activity lifecycle and process restoration.
- Stylus, hover, eraser, pressure, and touch event streams.
- Future local storage or import/export paths.
- Debuggable engineering APKs.

### Core computation

- Mission seed and generated environment.
- Numerical domain boundaries.
- Sensor-selection state transitions.
- Gate decisions, fusion weights, residual calculations, scoring, and evidence serialization.

### Human and claims layer

- README, screenshots, demos, talks, issue discussions, and proposal material.
- Misinterpretation of game terminology as an operational system.

## Threat analysis

| ID | Threat | Consequence | Existing controls | Residual work |
|---|---|---|---|---|
| T-SUP-001 | A mutable CI action tag is replaced upstream. | Arbitrary code execution in CI; artifact compromise. | Actions pinned to commit SHAs; read-only permissions; credentials not persisted. | Periodic pin review; provenance attestation; isolated release workflow. |
| T-SUP-002 | A Gradle or Android dependency is compromised or typosquatted. | Malicious build-time or runtime code. | Version catalog, Dependabot, dependency evidence. | Dependency verification metadata; SBOM; vulnerability scanning; repository allowlist. |
| T-SUP-003 | A build artifact is substituted after CI. | User installs unreviewed code. | APK SHA-256 evidence and commit record. | Signed releases, published checksums, provenance attestation, independent rebuild. |
| T-CRE-001 | Repository or CI credentials leak into logs or artifacts. | Unauthorized source or release modification. | Read-only workflow permissions; checkout credentials disabled; no signing keys in current CI. | Secret scanning; environment protection; short-lived release credentials. |
| T-KEY-001 | Future signing key is stolen or copied into source control. | Attacker can sign malicious releases. | Policy forbids repository storage. | Hardware-backed or managed key custody; rotation and revocation procedure. |
| T-COD-001 | A code change bypasses compute-budget or gate invariants. | Invalid game result appears legitimate. | Pure reducer, constructor validation, unit/stress tests, traceability matrix. | Property-based tests; independent review; formal model for critical invariants. |
| T-NUM-001 | NaN, infinity, overflow, underflow, or cancellation enters fusion or evidence. | Crashes, divergent replay, misleading trust result. | Finite/domain checks, invalid-weight failure paths, exact evidence encoding. | Broader numerical boundary tests; floating-point error budget; static analysis. |
| T-FUS-001 | Correlated or biased simulated cues are treated as independent. | Overconfident fused uncertainty inside the game. | Nuisance inflation, residual check, simulation disclaimer. | Explicit covariance model or conservative bound; calibration experiments if real sensors are ever introduced. |
| T-AUD-001 | Evidence record is changed while digest is recomputed by an untrusted party. | False audit narrative. | Canonical hash exposes accidental or uncoordinated change. | Digital signatures, trusted timestamp/provenance service, immutable release record. |
| T-AUD-002 | Canonical schema changes without versioning. | Replay and comparison become ambiguous. | Explicit schema version. | Compatibility tests and schema-change review rule. |
| T-AND-001 | An Android component is accidentally exported or a dangerous permission is added. | External invocation or unintended data access. | Current minimal manifest and no Internet permission. | Automated manifest assertions; exported-component review; permission diff gate. |
| T-HMI-001 | Unexpected event ordering or palm input causes unintended actions. | Incorrect player decision or degraded usability. | Stylus-first event handling. | Instrumentation and physical-device adversarial event tests. |
| T-DOS-001 | Rendering or input processing consumes excessive CPU, memory, battery, or thermal budget. | App slowdown, crash, device heating, battery drain. | Small offline vertical slice. | Macrobenchmark, frame metrics, memory limits, thermal and battery acceptance tests. |
| T-PRV-001 | Future telemetry or storage collects data without explicit review. | Privacy breach and system-boundary expansion. | No current networking; security policy change gate. | Automated permission checks; data inventory; retention and deletion tests. |
| T-CLA-001 | Simulation outputs are presented as validated real-world ranging capability. | Misleading technical or acquisition claim. | Repeated disclaimers and claim boundary. | Claim review checklist; prohibit performance numbers without test protocol and raw evidence. |
| T-DAT-001 | Classified, CUI, export-controlled, calibration, or operational data is committed. | Legal, security, and disclosure harm. | Public-repository prohibition in policy. | Pre-commit/CI scanning, contributor training, incident procedure. |
| T-DBG-001 | Debug APK is mistaken for a production release. | Weaker runtime protections and provenance confusion. | Artifact named debug; documentation labels engineering build. | Separate release pipeline; release signing; debug watermark and version metadata. |

## Abuse and misuse cases

### Misuse: presenting the app as a validated sensor

An operator, researcher, or promoter could remove the disclaimer and cite game scores or simulated error values as measured physical capability.

Required response:

- maintain clear in-app and repository claim boundaries;
- do not publish accuracy claims without a preregistered test protocol, raw measurements, uncertainty analysis, and independent review;
- preserve simulation and field-test datasets as distinct types and repositories.

### Misuse: importing sensitive real-world data

A future contributor could add camera input, target imagery, calibration files, or operational metadata to the public project.

Required response:

- stop development on that data path until classification, export-control, privacy, and storage boundaries are resolved;
- use synthetic/public data for the open repository;
- isolate any approved sensitive environment from public CI and public issue tracking.

### Misuse: treating a digest as an authenticity proof

A SHA-256 digest can demonstrate record equality, but anyone who can alter a record can calculate a new digest.

Required response:

- label the current evidence as tamper-evident comparison material, not signed provenance;
- add digital signatures and controlled key custody before authenticity claims.

## Risk-rating method

Risk is assessed using qualitative likelihood and impact:

- Likelihood: Rare, Unlikely, Possible, Likely, Almost Certain.
- Impact: Negligible, Minor, Moderate, Major, Severe.
- Priority: Low, Medium, High, Critical, based on the combined judgment and existing controls.

A numerical score may supplement this model, but it must not replace the written assumptions. Precision unsupported by evidence is not rigor.

## Review triggers

Update this threat model before merging any change that introduces:

- Internet, Bluetooth, NFC, USB-host, camera, microphone, location, storage, or sensor permissions;
- exported Android components, deep links, file providers, or IPC;
- telemetry, accounts, identifiers, analytics, cloud services, or remote configuration;
- file import/export or persistent mission records;
- native code, dynamic code loading, scripting, WebView, reflection-based plugins, or model downloads;
- new CI permissions, secrets, release signing, publishing, or third-party actions;
- real sensor data, calibration data, field trials, or externally reported performance claims;
- changes to evidence schema, scoring boundaries, gate thresholds, or fusion trust logic.

## Accepted current limitations

- The engineering APK is debug-signed.
- Mission evidence is hashed but not digitally signed.
- No standardized SBOM or SLSA-style provenance statement is emitted yet.
- No independent penetration test has been performed.
- No formal proof covers the Kotlin or Compose implementation.
- Device-level performance and stylus behavior have not yet been captured in a versioned acceptance report.

These limitations prevent any claim of operational or program qualification.
