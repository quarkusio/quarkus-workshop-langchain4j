# Step 00: Trip planner starter

The Miles of Smiles trip planner recommends a vehicle and creates an itinerary in parallel, then estimates costs. Java assembles these three agents' outputs into a `TripPlan` with `vehicle`, `routeOverview`, `itinerary`, and `costs`. The starter uses general prompts without skills, guardrails, or a pricing tool.

Copy this directory to a working directory outside the step folders, then follow the [Step 01 workshop chapter](../../docs/docs/section-3/step-01.md). Step 01 adds Markdown skills and their activation instructions while keeping the workflow, response model, and page unchanged. The starter UI already handles structured planning errors and uses a generic message for unrecognized failures. Step 02 adds recommendation guardrails, guarded rental pricing, and the backend error responses displayed by that UI. The script stays unchanged through Step 02.

## Run

Set `OPENAI_API_KEY` in your terminal and run `./mvnw quarkus:dev` (`.\mvnw.cmd quarkus:dev` on Windows). Open http://localhost:8080 for the trip form or http://localhost:8080/q/dev-ui for the Dev UI. Keep credentials out of shared logs.

`POST /trip/plan` accepts a `TripRequest` and returns the assembled plan synchronously. There is no chat, approval, status, or latest-plan endpoint in this step. The shared page has hooks for later workflow steps, but they are inactive here.

## Test

Run `./mvnw test -Dtest=TripPlanContractTest` for deterministic model, workflow, and HTTP contract checks. The test replaces only the chat model with fixed responses; it checks assembly and JSON fields without calling a provider. It also checks that `POST /trip/chat` returns 404.

Run `./mvnw test` with `OPENAI_API_KEY` unset to run all deterministic tests. The existing `TripPlannerResourceTest` is enabled only when that variable is set and calls the real model. Do not run `clean` while dev mode is running.

## References

- [Quarkus LangChain4j agentic workflows](https://docs.quarkiverse.io/quarkus-langchain4j/dev/agentic.html)
- [Quarkus REST JSON guide](https://quarkus.io/guides/rest-json)
- [Quarkus testing guide](https://quarkus.io/guides/getting-started-testing)
