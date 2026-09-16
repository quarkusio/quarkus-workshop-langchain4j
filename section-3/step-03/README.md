# Step 03: Event-driven approval with Quarkus Flow

This workshop step wraps the Step 02 planning pipeline in a Quarkus Flow workflow. Kafka carries CloudEvents that start planning, request a customer decision, and report confirmation, rejection, or failure. The initial HTTP request waits for planning, but the later approval wait does not keep that request open.

The planning pipeline keeps parallel vehicle and itinerary research followed by cost estimation with the guarded rental-pricing tool. Booking is simulated: the adapter generates a `MOS-...` reference without contacting inventory or reserving a vehicle. Pricing-tool rates are fictional, and guardrail checks do not establish route safety, vehicle suitability, or final-price correctness.

## Run

Use Java 21 or higher, set `OPENAI_API_KEY` (or configure your chosen model provider), and start Docker or Podman for Kafka Dev Services. Dependency versions are in `pom.xml`.

```bash
./mvnw quarkus:dev
```

On Windows, use `mvnw.cmd quarkus:dev`. Open http://localhost:8080 and http://localhost:8080/q/dev for the application and Dev UI. If another step owns the port, add `-Dquarkus.http.port=8083` and use port 8083 in both URLs. Do not run `clean` while dev mode is running.

Multiple trips can be planning or awaiting decisions. The vehicle guardrail reads each invocation's prompt variables, and the store correlates each outcome and decision to its own request and workflow instance. A planning HTTP timeout leaves the workflow running.

The store and waiting workflows are in memory. Refreshing the browser while the application remains running restores the latest trip with its original request and identifier. Application restarts lose both; Kafka retention alone does not recover them. The latest-trip endpoint is application-wide, not per-user storage. Use one workshop user for the refresh exercise. Step 04 adds persistence.

The supplied family skill and dev-only watched-resource setting retain Step 01's guidance and automatic reload behavior. Finish pending trips before editing a watched skill, code, or configuration: an automatic application reload also loses the in-memory store and approval waits.

## APIs

`POST /trip/plan` accepts a `TripRequest`. It registers a `requestId`, emits `com.tripplanner.trip.requested`, and waits for that request's outcome. The successful response is HTTP 200 with this envelope's fields: `requestId`, `instanceId`, `request`, `status`, `plan`, `confirmation`, `error`, and `message`. A ready plan has status `awaiting_approval`; the workflow identifier is assigned when Flow starts the instance.

`PUT /trip/approve` accepts a decision for an existing pending trip:

```json
{
  "instanceId": "the-identifier-displayed-with-the-plan",
  "status": "approved",
  "feedback": ""
}
```

Use exactly `approved` or `rejected`. HTTP 202 returns the status envelope with `decision_submitted`. This is submission acceptance, not a terminal outcome. The frontend keeps the plan visible and polls until a GET reports `confirmed`, `rejected`, or `failed`. The identifier remains visible for comparison with events.

`GET /trip/plan/status?instanceId=...` reads a trip's current envelope. Use `requestId=...` when the workflow identifier is not yet available; `instanceId` takes precedence if both are supplied. A known failed trip still returns HTTP 200 with `status: "failed"`. Missing query identifiers return 400 and unknown identifiers return 404.

`GET /trip/plan/latest` returns the latest registered trip, including its original request and terminal state, or HTTP 204 when the store is empty. It supports browser refresh. It is not used to correlate the initial POST's result, and it does not select an older trip when another request has since been registered.

## Events and outcomes

The application publishes `com.tripplanner.trip.requested` and `com.tripplanner.trip.approval.done` through `flow-in-producer` to Kafka topic `flow-in`. Flow consumes that topic through its `flow-in` channel. The initiating payload is `PlanningRequest(requestId, request)`; the decision carries `TripApproval` and a `flowinstanceid` extension matching the waiting instance.

The approval wait uses `.envelope(this::matchesDecision)` with a supplied helper that checks both the event's instance extension and the decoded decision against the one accepted by the store. A mismatched instance, mismatched decision, or unreadable payload cannot resume the wait. Both checks belong to this one predicate; a subsequent `dataAs()` filter would replace an existing envelope predicate in this DSL.

Flow publishes its status envelopes through `flow-out` to Kafka topic `flow-out`. The store's `flow-out-consumer` handles `com.tripplanner.trip.approval.requested`, `com.tripplanner.booking.finalized`, `com.tripplanner.trip.rejected`, and `com.tripplanner.trip.failed`. It verifies the event extension, payload identifiers, original request, and allowed state transition before accepting the result. Finalization cannot replace the reviewed plan, and replaying an approval-request event cannot reopen a terminal trip.

Approval calls the simulated booking adapter. Rejection emits its own outcome without calling finalization. Planning or finalization exceptions become failure events with safe messages, because an exception in the workflow cannot propagate through the earlier REST invocation. Event-submission failures are also recorded in the store.

If Flow itself fails, including when an outcome cannot be published, `TripPlanStore.onWorkflowFailed()` handles its `WorkflowFailedEvent`. This records a safe failure for the matching instance, releases its planning lock, and retains any reviewed plan. Unrelated instances, nonterminal notifications, and already-terminal records cannot trigger that transition. Normal outcomes still reach the store through domain events; a timeout or missing event alone does not invoke this fallback.

