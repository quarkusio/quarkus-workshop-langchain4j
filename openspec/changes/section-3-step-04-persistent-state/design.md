## Context

Step 03 ships `TripPlannerFlow` suspended at a `listen()` task with all state in memory. The
Quarkus dev mode process is the only persistence boundary, so a restart loses everything. The
`quarkus-flow-bom` at version `1.0.0` already includes `quarkus-flow-jpa`; the only missing piece
is a PostgreSQL datasource, the correct Hibernate schema strategy, and a surviving container.

Chat memory is held by `InMemoryChatMemoryStoreProducer`, which is a `@DefaultBean`. The
`ChatMemoryProcessor` extension wires `ChatMemoryStore` → `ChatMemoryProvider` → AI service
automatically, so replacing the store requires only one new bean.

## Goals / Non-Goals

**Goals:**
- Both chat history and Flow state survive a full Quarkus process restart.
- Participants write one class (`DatabaseChatMemoryStore`, ~35–40 lines).
- `TripPlannerFlow` is byte-for-byte unchanged.
- Tests are isolated from the dev container and cannot destroy persisted data.

**Non-Goals:**
- Converting `TripPlanStore` to Panache (reserved for "Going further").
- Kafka durability — the restored workflow waits for a fresh `approval.done` event.
- Production-grade connection pooling or schema migration tooling (Flyway/Liquibase).

## Decisions

### Use `quarkus-flow-jpa` for Flow persistence, not a hand-rolled solution
`quarkus-flow-jpa` is already in the BOM and provides automatic persistence with zero changes to
the workflow definition. The alternative — externalising state manually — would require modifying
`TripPlannerFlow`, which contradicts the teaching goal of the step.

### One row per message in `ChatMessageEntity`, not a JSON blob per conversation
One row per message makes the table observable in the Dev UI (attendees watch rows accumulate)
and is a natural teaching artefact. A single JSON blob per conversation would be marginally
simpler to implement but invisible at a glance. `PanacheEntity` arrives transitively via
`quarkus-flow-jpa` → `quarkus-hibernate-orm-panache`, so no extra dependency is required.

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

### Starter code includes `TripChatAgent`, `TripPlannerTools`, `TripChatResource`, and `ChatMessageEntity`
Participants only write the store. Everything else is pre-built to keep focus on the
`ChatMemoryStore` pattern and avoid rebuilding unrelated plumbing.

### `TripChatAgent` is `@ApplicationScoped`, not `@RequestScoped`
AI services default to `@RequestScoped`. `@MemoryId` cannot span requests in that scope, so
`@ApplicationScoped` is required. This is a gotcha worth a callout box in the docs, not silent
starter code.

## Risks / Trade-offs

- **Env var fails silently** → Mitigated by a "did it work?" verification step in the docs:
  look for `Restoring workflow instance: <id>` at DEBUG and confirm rows in the Dev UI.
- **Reused containers persist across workshop steps and sessions** → A cleanup note
  (`docker rm -f` / `podman rm -f`) is required in the docs, not optional.
- **`flowinstanceid` identity across restarts** → The approve-after-restart flow depends on the
  restored instance keeping its original ID. `FlowPersistenceRestore.restoreInstances` keys off
  the persisted definition, not a per-boot ID, but this needs empirical confirmation during
  implementation.
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
- Do the exact method names `ChatMessageSerializer.serialize` and `ChatMessageDeserializer.deserialize`
  match langchain4j 1.13.0, or have they changed? Confirm against the JAR before writing
  `DatabaseChatMemoryStore`.
