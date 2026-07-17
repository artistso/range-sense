# Security Policy

## Scope

Range Sense is an offline Android game simulation. The current application does not request Internet access and does not use a WebView. Security work therefore focuses on software supply-chain integrity, deterministic computation, local data handling, Android platform exposure, build provenance, and accurate technical claims.

This repository is pursuing a **high-assurance engineering baseline**. It is not certified, accredited, or endorsed by DARPA, the Department of Defense, NIST, or any other government organization.

## Supported versions

Security fixes are applied to the latest commit on `main` and to any active release branch explicitly identified in GitHub Releases. Development branches are not supported deployment targets.

## Reporting a vulnerability

Do not open a public issue for a vulnerability that could expose users, signing material, CI credentials, or the software supply chain.

Report privately through GitHub's private vulnerability reporting interface when it is enabled for this repository. Include:

- affected commit, tag, or APK digest;
- device and Android version;
- minimal reproduction steps;
- expected and observed behavior;
- security impact and attack prerequisites;
- logs, screenshots, or proof-of-concept material that do not contain secrets;
- suggested remediation, when known.

If private vulnerability reporting is not available, contact the repository owner through a private channel and provide only enough information to establish a secure follow-up channel.

## Response objectives

These are engineering objectives, not contractual service-level guarantees:

| Severity | Initial acknowledgement | Target triage | Target remediation decision |
|---|---:|---:|---:|
| Critical | 2 business days | 3 business days | 5 business days |
| High | 3 business days | 5 business days | 10 business days |
| Medium | 5 business days | 10 business days | 20 business days |
| Low | 10 business days | 20 business days | Next planned release |

A report may be reclassified after reproduction and threat analysis.

## Security invariants

The following properties are release gates unless an approved, documented exception exists:

1. The production manifest has no Internet permission unless a reviewed requirement explicitly introduces networking.
2. The application contains no WebView-based execution path.
3. GitHub Actions are pinned to immutable commit SHAs.
4. CI uses read-only repository permissions unless a job has a documented need for more.
5. CI credentials are not persisted by checkout.
6. The deterministic core rejects invalid, non-finite, or out-of-domain values.
7. Sensor-gate hard failures fail closed.
8. Build evidence includes source hashes, commit identity, dependency output, verification reports, and APK digest.
9. Secrets, private keys, signing stores, tokens, and controlled data are never committed.
10. Technical documentation distinguishes simulated game behavior from validated field performance.

## Dependency and supply-chain policy

- Dependencies and toolchains are version-pinned.
- Automation dependencies are pinned by commit SHA, not mutable tags alone.
- Dependabot changes require successful CI and human review.
- New dependencies require a stated purpose, license review, maintenance assessment, and removal plan.
- Release artifacts must be traceable to a reviewed source commit and accompanied by a cryptographic digest.
- Release signing keys must be stored outside the repository and outside ordinary CI logs or artifacts.

## Data handling

The current game is designed to operate without network communication. Do not add collection, telemetry, analytics, identifiers, location, camera, microphone, file export, or external storage access without:

1. a written requirement;
2. a threat-model update;
3. a privacy and retention decision;
4. least-privilege Android permissions;
5. negative tests proving that data is not collected outside the approved path.

No classified information, controlled unclassified information, export-controlled technical data, operational targeting data, or real sensor calibration data may be placed in this public repository.

## Security release checklist

Before a release candidate is promoted:

- CI, lint, unit tests, stress tests, and coverage generation pass;
- the APK digest is recorded;
- the source tree is clean during evidence generation;
- dependency changes are reviewed;
- Android permissions and exported components are inspected;
- the threat model and requirements traceability matrix are current;
- known critical or high vulnerabilities have a documented disposition;
- the physical-device verification plan is completed or explicitly waived with rationale;
- signing and artifact-retention procedures are verified.

## Safe-harbor intent

Good-faith research that avoids privacy violations, service disruption, data destruction, extortion, social engineering, and unauthorized access beyond what is necessary to demonstrate the issue will be treated as constructive security research. This statement does not authorize testing of third-party systems or bypass applicable law.
