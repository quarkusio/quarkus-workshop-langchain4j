# Step 07: Testing, evaluation, and observability

This step adds an evaluation harness to the trip planner: deterministic invariant checks, a judge model that is separate from the planner's models, and OpenTelemetry tracing exported to Langfuse with the evaluation score attached to the exact planning trace it measured.

The harness is a test-side component. It renders a completed `TripPlan` to a stable text form, evaluates that form against the sample's expected output, records the run, and publishes the score. The application code from Step 06 is unchanged; the work in this step is the `trip-planner` test sources, the evaluation fixtures, and the `quarkus-langfuse` and `quarkus-opentelemetry` dependencies.

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

MCP server tests:
```bash
cd mcp-server && ./mvnw test
```

Live evaluation suite (needs the MCP server running, a container runtime, and a real model key):

```bash
cd trip-planner
./mvnw -Pevals -Dit.test=TripPlannerCompositionLiveIT -Dquarkus.http.test-port=0 verify
./mvnw -Pevals -Dit.test=TripPlanQualityEvaluationLiveIT -Dquarkus.http.test-port=0 verify
```

The composition run drives the full workflow with a scripted model against the real MCP server (no LLM calls). The quality run performs one real planning run, captures its trace, applies the invariant and judge strategies, and publishes the score to Langfuse, which it verifies through the score API. The first quality run pulls the Langfuse Dev Services images. The live ITs skip themselves where their prerequisites are not met, so the default build stays green without them.

## Guides

- [LangChain4j Quarkus testing](https://docs.quarkiverse.io/quarkus-langchain4j/dev/testing.html)
- [Langfuse Quarkus integration](https://docs.quarkiverse.io/quarkus-langfuse/dev/index.html)
