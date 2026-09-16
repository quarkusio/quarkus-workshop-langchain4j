# Step 04: Persistent trip approval

This step is Step 03 plus PostgreSQL persistence. The agent pipeline, pricing tool, output guardrails, Flow descriptor, REST envelopes, and browser UI are inherited. Vehicle and itinerary research run in parallel, followed by cost estimation. The plan has no separate tips field or tips agent. Rental rates are fictional; confirmation is a simulated booking and does not reserve a vehicle.

`quarkus-flow-jpa` stores workflow checkpoints and supplies Hibernate ORM with Panache transitively. `quarkus-jdbc-postgresql` supplies the database driver. These are the only added extensions. Versions and build plugins follow Step 03's `pom.xml`.

## Run

Use Java 21 or newer, Docker or Podman, and a real `OPENAI_API_KEY` for normal application use. Kafka and PostgreSQL start through Dev Services. Keep this step's `.envrc` setting, `TESTCONTAINERS_REUSE_ENABLE=true`, when using reusable development containers.

```bash
./mvnw quarkus:dev
```

Open http://localhost:8080 and http://localhost:8080/q/dev. Add `-Dquarkus.http.port=8084` if needed. On Windows use `mvnw.cmd`. Never run `clean` while dev mode is running.

The dev profile uses schema `update` and Flow auto-restore is enabled. Restart with the same database and unchanged workflow definition to resume persisted approval waits. The original request, request identifier, workflow identifier, and plan remain in the status record. The family skill and watched-resource setting are retained. A watched skill or code edit reloads the application, so use an unchanged definition when checking restoration.

This is a disposable workshop schema, with no migration from earlier versions of Step 04. An old checkpoint or old trip table is not compatible with the corrected definition. Start with a fresh workshop database and fresh event topics when switching definitions. Do not treat `update` as a production migration strategy. Production needs an explicitly configured datasource and schema management.

## State and transactions

`TripPlanStore.register()` commits the original `TripRequest` under a new `requestId` before the planning event is sent. `instanceId` stays null until `bind()` records the actual Flow identifier. The entity ID uses a sequence with allocation size one to order registrations without per-JVM ID blocks. `latest()` returns the newest registration even when it is confirmed, rejected, or failed; updates to older requests do not change that order.

The persisted states are `planning`, `awaiting_approval`, `decision_submitted`, `confirmed`, `rejected`, and `failed`. The entity stores the reviewed plan, confirmation, safe error fields, and accepted decision. Status reads return the same `TripPlanStatus` envelope as Step 03. A later event cannot replace the reviewed plan or reopen a terminal trip. Event extension, payload identifiers, original request, and legal-transition checks are retained.

Each registration commits an independent trip record, so multiple trips can be planning or awaiting approval. Record updates use pessimistic row locks so concurrent decisions and outcomes for the same trip cannot overwrite one another. An HTTP timeout leaves that trip's workflow and planning status intact.

Transactions cover database operations only. Model calls, event publication, and polling sleeps do not hold a database transaction. The blocking outcome consumer commits before acknowledging an event. Only JSON decoding errors are treated as malformed payloads; database errors propagate to the messaging failure path. `onWorkflowFailed()` retains the safe, correlated fallback when Flow cannot publish an outcome. It does not turn unrelated or nonterminal lifecycle notifications into trip failures.

Database state changes and Kafka publication are separate operations. There is no transactional outbox or exactly-once delivery guarantee. A process crash between registration/decision persistence and event submission can leave a record without a corresponding event. An orphaned `planning` row blocks new planning until reconciled or the disposable database is reset. Restart only at `awaiting_approval`, before submitting the decision, when following the recovery exercise. The global latest-trip endpoint has no user isolation or authorization. This remains a single-user workshop, not a production booking service.

## APIs

- `POST /trip/plan` registers a request and waits for its planning outcome. HTTP 200 contains `awaiting_approval`; guardrail failures return 422 and other planning failures return 500. A bounded wait returns 504 without cancelling the workflow. Overlapping planning returns 409.
- `PUT /trip/approve` accepts `{ "instanceId": "...", "status": "approved", "feedback": "" }`, or exactly `rejected`. HTTP 202 reports `decision_submitted`, not completion. Invalid input returns 400, an unknown trip 404, and a duplicate or nonpending decision 409.
- `GET /trip/plan/status?instanceId=...` reads the current envelope. `requestId=...` is also supported before binding; `instanceId` takes precedence. A known failed trip returns HTTP 200 with `status: "failed"`.
- `GET /trip/plan/latest` returns the newest registration, including terminal states and original inputs, or HTTP 204 for an empty store.

