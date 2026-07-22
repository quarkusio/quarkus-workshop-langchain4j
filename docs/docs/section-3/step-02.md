# Step 02 - Guardrails and Compliance

## Ensuring Safe and Honest Recommendations

In Step 01, you built a multi-agent Trip Planner that splits planning across specialized agents. But what happens when a vehicle agent recommends a 2-seat sports car to a family of 5? Or an itinerary agent suggests a route through a dangerous area?

In production systems, you can't trust LLM output blindly. **Guardrails** are programmatic checks that validate, reject, or rewrite agent output before it reaches the user. They enforce business rules that the LLM alone might not follow consistently.

In this step, you'll add output guardrails to individual agents in the Trip Planner pipeline, ensuring every recommendation is **safe** and **appropriate** regardless of what the LLM generates.

---

## In This Step

In this step, you will:

- Implement `OutputGuardrail` classes that validate LLM-generated trip plans
- Use `retry()` and `reprompt()` to ask the LLM to **regenerate** output that fails safety or appropriateness checks
- Use `successWith()` to **rewrite** output that has minor appropriateness issues
- Build an **audit trail** that logs every guardrail decision for compliance
- Handle guardrail failures gracefully with a JAX-RS `ExceptionMapper`
- Bridge request context to guardrails using an `@ApplicationScoped` CDI bean

---

## Build Overview

We'll add four new components to the multi-agent Trip Planner from Step 01, applying guardrails to individual agents in the pipeline:

1. **TripSafetyGuardrail**: Applied to `ItineraryPlannerAgent`. Validates hard safety rules (dangerous routes, empty itineraries). Uses `retry()` on failure.
2. **TripAppropriatenessGuardrail**: Applied to `VehicleAdvisorAgent`. Validates soft appropriateness rules (vehicle/traveler mismatch, budget coherence). Uses `successWith()` to rewrite minor issues.
3. **GuardrailAuditLog**: Logs every guardrail decision (`PASS`, `RETRY`, `REWRITE`) for compliance.
4. **TripRequestContext**: An `@ApplicationScoped` CDI bean that bridges trip parameters from the REST endpoint to the guardrails.

**The Flow:**

```mermaid
sequenceDiagram
    participant User as Web UI
    participant REST as TripPlannerResource
    participant Context as TripRequestContext
    participant Seq as TripPlannerSystem
    participant VA as VehicleAdvisorAgent
    participant IP as ItineraryPlannerAgent
    participant LLM as OpenAI LLM
    participant G1 as TripSafetyGuardrail
    participant G2 as TripAppropriatenessGuardrail
    participant Audit as GuardrailAuditLog
    participant CE as CostEstimatorAgent
    participant TG as TipsGeneratorAgent

    User->>REST: POST /trip/plan (TripRequest)
    REST->>Context: Store trip parameters
    REST->>Seq: planTrip(...)

    par Vehicle & Itinerary in parallel
        VA->>LLM: Recommend vehicle
        LLM-->>VA: VehicleRecommendation JSON
        VA->>G2: validate(AiMessage)
        G2->>Context: Read trip parameters
        G2->>Audit: Log decision
        alt Vehicle too small
            G2-->>VA: successWith(rewritten JSON)
        else Luxury on economy
            G2-->>VA: reprompt("Use affordable vehicle", stricter prompt)
            VA->>LLM: Regenerate with new system prompt + feedback
        else All checks pass
            G2-->>VA: success()
        end
    and
        IP->>LLM: Plan itinerary
        LLM-->>IP: ItineraryResult JSON
        IP->>G1: validate(AiMessage)
        G1->>Audit: Log decision
        alt Safety check fails
            G1-->>IP: retry("Fix dangerous route...")
            IP->>LLM: Regenerate with feedback
        else Safety check passes
            G1-->>IP: success()
        end
    end

    Seq->>CE: estimateCosts(vehicle, itineraryResult, ...)
    CE-->>Seq: CostEstimate

    Seq->>TG: generateTips(vehicle, itineraryResult, costs, ...)
    TG-->>Seq: List of tips

    Note over Seq: @Output assembles TripPlan (no LLM)
    Seq-->>REST: TripPlan
    REST-->>User: JSON response
```

---

