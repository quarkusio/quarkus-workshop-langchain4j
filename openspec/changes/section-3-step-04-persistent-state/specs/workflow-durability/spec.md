## Purpose

Persists Quarkus Flow workflow instances to PostgreSQL via `quarkus-flow-jpa` so that
suspended workflows survive a full application restart and resume from their last checkpoint
without any changes to the workflow definition class.

## ADDED Requirements

### Requirement: Workflow state is persisted to PostgreSQL automatically
The system SHALL persist Flow instance state when `quarkus-flow-jpa` is on the classpath
and a PostgreSQL datasource is configured. No changes to `TripPlannerFlow` or any other
workflow definition class SHALL be required.

#### Scenario: Flow instance rows exist while a workflow is suspended
- **WHEN** `TripPlannerFlow` reaches a `listen()` task and pauses
- **THEN** the Flow instance is visible as a row in the PostgreSQL database and in the Dev UI

### Requirement: Suspended workflows are automatically restored on startup
The system SHALL restore all suspended Flow instances when the application starts, provided
`quarkus.flow.persistence.auto-restore` is `true` (the default). Restoration SHALL iterate
all registered workflow definitions, scan persisted instances, and call `start()` on each.

#### Scenario: A suspended workflow resumes after a restart
- **WHEN** the application is restarted with the same PostgreSQL container (schema intact)
- **THEN** the previously suspended workflow instance is restored and resumes waiting at its `listen()` task
- **THEN** the log at startup contains `Restoring workflow instance: <id>`

### Requirement: A restored workflow completes when its awaited event arrives
A restored Flow instance SHALL correlate incoming events by the same `flowinstanceid` as before
the restart and SHALL complete the workflow when the matching event is received.

#### Scenario: Approve-after-restart succeeds
- **WHEN** a trip approval event is sent after a restart using the original `flowinstanceid`
- **THEN** the workflow transitions out of the `listen()` task and completes normally

### Requirement: The workflow instance ID is displayed in the UI
The results page SHALL display the workflow instance ID returned by the `/trip/plan` endpoint,
below the approval controls. This gives the user a visible identifier to correlate against the
`Restoring workflow instance: <id>` startup log and the Dev UI database row, making restoration
observable rather than requiring the user to trust that it happened.

#### Scenario: The displayed instance ID matches the restored instance
- **WHEN** a plan is generated and the results page shows an instance ID, then the app is
  restarted
- **THEN** the startup log contains `Restoring workflow instance: <id>` with that same ID, and
  the Dev UI shows a Flow instance row with that ID

### Requirement: Hibernate schema strategy must not drop tables on restart
The application configuration SHALL set `quarkus.hibernate-orm.schema-management.strategy=update`.
Using the default `drop-and-create` under Dev Services SHALL cause all persisted state to be
lost on every restart, which nullifies durability.

#### Scenario: Data survives a restart when strategy is update
- **WHEN** the application is restarted with `schema-management.strategy=update`
- **THEN** Flow instance rows from before the restart are still present

### Requirement: Container reuse is required for a full cold restart
The PostgreSQL Dev Services container MUST survive the restart. This requires
`TESTCONTAINERS_REUSE_ENABLE=true` in the environment (set via `devbox.json`, `.envrc`, or
`~/.testcontainers.properties`). Setting this property inside `application.properties` has no
effect because Dev Services reads it from the environment, not from Quarkus config.

#### Scenario: Cold restart succeeds with container reuse enabled
- **WHEN** `TESTCONTAINERS_REUSE_ENABLE=true` is set in the shell environment before starting
- **THEN** the Dev Services PostgreSQL container is reused across restarts and its data is intact

#### Scenario: Env var absence causes silent data loss
- **WHEN** `TESTCONTAINERS_REUSE_ENABLE` is not set
- **THEN** a new container is created on restart, tables appear empty, and no error is emitted

### Requirement: Test runs use an isolated datasource
`src/test/resources/application.properties` SHALL configure a distinct `devservices.db-name`
and set `devservices.reuse=false` and `schema-management.strategy=drop-and-create` so that test
execution does not share or corrupt the developer's persisted dev container.

#### Scenario: Tests do not share the dev container
- **WHEN** `@QuarkusTest` runs while dev mode is active with a reused container
- **THEN** tests connect to a separate container (different db-name) and do not affect dev data
