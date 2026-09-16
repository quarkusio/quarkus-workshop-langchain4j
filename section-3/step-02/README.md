# Step 02: Guardrails and compliance

This completed workshop step extends the skill-aware trip planner with output checks on vehicle and itinerary recommendations, plus a guarded rental-pricing tool for the cost estimator.

The three-agent workflow and four-field `TripPlan` response are unchanged from Steps 00 and 01: vehicle and itinerary research run in parallel, followed by cost estimation and Java assembly. Step 01's skill files and activation instructions remain in place. This step adds guardrails, pricing, and safe backend error responses. The UI is unchanged from the starter and displays those responses when they occur; it has no chat, approval, status, or latest-plan endpoint.

The pricing tool uses fictional daily rates in EUR: compact 45, estate 65, SUV 80, and MPV 90. Its input guardrail accepts the exact lowercase names `compact`, `estate`, `suv`, and `mpv`, with an integer duration from 1 through 30. Invalid arguments return a tool error before the calculation runs. No booking is made and no external pricing service is called.

The model is instructed to use the tool's daily rate and rental subtotal. Tool input validation does not guarantee that the model calls the tool, selects the correct category or duration, or copies the result correctly. Fuel, accommodation, and other trip expenses remain model estimates.

The vehicle guardrail checks the original model against the economy-brand rule before rewriting a small vehicle for a large group. An accepted family rewrite replaces the type, model description, and reasoning with a generic MPV recommendation, not a verified rental offer. SUV and Estate corrections follow the same pattern for adventure and business trips. The recommendation explicitly asks the customer to confirm capacity, price, and availability.

Audit entries use `SKIP` for unchecked paths, including missing vehicle prompt variables and blank or unvalidated vehicle responses. `PASS` means only that the implemented keyword/structure checks completed. It does not establish route safety, affordability, vehicle capacity, or a complete itinerary for the requested duration.

## Run

Set `OPENAI_API_KEY`, then run `./mvnw quarkus:dev` (`.\mvnw.cmd quarkus:dev` on Windows). The trip form is at http://localhost:8080 and the Dev UI is at http://localhost:8080/q/dev-ui.

When another step owns port 8080, use `./mvnw quarkus:dev -Dquarkus.http.port=8082` and open http://localhost:8082 instead. Do not run `clean` while dev mode is running.

`POST /trip/plan` accepts a `TripRequest` and returns a `TripPlan`. The rental tool is available to the cost agent through `@ToolBox`, not through a separate HTTP endpoint.

The vehicle guardrail reads the current invocation's prompt variables, so overlapping plans retain their own traveler counts and budgets. The output checks are examples of validation and rewriting, not complete route-safety or vehicle-suitability checks.

The completed project retains Step 01's family-skill driving descriptions and dev-only watched-resource setting. The skill starts with exactly one rest day for trips of seven or more days. Restore that baseline after Step 01's exactly-two-rest-days experiment. Changing the watched file triggers an automatic application reload on the next request; activation uses the loaded content.

## Test

Run the Step 02 guardrail suite without a model call:

```bash
./mvnw test
```

The default Surefire configuration runs guardrail unit tests, `TripPlanningFailureTest`, and `GuardrailExceptionMapperTest` only. Baseline contract checks stay in Step 00.

The test configuration supplies a dummy API-key fallback. The suite checks output guardrail decisions, tool-argument rejection through Quarkus's execution pipeline, exact rental calculations, and a scripted invalid-call/corrected-call conversation through the production cost agent. Rejected calls must not enter the tool body. The scripted conversation does not predict live-model recovery.

`TripPlanningFailureTest` calls the production REST endpoint and pipeline with a test-profile chat model. It checks corrected vehicle fields over HTTP, exhausted itinerary retries and vehicle reprompts, and an unrelated agent failure. The current dependency counts `maxRetries = 3` as three total responses, including the initial answer; the exhaustion tests assert that count. No retry annotation is changed for testing.

For the supplied browser checks, keep the app running and install test-only tooling in your working copy:

```bash
npm install --no-save --package-lock=false playwright
npx playwright install chromium
APP_URL=http://localhost:8082 node --test src/test/frontend/app.test.cjs
```

In PowerShell, set `$env:APP_URL="http://localhost:8082"` before the `node` command. Use your app's port if different. Set `BROWSER_CHANNEL=chrome` to use an installed Chrome instead of downloaded Chromium. Set `SHOW_BROWSER=true` to watch the test display each result with short pauses.

The two browser tests check desktop and mobile views with intercepted responses, including the corrected family card, safe 422/500 messages, literal HTML-looking error text, malformed or missing messages, and network failures. They do not call a live model or establish that the backend ran a guardrail; the Java HTTP tests cover that boundary.

Verification on September 14, 2026 passed 53 deterministic Java tests and both desktop/mobile browser tests. The documentation built successfully and all three chapter diagrams rendered. No Step 02 live-model recovery or final-price correctness claim follows from these controlled results.

## Error contract

Wrapped agent failures from `POST /trip/plan` return `application/json` with exactly `error` and `message` string fields. A `GuardrailException` anywhere in the cause chain returns HTTP 422:

```json
{
  "error": "guardrail_violation",
  "message": "The trip plan could not pass the recommendation checks. Please revise your trip details and try again."
}
```

Unrelated wrapped agent failures return HTTP 500:

```json
{
  "error": "planning_failed",
  "message": "Could not generate the trip plan. Please try again later."
}
```

Neither message includes exception details or model output. Those details are logged server-side and may be sensitive. Tool-input rejection normally returns an error to the model inside its conversation, not HTTP 422. Failures outside `AgentInvocationException` mapping, such as malformed HTTP input, are not covered by this contract.

The supplied UI reads a nonblank string message only for a matching status/error-code pair and renders it with `textContent`. Invalid responses and network errors use the same generic message as `planning_failed`. Other trip fields still use the existing renderer; this error-path change is not general HTML sanitization.

## Continue

Follow the [Step 02 tutorial](../../docs/docs/section-3/step-02.md) for the hands-on changes and inspection exercise. Participants continuing from Step 01 must copy this step's `src/main/resources/META-INF/resources/app.js` to the same path in their working copy, keep their `index.html`, and refresh the browser. Copy the supplied guardrail, mapper, HTTP, and frontend tests too. No participant frontend implementation is required.

When continuing to Step 03, preserve the pricing tool and guardrail, the cost agent's `days` parameter and tool instructions, and the vehicle and audit corrections. Convert `days` to `Integer` along with `travelers` if the workflow switches to numeric scope values. Carry the error codes and safe messages through the event/store boundary: the REST mapper cannot report a background failure unless that outcome is explicitly returned to the client. Do not turn every workflow failure into `guardrail_violation` or expose exception messages in workflow status responses.

## References

- [Quarkus LangChain4j function calling and tool guardrails](https://docs.quarkiverse.io/quarkus-langchain4j/dev/function-calling.html#_tool_guardrails)
- [Quarkus LangChain4j model guardrails](https://docs.quarkiverse.io/quarkus-langchain4j/dev/guardrails.html)
- [Quarkus testing guide](https://quarkus.io/guides/getting-started-testing)
