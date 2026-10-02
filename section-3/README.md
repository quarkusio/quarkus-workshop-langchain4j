# Section 3 agent guide

Steps `00` through `07` are successive snapshots of the trip planner. Each step directory is a complete, runnable Quarkus project. Later steps build on earlier ones; when you change shared behavior, propagate the fix forward through every implemented step that follows.

## What each step adds

| Step | Lesson focus |
|------|----------------|
| **00** | Baseline multi-agent trip planner (research phase, cost estimator, synchronous `/trip/plan`) |
| **01** | Agent skills (`@Skills`, skill markdown under `classpath:skills`) |
| **02** | Output guardrails, rental pricing tool, safe HTTP error mapping |
| **03** | Voting pattern, iterative loops, and adaptive model selection |
| **04** | Quarkus Flow + Kafka approval lifecycle, asynchronous API and UI |
| **05** | PostgreSQL persistence, Flow checkpoints, restart and restore |
| **06** | MCP integration with declarative `@McpClientAgent` interfaces |
| **07** | Testing, evaluation, and observability: supplied evaluation harness with invariant checks, Langfuse dataset and LLM-as-a-judge evaluator provisioned at dev mode startup, live quality run scored by Langfuse |

When editing step `N`, touch only what that lesson introduces unless you are fixing a bug that also affects later steps. Inherited source, configuration, and UI should stay consistent with the narrative in `docs/docs/section-3/step-NN.md`.

## Tests: one step, one scope

Each step's default `./mvnw test` suite covers **only what that step adds**. Earlier lessons are covered by their own step's CI job, not repeated downstream.

| Step | Default Surefire tests |
|------|------------------------|
| **00** | `TripPlanContractTest`, `TripPlannerResourceTest` (live endpoint needs `OPENAI_API_KEY`) |
| **01** | None (skills are validated manually and in later steps) |
| **02** | Guardrail unit tests, `TripPlanningFailureTest`, `GuardrailExceptionMapperTest` |
| **03** | `VehicleEvaluationAggregatorTest` (aggregation), `VehicleReviewWorkflowTest` (loop, revision, exit conditions), `TripPlanContractTest` (loop position in the pipeline) |
| **04** | `TripPlannerFlowTest` (smoke), `TripPlanStoreLifecycleTest` |
| **05** | `PersistentTripPlanStoreTest`, `TripPlanStoreLifecycleTest` (persistence-aware) |
| **06** | trip-planner: `McpAgentTest`, `DestinationEvidenceTest`; mcp-server: all tests (new module) |
| **07** | trip-planner: `TripPlanInvariantStrategyTest`, `TripPlanEvaluationHarnessTest`; mcp-server: none (unchanged from Step 06) |

Opt-in tests (not in the default suite):

- **Step 04** — `src/test/frontend/live.test.cjs` (real model; requires env vars)
- **Step 06** — `TripPlannerMcpWorkflowIT` (needs a running MCP server)
- **Step 07** — `*LiveIT` classes under the `evals` profile
- **Step 05** — `FlowRestartProbe` (three JVM phases against a disposable Postgres; see step-05 `README.md`)

Browser UI checks under `src/test/frontend/` are run manually with Node/Playwright, not Maven Surefire. They follow the same scoping: Step 02 has the original UI checks, Step 04 has the async UI checks, and Step 06 has the MCP error-rendering checks.

`src/main/resources/META-INF/resources/app.js` and `index.html` are identical in every step (under `trip-planner/` in Steps 06 and 07), so participants never copy frontend files between steps. Steps 00–03 have no `/trip/plan/latest` endpoint, and the frontend treats its 404 as a sign that `/trip/plan` returns a bare `TripPlan` instead of a workflow status envelope. When you change the frontend, apply the same change to all eight copies and run the Step 02, Step 04, and Step 06 browser checks.

## Working across steps

1. Read the lesson doc in `docs/docs/section-3/step-NN.md` before changing code.
2. Compare with the previous step when unsure what is new vs inherited.
3. After a fix in step `N`, apply the same fix to steps `N+1` … `07` if the affected files exist there unchanged.
4. Do not copy full test suites from an earlier step into a later one. Add or adapt tests only for the current lesson.
5. Keep `pom.xml` Surefire `<includes>` aligned with the table above when adding test classes.

## CI

GitHub Actions runs `./mvnw verify` per step in the build matrix (`section-3/step-00` … `step-07`). Each job should finish quickly because tests are scoped to that step only.
