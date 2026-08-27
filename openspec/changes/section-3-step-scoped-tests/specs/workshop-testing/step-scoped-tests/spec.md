## Purpose

Defines how automated tests are scoped across the incrementally-built workshop steps (Sections 1, 2, and 3) so that each step verifies only the behavior it introduces, keeping a full-workshop build fast and each step's tests aligned with what it teaches.

## ADDED Requirements

### Requirement: Tests cover only a step's own additions

Each `section-N/step-XX` module SHALL contain tests only for the code that step introduces or changes relative to the previous step. Behavior inherited unchanged from an earlier step SHALL NOT be re-tested in the later step.

#### Scenario: Step adds a new feature

- **WHEN** a workshop step introduces new production code (an AI service, agent, tool, resource, or workflow) not present in the previous step
- **THEN** that step's `src/test` contains tests exercising that new code
- **AND** it does not contain tests that only re-exercise code carried over unchanged from earlier steps

#### Scenario: Step carries code forward unchanged

- **WHEN** a workshop step reuses code from the previous step without changing its behavior
- **THEN** the later step does not include a test for that unchanged behavior
- **AND** the coverage for that behavior remains only in the step that introduced it

#### Scenario: Step modifies inherited behavior

- **WHEN** a workshop step changes the observable behavior of code inherited from an earlier step
- **THEN** the later step includes or updates a test that covers the changed behavior

### Requirement: Convention is documented for agents

The root `AGENTS.md` SHALL document the step-scoped testing convention so that agents authoring or reviewing any workshop step apply it.

#### Scenario: Agent authors tests for a workshop step

- **WHEN** an agent writes tests for a `section-N/step-XX` module
- **THEN** `AGENTS.md` states that tests must cover only that step's additions and must not duplicate earlier steps' coverage
