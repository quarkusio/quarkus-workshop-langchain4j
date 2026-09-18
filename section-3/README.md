# Section 3 agent guide

Steps `00` through `05` are successive snapshots of the trip planner. Each step directory is a complete, runnable Quarkus project. Later steps build on earlier ones; when you change shared behavior, propagate the fix forward through every implemented step that follows.

## What each step adds

| Step | Lesson focus |
|------|----------------|
| **00** | Baseline multi-agent trip planner (research phase, cost estimator, synchronous `/trip/plan`) |
| **01** | Agent skills (`@Skills`, skill markdown under `classpath:skills`) |
| **02** | Output guardrails, rental pricing tool, safe HTTP error mapping |
| **03** | Voting pattern, iterative loops, and adaptive model selection |
| **04** | Quarkus Flow + Kafka approval lifecycle, asynchronous API and UI |
| **05** | PostgreSQL persistence, Flow checkpoints, restart and restore |

When editing step `N`, touch only what that lesson introduces unless you are fixing a bug that also affects later steps. Inherited source, configuration, and UI should stay consistent with the narrative in `docs/docs/section-3/step-NN.md`.

## Tests: one step, one scope

Each step's default `./mvnw test` suite covers **only what that step adds**. Earlier lessons are covered by their own step's CI job, not repeated downstream.

| Step | Default Surefire tests |
|------|------------------------|
| **00** | `TripPlanContractTest`, `TripPlannerResourceTest` (live endpoint needs `OPENAI_API_KEY`) |
| **01** | None (skills are validated manually and in later steps) |
| **02** | Guardrail unit tests, `TripPlanningFailureTest`, `GuardrailExceptionMapperTest` |
| **03** | `VehicleEvaluationAggregatorTest` (aggregation), `TripPlanContractTest` (pipeline with loop), `TripPlanningFailureTest`, guardrail tests |
| **04** | `TripPlannerFlowTest` (smoke), `TripPlanStoreLifecycleTest` |
| **05** | `PersistentTripPlanStoreTest`, `TripPlanStoreLifecycleTest` (persistence-aware) |

Opt-in tests (not in the default suite):

- **Step 04** — `src/test/frontend/live.test.cjs` (real model; requires env vars)
- **Step 05** — `FlowRestartProbe` (three JVM phases against a disposable Postgres; see step-05 `README.md`)

Browser UI checks under `src/test/frontend/` are run manually with Node/Playwright, not Maven Surefire.

## Working across steps

1. Read the lesson doc in `docs/docs/section-3/step-NN.md` before changing code.
2. Compare with the previous step when unsure what is new vs inherited.
3. After a fix in step `N`, apply the same fix to steps `N+1` … `05` if the affected files exist there unchanged.
4. Do not copy full test suites from an earlier step into a later one. Add or adapt tests only for the current lesson.
5. Keep `pom.xml` Surefire `<includes>` aligned with the table above when adding test classes.

## CI

GitHub Actions runs `./mvnw verify` per step in the build matrix (`section-3/step-00` … `step-05`). Each job should finish quickly because tests are scoped to that step only.
