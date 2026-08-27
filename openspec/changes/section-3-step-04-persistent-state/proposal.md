## Why

Step 03 ends with a Quarkus Flow workflow suspended in memory; a restart loses the pending
approval. Step 04 closes that gap by adding PostgreSQL-backed persistence so the workflow
survives a full application restart and resumes exactly where it suspended.

## What Changes

- Add `quarkus-flow-jpa` and `quarkus-jdbc-postgresql` to `section-3/step-04/pom.xml`; Flow
  instance state is then persisted automatically with no changes to `TripPlannerFlow`.
- Set `quarkus.hibernate-orm.schema-management.strategy=update` so the schema is not dropped on
  restart.
- Enable Testcontainers container reuse via `TESTCONTAINERS_REUSE_ENABLE=true` in `devbox.json`
  and a new `.envrc`; document the equivalents for users without devbox.
- Add isolated test datasource config in `src/test/resources/application.properties` to prevent
  test runs from sharing the dev container.
- Display the workflow instance ID on the results page so participants can visually confirm the
  same instance is restored after a restart.
- Renumber Section 3 docs from eight steps to nine, inserting the new Persistent State step at
  position 04 and shifting the former 04–08 to 05–09.

## Capabilities

### New Capabilities

- `workflow-durability`: Quarkus Flow workflow state persisted to PostgreSQL via
  `quarkus-flow-jpa`, with automatic restore on application startup. The workflow instance ID is
  shown in the UI so restoration is visible to the user, not only in logs and the Dev UI.

### Modified Capabilities

<!-- No existing spec-level requirements are changing — this is a new step. -->

## Impact

- **Dependencies**: `quarkus-flow-jpa` (from existing `quarkus-flow-bom`), `quarkus-jdbc-postgresql`.
  `quarkus-hibernate-orm-panache` arrives transitively via `quarkus-flow-jpa`; no extra entry needed.
- **Configuration**: `application.properties` gains three new properties; a test-scoped override
  file is required to prevent test/dev container collisions.
- **Environment**: `TESTCONTAINERS_REUSE_ENABLE=true` must be set for cross-restart container
  reuse. Fails silently without it — tables appear empty but no error is emitted.
- **UI**: The results page shows the workflow instance ID below the approval controls. No new
  planning path is introduced; the form remains the single way to create a trip plan.
- **Docs / nav**: `mkdocs.yml` and five existing `step-NN.md` files need renaming and heading
  updates. The root `section-3-step-04-plan.md` (old Voting step) is deleted.
- **No change to `TripPlannerFlow`**: Flow persistence requires zero modifications to the existing
  workflow definition class.
