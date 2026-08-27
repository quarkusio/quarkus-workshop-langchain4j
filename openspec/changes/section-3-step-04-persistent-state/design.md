## Context

Step 03 ships `TripPlannerFlow` suspended at a `listen()` task with all state in memory. The
Quarkus dev mode process is the only persistence boundary, so a restart loses everything. The
`quarkus-flow-bom` at version `1.0.0` already includes `quarkus-flow-jpa`; the only missing piece
is a PostgreSQL datasource, the correct Hibernate schema strategy, and a surviving container.

## Goals / Non-Goals

**Goals:**
- Flow state survives a full Quarkus process restart and resumes at the suspended task.
- The restart effect is visible in the UI (workflow instance ID), not only in logs and the Dev UI.
- `TripPlannerFlow` is byte-for-byte unchanged.
- Tests are isolated from the dev container and cannot destroy persisted data.

**Non-Goals:**
- Chat memory persistence (`DatabaseChatMemoryStore`, `TripChatAgent`) — deferred to a later step
  if still valuable; it does not add to the persistence demonstration the approval flow already
  provides.
- Plan refinement / workflow modification — a workflow-orchestration topic (loops, branching,
  multiple event types) that belongs in the Voting/Loops step, not the persistence step.
- Converting `TripPlanStore` to Panache (reserved for "Going further").
- Kafka durability — the restored workflow waits for a fresh `approval.done` event.
- Production-grade connection pooling or schema migration tooling (Flyway/Liquibase).

## Decisions

### Use `quarkus-flow-jpa` for Flow persistence, not a hand-rolled solution
`quarkus-flow-jpa` is already in the BOM and provides automatic persistence with zero changes to
the workflow definition. The alternative — externalising state manually — would require modifying
`TripPlannerFlow`, which contradicts the teaching goal of the step.

### Testcontainers reuse via env var (`TESTCONTAINERS_REUSE_ENABLE=true`), not Compose Dev Services
`quarkus.datasource.devservices.stop-services=false` (Compose V2) was the alternative. The env
var approach is project-scoped through `devbox.json` / `.envrc` without requiring Compose V2,
which is not in `requirements.md`. The trade-off is that the env var cannot be set inside
`application.properties` — it must come from the environment — and fails silently when absent.
Two equivalents are documented for attendees without devbox.

### `schema-management.strategy=update`, not `drop-and-create`
The default under Dev Services is `drop-and-create`, which wipes every table on startup. The
`update` strategy preserves rows. The property was renamed in Quarkus 3.x; the legacy alias
`quarkus.hibernate-orm.database.generation` used in older Flow docs must not appear in this step.

### Display the workflow instance ID in the UI for tangible verification
The results page shows the workflow instance ID below the approve/reject buttons. The tutorial
instructs participants to note the ID, restart the app, refresh the browser, and confirm the same
ID appears — proving the workflow was restored from the database rather than freshly created. This
gives immediate visual feedback that persistence worked, supplementing the developer-focused
verification via the `Restoring workflow instance: <id>` log line and Dev UI database inspection.
The instance ID is already returned by the `/trip/plan` endpoint in Step 03 (used for approval
correlation), so surfacing it is a display-only change with no backend work.

### The form stays the single way to create a trip plan
No chat agent, no second planning entry point. A second path (chat-triggered planning) was
considered and rejected as confusing. The results-page UI adds only a read-only instance ID
display; the approval controls and polling from Step 03 are unchanged.

## Risks / Trade-offs

- **Env var fails silently** → Mitigated by a "did it work?" verification step in the docs:
  look for `Restoring workflow instance: <id>` at DEBUG, confirm the same instance ID in the UI,
  and confirm rows in the Dev UI.
- **Reused containers persist across workshop steps and sessions** → A cleanup note
  (`docker rm -f` / `podman rm -f`) is required in the docs, not optional.
- **`flowinstanceid` identity across restarts** → The approve-after-restart flow depends on the
  restored instance keeping its original ID, and so does the UI instance-ID verification.
  `FlowPersistenceRestore.restoreInstances` keys off the persisted definition, not a per-boot ID,
  but this needs empirical confirmation during implementation.
- **Test/dev container isolation** → If the distinct `db-name` strategy produces a second
  database inside the same container rather than a separate container, tests could still share
  state. Verify during implementation.
- **`QuarkusDevModeTest` for restart durability** → Combining `QuarkusDevModeTest` with in-memory
  connectors and a Dev Services PostgreSQL is fragile. Fall back to asserting Flow instance rows
  exist while suspended (proving persistence) and leaving the restore half to documented manual
  verification if the combined test proves unstable.

## Open Questions

- Does `quarkus.datasource.devservices.db-name=tripplanner_test` in the test profile yield a
  separate Docker/Podman container, or just a separate database inside the reused container?
  Answer determines whether `devservices.reuse=false` is belt-and-braces or load-bearing.
- Is the workflow instance ID stable across a restart (same value before and after), so the UI
  comparison is a valid proof of restoration? Confirm alongside the `flowinstanceid` check.