## Validation and timeouts

A null planning body returns HTTP 400 `invalid_request`. Approval requires a nonblank identifier and an exact decision value: invalid input returns 400 `invalid_decision`, an unknown trip returns 404 `unknown_trip`, and a duplicate decision or decision for a nonpending trip returns 409 `decision_not_pending`. These checks are not comprehensive validation of every trip field or customer authorization.

During the initial planning wait, a guardrail exception in the cause chain returns HTTP 422 `guardrail_violation`; unrelated failures return HTTP 500 `planning_failed`. An unrelated failure during simulated booking uses `finalization_failed`. Failed status envelopes carry safe messages without raw exception text or model output. The event boundary uses this envelope instead of the Step 02 bare-plan response and direct exception-mapper path.

`trip.planning.timeout` defaults to `PT120S`. HTTP 504 `planning_timeout` preserves the request identifier and any available instance identifier, and does not cancel or fail the workflow. Check status before retrying. An interrupted HTTP wait returns 503 `wait_interrupted`, also without proving the workflow stopped.

Browser polling and network waits are bounded. A timeout or status-fetch error keeps the plan and identifiers available and reports uncertainty, without inventing a terminal result. Refreshing the browser can retrieve a later outcome while the application remains running. A successfully submitted rejection is not displayed as completed until the backend records `rejected`.

The browser displays server messages only for recognized error-code and HTTP-status pairs. A GET can report a known failed workflow in an HTTP 200 status envelope. Unknown codes, mismatched statuses, and invalid messages use the local fallback, retaining Step 02's safe-error behavior across the event boundary.

## Verification

Run the default Step 03 suite:

```bash
./mvnw test
```

Surefire runs a slim `TripPlannerFlowTest` smoke suite and `TripPlanStoreLifecycleTest` only. Guardrail and HTTP failure coverage stays in Step 02.

`TripPlannerFlowTest` uses the real Flow definition, store, and REST resources with a mocked adapter. In-memory messaging connectors replace Kafka for the test run. The three smoke tests check approval through to confirmation, rejection without finalization, and safe HTTP 422/500 responses when planning fails. `TripPlanStoreLifecycleTest` checks unrelated instances, nonterminal notifications, workflow failure while awaiting approval, and duplicate failure events.

For the supplied frontend tests, install test-only tooling in your working copy:

```bash
npm install --no-save --package-lock=false playwright
npx playwright install chromium
node --test src/test/frontend/app.test.cjs
```

The suite starts its own local server for the supplied HTML and JavaScript, so Quarkus does not need to be running and no `APP_URL` is required. Set `BROWSER_CHANNEL=chrome` (or `$env:BROWSER_CHANNEL="chrome"` in PowerShell) to use installed Chrome instead of downloaded Chromium. Browser tests use intercepted API responses to check desktop/mobile rendering, restoration, decision submission, safe failures, and bounded waits. They do not prove Kafka delivery or live-model behavior.

For a live check, generate a trip and record its identifier. Confirm the planning HTTP request has finished while Flow waits, then refresh without restarting the application and compare the plan, identifier, and original request. Approve it and follow the matching decision event to the simulated confirmation. Generate another trip, reject it, and refresh to verify it remains rejected with no booking-finalized event for that instance. Inspect the cost agent's pricing-tool call and run the predecessor's controlled checks separately.

`./mvnw test` runs six Java tests (three Flow smoke cases and three store lifecycle checks) in about ten seconds with in-memory messaging and no live model.

## Participation routes

The [Step 03 tutorial](../../docs/docs/section-3/step-03.md) supplies the copy list for participants continuing from Step 02. Use this step's frontend, store, resources, payload models, Flow helpers, and tests together. Keep the complete supplied `TripPlannerFlow` class, including its injected `ObjectMapper` and `matchesDecision()` helper, when editing the descriptor. Remove the earlier bare-response `TripPlannerResourceTest` and `TripPlanContractTest`. The supplied agents retain the predecessor's pricing tool and corrected guardrails, with `days` and `travelers` parameters changed to `Integer` for numeric Flow scope values. The cost agent keeps its duration and tool instructions. The participant edit is the Flow descriptor, not frontend or storage implementation.

Participants opening the completed step use the same supplied files and verification sequence. Neither route introduces persistence or real booking integration.

## Guides

- [Quarkus Flow](https://quarkiverse.github.io/quarkiverse-docs/quarkus-flow/dev/index.html)
- [Quarkus Kafka messaging](https://quarkus.io/guides/kafka)
- [Kafka Dev Services](https://quarkus.io/guides/kafka-dev-services)
- [Quarkus LangChain4j agentic workflows](https://docs.quarkiverse.io/quarkus-langchain4j/dev/agentic.html)
- [Quarkus LangChain4j guardrails](https://docs.quarkiverse.io/quarkus-langchain4j/dev/guardrails.html)
- [Quarkus LangChain4j tool guardrails](https://docs.quarkiverse.io/quarkus-langchain4j/dev/function-calling.html#_tool_guardrails)
- [Quarkus testing](https://quarkus.io/guides/getting-started-testing)
- [CloudEvents](https://cloudevents.io/)
