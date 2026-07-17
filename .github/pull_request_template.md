## Change summary

Describe the problem, the implemented change, and why this approach was selected.

## Requirement and defect traceability

- Requirements affected: `RS-...`
- Defects addressed: `...`
- Assurance claims affected: `C-...`

## System-boundary impact

Check every applicable item:

- [ ] No Android permission change
- [ ] No exported-component or IPC change
- [ ] No networking, telemetry, storage, import/export, camera, microphone, location, or sensor expansion
- [ ] No dependency or build-tool addition
- [ ] No CI permission, secret, action, signing, or publishing change
- [ ] No scoring, gate, fusion, evidence-schema, or technical-claim change
- [ ] Threat model updated where required

Explain all checked exceptions or boundary changes:

## Failure behavior

Describe invalid inputs, unavailable resources, numerical edge cases, and how the change fails closed or degrades safely.

## Verification

- [ ] Unit tests added or updated
- [ ] Boundary and negative tests added
- [ ] Stress/property-oriented tests considered
- [ ] Android lint passes
- [ ] Debug APK builds
- [ ] Coverage report inspected for critical paths
- [ ] Physical-device testing completed or explicitly deferred
- [ ] Assurance evidence generated for the exact head commit

Commands, CI run, seeds, devices, and measurements:

## Supply-chain review

For each new or changed dependency, include purpose, exact version, source, license, maintenance status, permissions, alternatives considered, and removal plan.

- [ ] GitHub Actions remain pinned to immutable commit SHAs
- [ ] Checkout credentials are not persisted
- [ ] Workflow permissions remain least-privilege
- [ ] No secrets or signing material are present in source, logs, or artifacts

## Claim review

- [ ] Documentation distinguishes game simulation from validated field performance
- [ ] No accuracy, security, performance, or compliance claim exceeds the attached evidence
- [ ] Hashes are not described as digital signatures or authenticity proof
- [ ] DARPA/DoD terminology is not used as an endorsement or certification claim

## Evidence and artifacts

List:

- source commit SHA;
- APK SHA-256;
- test, coverage, lint, and stress-report locations;
- threat-model or traceability changes;
- device report, when applicable;
- accepted risks or waivers.

## Reviewer decision record

- [ ] Requirement links are complete
- [ ] Security and numerical failure paths were reviewed
- [ ] Evidence supports the stated claims
- [ ] Known limitations remain visible
- [ ] Ready to merge after required checks and approvals
