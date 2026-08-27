## Context

See proposal.md — Why. Workshop steps are self-contained Maven modules where each step directory holds the final state of that step and builds on the previous one. `./mvnw clean install` from the root builds every module in sequence, so any test copied forward into a later step runs again at that step. Test coverage today is sparse and uneven across the three sections; this change sets the rule now so it applies as steps gain tests.

## Goals / Non-Goals

**Goals:**
- A written, agent-followed rule for which tests belong in which step, applied to all sections.
- The rule captured in root `AGENTS.md` where agents already look for conventions.
- The existing tests across sections checked against the rule.

**Non-Goals:**
- Changing the build so it skips or deduplicates tests automatically (the rule is enforced by what tests exist, not by build wiring).
- Writing new tests for steps that don't have them yet — that happens when each step is implemented.

## Decisions

**Enforce by placement, not by tooling.** The rule is "a step's `src/test` only tests that step's additions." This is a convention checked at authoring/review time rather than a Maven/Surefire filter. Alternative considered: a shared test module or `@Tag`-based include/exclude at the reactor level. Rejected because each step must stay a standalone, copyable project (AGENTS.md, "Important Considerations") — cross-module test wiring breaks that and adds machinery workshop participants would have to understand.

**Apply to all sections.** The incremental-step structure and full-build cost are the same in Sections 1, 2, and 3, so the rule is stated generally rather than scoped to one section. Alternative considered: scope to Section 3 only (the section under active development). Rejected because the reasoning is identical everywhere and a single uniform rule is easier to follow than a per-section exception.

**Document in root `AGENTS.md`.** Alternative considered: a per-step `AGENTS.md` note. Rejected because the rule is uniform across the workshop; a single statement in the root file (which per-step files inherit from) avoids drift.

**"Addition" means new or behavior-changed production code.** Pure carry-forward code is out of scope for a later step's tests; code whose observable behavior changes in a step is in scope. This mirrors the spec's three scenarios (new / unchanged / modified).

## Risks / Trade-offs

- [A regression in inherited code goes uncaught at a later step because that step no longer tests it] → Acceptable: the behavior is still covered by the step that introduced it, and `clean install` builds every step, so the earlier step's test still runs in a full build.
- [Judging what counts as "changed behavior" is subjective] → Mitigation: the spec's modified-behavior scenario and the AGENTS.md wording tie the decision to *observable* behavior, and step docs describe what each step adds.
