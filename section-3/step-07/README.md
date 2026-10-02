# Step 07: Testing, evaluation, and observability

This step adds an evaluation harness to the trip planner: deterministic invariant checks, and an LLM-as-a-judge evaluator that runs inside Langfuse and scores each planning run against a dataset of samples. OpenTelemetry traces the run, and the score with the judge's reasoning lands on that trace.

In dev mode, `LangfuseEvaluationSetup` provisions Langfuse at startup: an OpenAI LLM connection for the judge (the `judgeModel` name, gpt-4o-mini), a numeric `plan-quality` score config, the `trip-plan-samples` dataset seeded from `src/main/resources/evaluation/samples.yaml`, the `plan-quality` evaluator with `evaluation/rubric.txt` as its prompt, and an evaluation rule for the dataset's experiment items. Langfuse names each score after its evaluator, which is why the evaluator and the score config share the name. Every step is idempotent and only logs a warning on failure. The setup is enabled with `trip.evaluation.langfuse-setup.enabled`, which is true in `%dev` only. The rest of the application changes are small. `TracingExecutorSetup` carries the OpenTelemetry context into parallel agent branches so one planning run produces one trace. `VehicleBudgetJudge` and `BudgetVerdict` let `TripAppropriatenessGuardrail` ask the judge model whether the vehicle fits the budget, and `application.properties` configures that `judgeModel`.

The project is a multi-module Maven build:

- `mcp-server/` — Quarkus MCP server with `@Tool` methods returning deterministic scenario data, unchanged from Step 06
- `trip-planner/` — The trip planner app from Step 06 with the Langfuse evaluation setup added and the test harness under `src/test/java/com/tripplanner/evaluation/`

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

Dev mode needs `OPENAI_API_KEY` set for the Langfuse judge. With another provider, edit the LLM connection in the Langfuse UI or in `createLlmConnection()` to use an OpenAI-compatible endpoint (Ollama needs `host.docker.internal` from inside the Langfuse container), or skip the quality run. Langfuse Dev Services starts empty on every dev mode start, and the setup recreates the dataset and the evaluator from `samples.yaml` and `rubric.txt`.

## Tests

Offline suite (no API key, no container runtime, no running MCP server):

```bash
cd trip-planner && ./mvnw test
```

This runs the invariant strategy against the known-good and known-bad fixtures and the harness test over every sample in `samples.yaml`. It is the fast check to run after a prompt, skill, or model change.

The MCP server carries no tests of its own in this step; its `@Tool` methods are unchanged from Step 06 and covered there.

Live evaluation suite (needs the MCP server running and a real model key; the quality run also needs the trip planner running in dev mode, because that is where the dataset and the evaluator are created):

```bash
cd trip-planner
./mvnw -Pevals -Dit.test=TripPlannerCompositionLiveIT -Dquarkus.http.test-port=0 verify
./mvnw -Pevals -Dit.test=TripPlanQualityEvaluationLiveIT -Dquarkus.http.test-port=0 \
    -Dquarkus.langfuse.base-url=http://localhost:<port> verify
```

Take the base URL from the "Dev Services for Langfuse started" message in the dev mode log. The port changes on every restart.

The composition run drives the full workflow with a scripted model against the real MCP server (no LLM calls). The quality run loads the `rome-family-three-days` item from the `trip-plan-samples` dataset and runs `planTrip` through `TripPlanExperimentRunner`, inside a `trip-plan-evaluation` root span with `langfuse.experiment.*` attributes so the trace becomes an experiment item. Each IT run uses a new experiment name (`step-07-<timestamp>`), so it shows up as its own dataset run. The test asserts the invariant checks locally, then waits for the Langfuse evaluator's `plan-quality` score on that trace and asserts it is at least 0.7 with a non-empty reason. Log in with `quarkus@quarkus.io` / `quarkuslangfuse` to see the trace, the score and its reasoning, and the runs under Datasets → `trip-plan-samples`. The live ITs skip themselves when `quarkus.langfuse.base-url` is not set, so the default build stays green without them. When it is set, the quality run fails if the dataset is missing.

## Guides

- [LangChain4j Quarkus testing](https://docs.quarkiverse.io/quarkus-langchain4j/dev/testing.html)
- [Langfuse Quarkus integration](https://docs.quarkiverse.io/quarkus-langfuse/dev/index.html)
- [Langfuse experiments via OpenTelemetry](https://langfuse.com/integrations/native/opentelemetry/experiments)
- [non-deterministic-no-problem](https://github.com/edeandrea/non-deterministic-no-problem), drift detection with a guardrail and session-level scoring on the same extension