## Understanding Output Guardrails

An `OutputGuardrail` is a class that intercepts the LLM's response **before** it's returned to the caller. It can:

| Action | Method | When to Use |
|--------|--------|-------------|
| **Accept** | `success()` | Output passes all checks |
| **Retry** | `retry(feedback)` | Output has serious issues, ask the LLM to regenerate with feedback |
| **Reprompt** | `reprompt(feedback, newSystemPrompt)` | Like retry, but also replaces the system prompt for stricter instructions |
| **Rewrite** | `successWith(AiMessage)` | Output has minor issues, fix them in code and accept |
| **Reject** | `failure(message)` | Output is fatally flawed, stop processing |

Multiple guardrails can be chained with `@OutputGuardrails`. They run in order: if the first guardrail triggers a `retry()`, the LLM regenerates and **all** guardrails run again against the new response. The `maxRetries` parameter caps the number of retry attempts.

!!! note "Input vs. Output Guardrails"
    In [Section 1](../section-1/step-09.md){target="_blank"}, you used an `InputGuardrail` to detect prompt injection **before** the LLM processes the request. Output guardrails work on the other side and validate the LLM's **response** before it reaches the user. Both are critical for production systems.

---

## Prerequisites

=== "Option 1: Continue from Step 01"

    If you want to continue building on top of Step 01 code, stay in the `section-3/step-01` directory. You'll add the guardrail classes and wire them to the existing agents as described below.

=== "Option 2: Follow along using the completed solution"

    If you prefer to follow along (without making any code changes), navigate to the completed `section-3/step-02` directory:

    === "Linux / macOS"
        ```bash
        cd section-3/step-02
        ./mvnw quarkus:dev
        ```

    === "Windows"
        ```cmd
        cd section-3\step-02
        mvnw quarkus:dev
        ```

---

## Component 1: The Audit Log

Before building the guardrails, let's create the audit trail service that both guardrails will use.

In `src/main/java/com/tripplanner/guardrails`, create `GuardrailAuditLog.java`:

```java title="GuardrailAuditLog.java"
--8<-- "../../section-3/step-02/src/main/java/com/tripplanner/guardrails/GuardrailAuditLog.java"
```

`@ApplicationScoped` gives you a single instance shared across all requests. Each audit entry records a timestamp, the guardrail name, the decision (`PASS`, `RETRY`, `REWRITE`), and a reason. Entries are stored in a `ConcurrentLinkedDeque` (capped at 100) for thread safety. The component also logs to the JBoss logger at INFO level, so you can watch guardrail decisions in the terminal while you test.

---

## Component 2: The TripRequestContext

The `TripAppropriatenessGuardrail` needs to know the original trip parameters (how many travelers, what budget, what trip type) to validate the LLM's response. Since the `OutputGuardrail.validate()` method only receives the `AiMessage`, we use a `@RequestScoped` CDI bean to bridge the data.

In `src/main/java/com/tripplanner/model`, create `TripRequestContext.java`:

```java title="TripRequestContext.java"
--8<-- "../../section-3/step-02/src/main/java/com/tripplanner/model/TripRequestContext.java"
```

`@ApplicationScoped` gives you one shared instance that persists across requests. The field is `volatile` so updates remain visible across the parallel threads used by `@ParallelAgent`. The REST endpoint populates the context before calling the agent, and the guardrail reads it during validation. This is why `@ApplicationScoped` is used instead of `@RequestScoped`, since request context is not propagated to those parallel agent threads.

---

## Component 3: The Safety Guardrail

This guardrail enforces **hard rules** — violations that require the LLM to regenerate its response. It is applied to `ItineraryPlannerAgent`, validating the itinerary and route for safety.

In `src/main/java/com/tripplanner/guardrails`, create `TripSafetyGuardrail.java`:

```java title="TripSafetyGuardrail.java"
--8<-- "../../section-3/step-02/src/main/java/com/tripplanner/guardrails/TripSafetyGuardrail.java"
```

Here is what this guardrail does:

### Handling Structured Output

The guardrail receives the raw `AiMessage` that the framework will parse into a `TripPlan` record. Depending on the LLM and configuration, the response may arrive as plain JSON text, as JSON wrapped in markdown code blocks (`` ```json...``` ``), or even as `null` text when the framework uses tool calls for structured output.