The browser restores the latest trip, preserves its identifiers and reviewed plan while polling, and displays a terminal result only after a status read reports it. Network and polling waits remain bounded.

The inherited browser error handling checks recognized error-code and HTTP-status pairs before displaying a server message. Known failed workflows can also be read through HTTP 200 status envelopes; unknown codes and mismatched statuses use local fallback messages.

## Tests

```bash
./mvnw test -Dquarkus.http.test-port=0
```

The default Surefire suite runs only Step 04 persistence tests. Flow, guardrail, and HTTP failure coverage stays in Step 03 so each tutorial step tests what it introduces. The default run uses isolated PostgreSQL Dev Services with container reuse disabled and a fresh schema. Tests need no real API key or live model.

`PersistentTripPlanStoreTest` reads through new transactions and new store objects. It covers registration before binding, active planning exclusion, decisions, every terminal status, replay/identity/transition guards, safe failure, deterministic ordering, and failed database commits without acknowledgement. `TripPlanStoreLifecycleTest` uses the same persistence-aware fixture for lifecycle-event handling. Store recreation alone is not an application-restart test.

The browser checks from earlier steps still apply to this UI. They serve their own HTML/JS and intercept API responses. With Playwright installed in the working copy:

```bash
node --test src/test/frontend/app.test.cjs
```

Use `BROWSER_CHANNEL=chrome` for installed Chrome, or install Playwright Chromium. The opt-in `live.test.cjs` is inherited unchanged and calls a real model only when its live environment variables are set; it is not part of the controlled verification.

## Restart probe

`FlowRestartProbe` is opt-in and excluded from the default Surefire suite. Each command below starts and stops a separate Quarkus test JVM. The probe uses the real Flow definition, JPA checkpoints, REST approval resource, and store, with an isolated scripted adapter and in-memory messaging. It never calls a model.

Start a dedicated PostgreSQL container. The credentials below are only for this disposable local test database. Choose a free host port if 55434 is occupied. Substitute `podman` for `docker` when using Podman.

```bash
docker run --rm -d --name step04-restart-probe -p 127.0.0.1:55434:5432 -e POSTGRES_DB=restart -e POSTGRES_USER=restart -e POSTGRES_PASSWORD=restart postgres:18
export TRIP_RESTART_JDBC_URL=jdbc:postgresql://localhost:55434/restart
./mvnw test -Dtest=FlowRestartProbe -Dtrip.restart.phase=prepare -Dquarkus.http.test-port=0
./mvnw test -Dtest=FlowRestartProbe -Dtrip.restart.phase=restore -Dquarkus.http.test-port=0
./mvnw test -Dtest=FlowRestartProbe -Dtrip.restart.phase=verify -Dquarkus.http.test-port=0
docker stop step04-restart-probe
```

Run the phases sequentially and stop on any failure. `prepare` drops and recreates this dedicated schema, produces a plan, and checks that Flow wrote a checkpoint. `restore` must recover the same trip without planning again and confirm it through the restored approval wait. `verify` checks the confirmed record in another new JVM. This does not test Kafka retention, abrupt process crashes, database outages during restart, or changes to the workflow definition.

## Verification results

On September 15, 2026, `./mvnw package -Dquarkus.http.test-port=0` passed all 80 default Java tests and built the application. All 25 controlled browser tests passed. The PostgreSQL store tests also passed through the Dev MCP runner. The full Java suite used Maven after the Dev MCP connection switched to a closed test-app port. The scripted failure tests can add about a minute to shutdown while a dependency worker finishes.

All three restart-probe phases passed against a dedicated PostgreSQL 18 container using Podman. Separate JVMs created workflow `01M2JYSRZ2RDXG2WGN8XFJV1BW`, restored its `waitApproval` task, confirmed it without another planning call, and recovered the confirmed record after a second restart. The probe container was stopped afterward. No live-model or real Kafka journey was run for Step 04; browser tests used intercepted responses.

## Guides

- [Quarkus Flow](https://quarkiverse.github.io/quarkiverse-docs/quarkus-flow/dev/index.html)
- [Hibernate ORM with Panache](https://quarkus.io/guides/hibernate-orm-panache)
- [Transactions](https://quarkus.io/guides/transaction)
- [Datasources and Dev Services](https://quarkus.io/guides/datasource)
- [Kafka messaging](https://quarkus.io/guides/kafka)
- [LangChain4j guardrails](https://docs.quarkiverse.io/quarkus-langchain4j/dev/guardrails.html)
- [LangChain4j tool guardrails](https://docs.quarkiverse.io/quarkus-langchain4j/dev/function-calling.html#_tool_guardrails)
- [Quarkus testing](https://quarkus.io/guides/getting-started-testing)
