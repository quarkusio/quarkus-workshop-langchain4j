## Why

Workshop steps build incrementally, each carrying forward the code from the previous step. If every step also copies the previous step's tests, building the full workshop re-runs the same tests at every later step. That wastes build time and, worse, couples a step's green build to behavior that was already the point of an earlier step. Tests should verify what a step actually teaches, not re-litigate settled ground.

## What Changes

- Establish a convention across all sections: each `section-N/step-XX` module ships tests only for the code it introduces or changes, not for behavior inherited unchanged from earlier steps.
- Add the convention to the root `AGENTS.md` so agents apply it when authoring or reviewing any workshop step.
- Audit the existing test files across all sections against the convention and note any that duplicate earlier-step coverage.

## Capabilities

### New Capabilities
- `workshop-testing/step-scoped-tests`: Defines how tests are scoped across incremental workshop steps so each step tests only its own additions.

### Modified Capabilities

<!-- None: the AGENTS.md edit is documentation of the new capability's rule, not a spec-level behavior change to an existing capability. -->

## Impact

- `AGENTS.md` (root): new convention documented.
- `section-N/step-XX/src/test/**` across Sections 1, 2, and 3: existing and future test files must follow the convention.
- No production/runtime code changes.