The guardrail handles all these cases:

- **Null or blank text**: The framework is using tool calls for structured output — the guardrail returns `success()` and lets the framework handle deserialization
- **JSON in markdown**: The `extractJson()` helper strips any surrounding text and extracts the JSON object
- **Malformed JSON**: The guardrail calls `retry()` — giving the LLM another chance to produce valid output

### Safety Checks

The guardrail validates two rules:

1. **Completeness** — the itinerary must not be empty (a trip plan without days is useless)
2. **Route safety** — the route overview and itinerary descriptions are scanned for dangerous-area keywords

Each failed check calls `retry(feedback)`, which sends the feedback to the LLM and asks it to regenerate. The LLM gets up to `maxRetries` attempts configured on the annotation.

### Audit Logging

Every decision, pass or retry, is logged to the `GuardrailAuditLog`:

```java
auditLog.log("TripSafetyGuardrail", "RETRY", "Total cost estimate is missing");
```

---

## Component 4: The Appropriateness Guardrail

This guardrail enforces **soft rules**. These are violations you can fix by **rewriting** the JSON in code, without asking the LLM to regenerate. It is applied to `VehicleAdvisorAgent`, validating vehicle recommendations against the trip context.

In `src/main/java/com/tripplanner/guardrails`, create `TripAppropriatenessGuardrail.java`:

```java title="TripAppropriatenessGuardrail.java"
--8<-- "../../section-3/step-02/src/main/java/com/tripplanner/guardrails/TripAppropriatenessGuardrail.java"
```

Here is what this guardrail does:

### Context-Aware Validation

The guardrail injects `TripRequestContext` to access the original trip parameters:

```java
TripRequest request = tripRequestContext.get();
```

This enables checks like "is this vehicle appropriate for the number of travelers?" The guardrail can only answer that by comparing the LLM's recommendation against the original request.

### Vehicle/Traveler Mismatch — Rewriting

When the LLM recommends a sports car for 4+ travelers, the guardrail doesn't ask the LLM to try again. Instead, it **rewrites the JSON directly**:

```java
vehicle.put("type", replacement);
vehicle.put("reasoning", "Vehicle upgraded by guardrail: ...");
return successWith(AiMessage.from(root.toString()));
```

The `successWith(AiMessage)` method replaces the LLM's response with the corrected version. The framework then deserializes the modified JSON into the `TripPlan` record as usual.

### Budget Coherence — Reprompt

When the mismatch is too significant to fix in code (luxury brand on economy budget), the guardrail calls `reprompt()`. Unlike `retry()`, which only sends feedback, `reprompt()` also **replaces the system prompt** — giving the LLM stricter instructions for the retry attempt:

```java
return reprompt("The vehicle recommendation is a luxury vehicle but the budget is economy. "
        + "Please recommend an affordable, budget-friendly vehicle instead.",
        "You are a vehicle advisor for road trips. You MUST recommend only budget-friendly, "
        + "affordable vehicles. Never suggest luxury, premium, or sports brands.");
```

---

## Component 5: Wiring Guardrails to Agents

In the multi-agent system from Step 01, each agent has a focused responsibility. Guardrails are applied to individual agents — each guardrail validates the specific output of the agent it's attached to.

### ItineraryPlannerAgent with Safety Guardrail

```java hl_lines="3 10 30" title="ItineraryPlannerAgent.java"
--8<-- "../../section-3/step-02/src/main/java/com/tripplanner/agentic/agents/ItineraryPlannerAgent.java"
```

The `@OutputGuardrails` annotation on `planItinerary()` runs `TripSafetyGuardrail` against the `ItineraryResult` JSON before it's deserialized. If the guardrail calls `retry()`, the LLM regenerates and the guardrail runs again (up to `maxRetries = 3`).

### VehicleAdvisorAgent with Appropriateness Guardrail

```java hl_lines="3 10 28" title="VehicleAdvisorAgent.java"
--8<-- "../../section-3/step-02/src/main/java/com/tripplanner/agentic/agents/VehicleAdvisorAgent.java"
```

