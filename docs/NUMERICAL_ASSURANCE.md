# Numerical Assurance Policy

## Scope

This document defines the numerical safety policy for the deterministic Range Sense game model. It governs finite-value checks, physical-domain checks, fusion invariants, and test tolerances.

It does **not** establish real-world sensor accuracy, calibrated estimator performance, safety certification, or operational qualification. The equations remain deterministic game mechanics.

## Fail-closed input domain

A sensor cue is excluded before fusion when any of the following is true:

- range estimate is non-finite or non-positive;
- uncertainty is non-finite or non-positive;
- observed information is non-finite or non-positive;
- conditional information is non-finite or negative;
- conditional information exceeds observed information by more than `1e-18 m^-2`;
- nuisance coupling is outside `[0, 1]`;
- retained information fraction is outside `[0, 1]`;
- a cue-specific hard-failure condition is active.

Fusion independently repeats the essential finite and physical-domain checks. A forced-open or corrupted gate decision must therefore not bypass the numerical boundary.

## Fusion invariants

For every successful fusion result:

1. the estimate and standard deviation are finite and positive;
2. every raw and normalized weight is finite and positive;
3. normalized weights sum to one within `1e-9` in the stress suite;
4. the weighted estimate remains inside the closed hull of contributor estimates, allowing relative roundoff of `1e-12`;
5. weighted residual variance is finite and not below `-1e-12 m^2`;
6. normalized residual and relative uncertainty are finite;
7. sensor iteration order changes the estimate and uncertainty by no more than relative `1e-12`.

A violation returns an empty, untrustworthy fusion result rather than propagating a non-finite value.

## Error-budget interpretation

The tolerances above are implementation error budgets, not measurement uncertainty claims:

| Quantity | Budget | Purpose |
| --- | ---: | --- |
| Conditional-information overshoot | `1e-18 m^-2` | Accommodate final-bit subtraction or multiplication noise |
| Residual-variance negative floor | `1e-12 m^2` | Reject material negativity while tolerating roundoff near zero |
| Weight normalization | `1e-9` absolute | Stress-test guard over many generated missions |
| Contributor-hull check | `1e-12` relative | Verify weighted-mean geometry |
| Sensor-order stability | `1e-12` relative | Bound reduction-order sensitivity |

These values must be changed only with a documented rationale, regression tests, and review of all dependent requirements.

## Verification coverage

The automated test suite currently exercises:

- 2,000 deterministic missions;
- all sensor readings for each mission;
- every sensor subset within the compute budget;
- finite-value and information-ordering invariants;
- contributor identity and normalized-weight invariants;
- weighted-estimate hull containment;
- 500-mission forward/reverse sensor-order comparison;
- explicit adversarial infinite-value fixtures;
- hard-failure and information-overshoot gate closure.

## Remaining work

The following are required before claiming higher numerical assurance:

- compensated or pairwise summation comparison;
- randomized property testing over synthetic extreme-value fixtures;
- explicit overflow and underflow tests near IEEE-754 limits;
- cross-runtime comparison on the target Android hardware;
- formal specification of gate and fusion preconditions/postconditions;
- independent review of tolerance selection.
