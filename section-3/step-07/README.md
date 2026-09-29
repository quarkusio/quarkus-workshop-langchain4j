# Step 07: Testing, evaluation, and observability

This step adds an evaluation harness to the trip planner: deterministic invariant checks, a judge model that is separate from the planner's models, and OpenTelemetry tracing exported to Langfuse with the evaluation score attached to the exact planning trace it measured.

The harness is a test-side component. It renders a completed `TripPlan` as JSON, checks it against invariants derived from the sample's parameters, asks the judge whether it meets the requirements in the sample's expected output, records the run, and publishes the score. Most of the work is in the `trip-planner` test sources, the evaluation fixtures, and the `quarkus-langfuse` and `quarkus-opentelemetry` dependencies. The application code gets three small additions. `TracingExecutorSetup` carries the OpenTelemetry context into parallel agent branches so one planning run produces one trace. `VehicleBudgetJudge` and `BudgetVerdict` let `TripAppropriatenessGuardrail` ask the judge model whether the vehicle fits the budget. `application.properties` configures that `judgeModel`.

The project is a multi-module Maven build:

- `mcp-server/` — Quarkus MCP server with `@Tool` methods returning deterministic scenario data, unchanged from Step 06
- `trip-planner/` — The trip planner app from Step 06 with the evaluation harness added under `src/test/java/com/tripplanner/evaluation/`

The agent pipeline, voting loop, pricing tool, output guardrails, Flow descriptor, persistence, REST envelopes, and browser UI are inherited from Step 06.

## Run

Use Java 21 or newer, Docker or Podman, and a real `OPENAI_API_KEY` for the live evaluation runs.

**Terminal 1 — MCP Server:**
```bash
cd mcp-server
./mvnw quarkus:dev
```

**Terminal 2 — Trip Planner:**
```bash
cd trip-planner
./mvnw quarkus:dev
```

Open http://localhost:8080 and http://localhost:8080/q/dev. The MCP server runs on port 8085.

## Tests

Offline suite (no API key, no container runtime, no running MCP server):

```bash
cd trip-planner && ./mvnw test
```

This runs the invariant strategy against the known-good and known-bad fixtures, the judge contract with a scripted model, the scorer and sample loading, and the run recorder. It is the fast check to run after a prompt, skill, or model change.

The MCP server carries no tests of its own in this step; its `@Tool` methods are unchanged from Step 06 and covered there.

Live evaluation suite (needs the MCP server running and a real model key; the quality run also needs the trip planner running in dev mode so its Langfuse is up):

```bash
cd trip-planner
./mvnw -Pevals -Dit.test=TripPlannerCompositionLiveIT -Dquarkus.http.test-port=0 verify
./mvnw -Pevals -Dit.test=TripPlanQualityEvaluationLiveIT -Dquarkus.http.test-port=0 \
    -Dquarkus.langfuse.base-url=http://localhost:<port> verify
```

Take the base URL from the "Dev Services for Langfuse started" message in the dev mode log. The port changes on every restart.

The composition run drives the full workflow with a scripted model against the real MCP server (no LLM calls). The quality run performs one real planning run, captures its trace, applies the invariant and judge strategies, and publishes the score to the dev mode Langfuse, where it verifies the score through the API. Log in with `quarkus@quarkus.io` / `quarkuslangfuse` to see the trace and its `plan-quality` score. The live ITs skip themselves where their prerequisites are not met, so the default build stays green without them.

## Guides

- [LangChain4j Quarkus testing](https://docs.quarkiverse.io/quarkus-langchain4j/dev/testing.html)
- [Langfuse Quarkus integration](https://docs.quarkiverse.io/quarkus-langfuse/dev/index.html)