The `@OutputGuardrails` annotation on `recommendVehicle()` runs `TripAppropriatenessGuardrail` against the `VehicleRecommendation` JSON. If the vehicle is too small, the guardrail rewrites it in place via `successWith()`. If it's a luxury brand on economy budget, it calls `retry()`.

Guardrails are applied per agent. The safety guardrail checks itinerary content and the appropriateness guardrail checks vehicle suitability. With `maxRetries = 3`, each agent gets up to three chances to fix output when a guardrail triggers a retry. Both agents run in parallel inside `ResearchPhase`, and each guardrail runs independently on its own agent output.

---

## Component 6: Updated REST Endpoint

Update `TripPlannerResource.java` to use `TripPlannerSystem` (the multi-agent pipeline) and populate the `TripRequestContext`:

```java title="TripPlannerResource.java"
--8<-- "../../section-3/step-02/src/main/java/com/tripplanner/resource/TripPlannerResource.java"
```

The important line is:

```java
tripRequestContext.set(request);
```

It runs **before** calling the agent, so the context is available when the guardrails execute.

---

## Component 7: Guardrail Exception Mapper

When the LLM exhausts all retries and the guardrails still fail, an `AgentInvocationException` is thrown (wrapping the underlying `OutputGuardrailException`). Without proper handling, this surfaces as a raw HTTP 500 error.

In `src/main/java/com/tripplanner/resource`, create `GuardrailExceptionMapper.java`:

```java title="GuardrailExceptionMapper.java"
--8<-- "../../section-3/step-02/src/main/java/com/tripplanner/resource/GuardrailExceptionMapper.java"
```

`@Provider` registers the mapper with JAX-RS automatically, with no extra configuration. The mapper walks the exception cause chain to find the underlying `GuardrailException` and returns a structured HTTP 422 response with a JSON body instead of a raw 500 error.

---

## Running the Application

Start the application:

=== "Linux / macOS"
    ```bash
    cd section-3/step-02
    ./mvnw quarkus:dev
    ```

=== "Windows"
    ```cmd
    cd section-3\step-02
    mvnw quarkus:dev
    ```

