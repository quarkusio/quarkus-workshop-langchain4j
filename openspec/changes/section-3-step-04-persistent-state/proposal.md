## Why

Step 03 ends with a Quarkus Flow workflow suspended in memory and a chat agent with no durable
memory store; a restart loses both the pending approval and the conversation history. Step 04
closes that gap by adding PostgreSQL-backed persistence so that both survive a full application
restart.

## What Changes

- Add `DatabaseChatMemoryStore` — an `@ApplicationScoped` `ChatMemoryStore` implementation that
  persists chat messages in PostgreSQL via a `ChatMessageEntity` (one row per message).
- Add `quarkus-flow-jpa` and `quarkus-jdbc-postgresql` to `section-3/step-04/pom.xml`; Flow
  instance state is then persisted automatically with no changes to `TripPlannerFlow`.
- Set `quarkus.hibernate-orm.schema-management.strategy=update` so the schema is not dropped on
  restart.
- Enable Testcontainers container reuse via `TESTCONTAINERS_REUSE_ENABLE=true` in `devbox.json`
  and a new `.envrc`; document the equivalents for users without devbox.
- Add isolated test datasource config in `src/test/resources/application.properties` to prevent
  test runs from sharing the dev container.
- Renumber Section 3 docs from eight steps to nine, inserting the new Persistent State step at
  position 04 and shifting the former 04–08 to 05–09.

## Capabilities

### New Capabilities

- `chat-memory-persistence`: Durable per-session chat memory backed by PostgreSQL, implemented as
  a `ChatMemoryStore` bean that replaces the default in-memory store.
- `workflow-durability`: Quarkus Flow workflow state persisted to PostgreSQL via
  `quarkus-flow-jpa`, with automatic restore on application startup.

### Modified Capabilities

<!-- No existing spec-level requirements are changing — this is a new step. -->

## Impact

- **Dependencies**: `quarkus-flow-jpa` (from existing `quarkus-flow-bom`), `quarkus-jdbc-postgresql`.
  `quarkus-hibernate-orm-panache` arrives transitively via `quarkus-flow-jpa`; no extra entry needed.
- **Configuration**: `application.properties` gains three new properties; a test-scoped override
  file is required to prevent test/dev container collisions.
- **Environment**: `TESTCONTAINERS_REUSE_ENABLE=true` must be set for cross-restart container
  reuse. Fails silently without it — tables appear empty but no error is emitted.
- **Docs / nav**: `mkdocs.yml` and five existing `step-NN.md` files need renaming and heading
  updates. The root `section-3-step-04-plan.md` (old Voting step) is deleted.
- **No change to `TripPlannerFlow`**: Flow persistence requires zero modifications to the existing
  workflow definition class.