Open your browser to [http://localhost:8080](http://localhost:8080){target="_blank"}.

---

## Try It Out

### Test 1: Normal Trip (Guardrails Pass)

Fill in the form:

- **Destination**: `Italian Riviera`
- **Duration**: `5` days
- **Travelers**: `4`
- **Trip Type**: `Family Vacation`
- **Budget**: `Moderate (€1,000–€2,500)`

Click **Generate Trip Plan**.

**What happens?**

The multi-agent pipeline runs: `ResearchPhase` (parallel vehicle + itinerary), then `CostEstimatorAgent`, then `TipsGeneratorAgent`. The guardrails run on individual agents. Check your terminal logs — you should see:

```
🛡️ [TripSafetyGuardrail] PASS — All safety checks passed
🛡️ [TripAppropriatenessGuardrail] PASS — All appropriateness checks passed
```

Both guardrails ran and approved their respective agent's output.

### Test 2: Vehicle Rewrite

Try a trip with many travelers and an adventure type (which sometimes triggers a small vehicle recommendation):

- **Travelers**: `6`
- **Trip Type**: `Adventure Trip`
- **Destination**: `Swiss Alps`

If the LLM recommends a compact or sporty vehicle, you'll see:

```
🛡️ [TripAppropriatenessGuardrail] REWRITE — Vehicle type 'sports car' is too small for 6 travelers
```

The vehicle recommendation in the response will show the guardrail-corrected type (SUV) with a note about the upgrade.

### Test 3: Observe Audit Trail

After running a few requests, check your terminal for the full audit trail. Each guardrail decision is logged with a timestamp, the guardrail name, the decision, and the reason.

---

## How It All Works Together

Let's trace through a scenario where the itinerary agent includes a dangerous route:

```mermaid
sequenceDiagram
    participant IP as ItineraryPlannerAgent
    participant LLM as OpenAI GPT-4o
    participant G1 as TripSafetyGuardrail
    participant Audit as GuardrailAuditLog

    IP->>LLM: Plan itinerary
    LLM-->>IP: ItineraryResult JSON (route through conflict area)

    Note over IP: Safety guardrail runs

    IP->>G1: validate(AiMessage)
    G1->>G1: Parse JSON ✓
    G1->>G1: Check itinerary ✓
    G1->>G1: Check route safety ✗ (dangerous keyword!)
    G1->>Audit: log("RETRY", "Dangerous content detected")
    G1-->>IP: retry("Avoid dangerous areas, suggest safe alternatives")

    Note over IP: Retry #1

    IP->>LLM: Regenerate with feedback
    LLM-->>IP: ItineraryResult JSON (safe route)

    IP->>G1: validate(AiMessage)
    G1->>G1: All checks ✓
    G1->>Audit: log("PASS", "All safety checks passed")
    G1-->>IP: success()

    IP-->>IP: Deserialize JSON → ItineraryResult
```

---

## Summary

- **Output guardrails validate LLM responses** before they reach the user, which is critical for production systems
- **`retry()` asks the LLM to regenerate** with feedback, useful for hard safety violations; **`reprompt()` also replaces the system prompt** for stricter control
- **`successWith()` rewrites the response in code**, useful for minor fixups without another LLM call
- **Guardrails are per-agent**: in a multi-agent system, each guardrail validates the output of the specific agent it is attached to
- **Audit trails** log every guardrail decision, enabling compliance and debugging
- **`@ApplicationScoped` context** bridges request parameters to guardrails, using `volatile` for thread-safe access across parallel agent threads

---

## Experiment Further

### 1. Add a Tips Guardrail

Create a `TipsAppropriatenessGuardrail` that filters family-inappropriate content (nightclubs, casinos, bar hopping) from the tips generated by `TipsGeneratorAgent`. Apply it with `@OutputGuardrails` on `TipsGeneratorAgent.generateTips()`. Hint: the LLM response is a JSON array of strings — iterate over the array and remove inappropriate entries using `successWith()`.

### 2. Try Different `reprompt()` Strategies

The `TripAppropriatenessGuardrail` already uses `reprompt()` to replace the system prompt when a luxury vehicle is recommended on an economy budget. Try varying the replacement system prompt — for example, add constraints about vehicle age or fuel efficiency — and observe how the LLM's retry output changes compared to a plain `retry()` with just feedback.

### 3. Test Guardrail Exhaustion

Set `maxRetries = 0` on the `@OutputGuardrails` annotation and trigger a retry scenario. Observe how the `GuardrailExceptionMapper` returns an HTTP 422 error.

### 4. Write a Custom Unit Test

Add a test case to `TripSafetyGuardrailTest` with a JSON payload that contains a dangerous keyword in the itinerary title (not just the description). Does the guardrail catch it? If not, extend the `findDangerousContent` method.

---

## Troubleshooting

??? warning "Error: OPENAI_API_KEY not set"
    Make sure you've exported the environment variable:

    ```bash
    export OPENAI_API_KEY=sk-your-key-here
    ```

    Then restart the application.

??? warning "Guardrail never triggers"
    The LLM usually generates reasonable output. To test guardrails reliably, use the unit tests (`TripSafetyGuardrailTest`, `TripAppropriatenessGuardrailTest`) which construct specific JSON payloads that trigger each rule. In the multi-agent system, each guardrail runs on its respective agent — check the terminal logs for guardrail decisions from both `VehicleAdvisorAgent` and `ItineraryPlannerAgent`.

??? warning "GuardrailException thrown unexpectedly"
    This means all retries were exhausted and the guardrails still failed. Check:

    - The `maxRetries` value on `@OutputGuardrails` (default is 2, we set it to 3)
    - The terminal logs for audit trail entries showing what failed
    - Whether your retry feedback is clear enough for the LLM to fix the issue

---

## What's Next?

In this step you added output guardrails to individual agents in the multi-agent pipeline. You saw how `retry()` asks the LLM to regenerate, how `successWith()` rewrites output in code, how guardrails are distributed across agents in a parallel pipeline, and how an audit trail logs every decision for compliance.

In **Step 03**, you'll wrap the trip planning pipeline in an event-driven Quarkus Flow workflow with Kafka and CloudEvents, adding a human-in-the-loop approval checkpoint that suspends execution without holding any threads.

[Continue to Step 03 - Event-Driven Workflows with Quarkus Flow](step-03.md)
