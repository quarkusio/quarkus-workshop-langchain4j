# Step 01 - Multi-Agent Trip Planner with Skills

## Welcome to Section 3: Enterprise Agentic Patterns

Congratulations on completing Section 2! You've learned how to build AI agents, compose them into workflows (sequential, parallel, conditional, and loop), and orchestrate them with supervisors and planners.

In **Section 3**, we're shifting to a brand-new scenario and exploring **enterprise-grade agentic patterns** — skills, guardrails, persistent state, and more. Instead of managing a car fleet, you'll build an intelligent **Customer Trip Planner** that helps customers plan road trips.

### The Scenario

**Miles of Smiles** offers customers more than just a car — an intelligent Trip Planner helps plan the entire journey. Customers describe their trip (e.g., "7 days on the Italian Riviera, family of 4, moderate budget") and the system picks the right vehicle, plans a route, estimates costs, and provides practical tips.

Different trip types need different planning expertise. For example, a "family vacation" skill knows about kid-friendly stops, rest frequency, and car seat compatibility, while a "business travel" skill optimizes for speed, motorway routes, and professional accommodation. 

---

## What You'll Learn

In this step, you will:

- Build a **multi-agent system** that splits trip planning across specialized agents
- Use `@SequenceAgent` and `@ParallelAgent` to orchestrate agents into a **pipeline with parallel phases**
- Use `@LoopAgent` with `@ExitCondition` to **iteratively refine** the plan until costs fit the budget
- Use `@Output` to **assemble results** from the `AgenticScope` into a final response
- Use `SkillsToolProvider` to **dynamically load expertise** from Markdown files on the filesystem
- Use `@SystemMessageProviderSupplier` to **inject skill knowledge** into specific agents at runtime
- Use `MonitoredAgent` for **built-in observability** across the entire agent pipeline

---

## What Are We Going to Build?

Instead of a single monolithic agent that does everything in one LLM call, we'll split trip planning into specialized agents orchestrated by a sequential pipeline with a **budget-aware feedback loop**:

```
TripPlannerSystem (@SequenceAgent)
│
├─ BudgetReviewInitializer (non-AI) → initializes budgetReview
│
├─ BudgetAwareResearch (@LoopAgent, maxIterations=3)
│   ├─ ResearchPhase (@ParallelAgent)
│   │   ├── VehicleAdvisorAgent    → recommends a vehicle
│   │   └── ItineraryPlannerAgent  → plans day-by-day itinerary
│   ├─ CostEstimatorAgent          → estimates costs
│   └─ BudgetReviewerAgent         → approves or suggests cost reductions
│   @ExitCondition: budgetReview.approved()
│
├─ TipsGeneratorAgent              → generates practical tips
│
└─ @Output                         → assembles the final TripPlan
```

**Why this architecture?**

- **VehicleAdvisorAgent** and **ItineraryPlannerAgent** are independent — they run **in parallel** inside `ResearchPhase`
- **CostEstimatorAgent** needs the vehicle and itinerary to produce realistic estimates — it runs **after** the research phase
- **BudgetReviewerAgent** compares estimated costs to the budget — if costs exceed the budget, it provides specific reduction hints and the loop iterates again
- On subsequent iterations, the research agents see the **cost reduction hints** from the budget review and adjust their recommendations (e.g., cheaper vehicle, budget-friendly accommodation)
- **maxIterations = 3** acts as a safety cap — even if costs never fit, the pipeline moves on after 3 attempts
- **TipsGeneratorAgent** runs after the loop with the final (budget-optimized) plan
- The `@Output` method assembles the final `TripPlan` from scope values — pure Java, no LLM call needed

Each agent has a **focused prompt** producing better results than asking a single LLM call to do everything at once.

The Quarkus DevUI provides an interactive topology view of the entire agent system, showing each agent's type (Sequence, Loop, Parallel, AI, Action), its inputs/outputs, and the data flow between them:

![Agentic System Topology](../images/section-3-topology.png)

**The Flow:**

```mermaid
sequenceDiagram
    participant User as Web UI
    participant REST as TripPlannerResource
    participant Seq as TripPlannerSystem
    participant Init as BudgetReviewInitializer
    participant BAR as BudgetAwareResearch
    participant RP as ResearchPhase
    participant VA as VehicleAdvisorAgent
    participant IP as ItineraryPlannerAgent
    participant CE as CostEstimatorAgent
    participant BR as BudgetReviewerAgent
    participant TG as TipsGeneratorAgent

    User->>REST: POST /trip/plan (TripRequest)
    REST->>Seq: planTrip(destination, days, tripType, ...)

    Seq->>Init: initialize()
    Init-->>Seq: BudgetReview(false, "Plan freely...") → scope["budgetReview"]

    rect rgb(255, 245, 230)
        Note over BAR: Budget-Aware Loop (max 3 iterations)
        loop until budgetReview.approved() or 3 iterations
            rect rgb(230, 245, 255)
                Note over RP: Parallel Phase
                par Vehicle & Itinerary in parallel
                    RP->>VA: recommendVehicle(..., budgetReview)
                    VA-->>RP: VehicleRecommendation → scope["vehicle"]
                and
                    RP->>IP: planItinerary(..., budgetReview)
                    IP-->>RP: ItineraryResult → scope["itineraryResult"]
                end
            end

            BAR->>CE: estimateCosts(vehicle, itineraryResult, ...)
            CE-->>BAR: CostEstimate → scope["costs"]

            BAR->>BR: reviewBudget(costs, budget)
            BR-->>BAR: BudgetReview → scope["budgetReview"]
        end
    end

    Seq->>TG: generateTips(vehicle, itineraryResult, costs, ...)
    TG-->>Seq: List of String → scope["tips"]

    Note over Seq: @Output assembles TripPlan (no LLM)
    Seq-->>REST: TripPlan
    REST-->>User: JSON response → rendered in UI
```

---

## Understanding Skills

In Sections 1 and 2, agents got their behavior from annotations like `@SystemMessage`. While those messages can also reference external files, thus not requiring recompilation in case you want to change them, they nevertheless remain static, as they are not contextualized with the specific user request at runtime.

**Skills** solve this problem by externalizing domain expertise into Markdown files that are loaded at runtime. Each skill is a `SKILL.md` file with YAML frontmatter (name and description) followed by Markdown content:

```markdown
---
name: adventure-trip
description: Instructions for planning adventurous road trips.
---

# Adventure Trip Planning

You are an expert at planning adventurous road trips across Europe...

## Vehicle Recommendations
- Recommend vehicles with good ground clearance...
```

The `quarkus-langchain4j-skills` extension scans one or more directories and makes all discovered skills available to agents via a `SkillsToolProvider` CDI bean. The directories to scan are configured in `application.properties` using the `quarkus.langchain4j.skills.directories` property — each entry can be a filesystem path or a classpath location (prefixed with `classpath:`). See the [Skills extension documentation](https://docs.quarkiverse.io/quarkus-langchain4j/dev/skills.html#_configuration){target="_blank"} for full configuration details. The agent can then inject this knowledge into its system message dynamically.

**Benefits of this approach:**

- **Hot-reload friendly**: Quarkus dev mode picks up changes to skill files automatically
- **Separation of concerns**: Domain experts can author skill content in Markdown without touching Java code
- **Composable**: Skills can be loaded selectively based on context
- **Cost-effective**: Skills are only loaded when needed, reducing LLM token usage

---

## Running the Application

Navigate to the `section-3/step-01` directory and start the application:

=== "Linux / macOS"
    ```bash
    cd section-3/step-01
    ./mvnw quarkus:dev
    ```

=== "Windows"
    ```cmd
    cd section-3\step-01
    mvnw quarkus:dev
    ```

Once started, open your browser to [http://localhost:8080](http://localhost:8080){target="_blank"}.

### Understanding the UI

The application has a two-panel layout:

1. **Left Panel — Trip Form**: Fields for destination, duration, number of travelers, trip type (family, business, adventure, romantic, road trip), budget range, and additional preferences.
2. **Right Panel — Trip Plan**: Displays the generated plan with vehicle recommendation, route overview, daily itinerary, cost estimates, and practical tips.
3. **Bottom — Refine Drawer**: A text input to send follow-up adjustments (e.g., "skip Florence", "add a lake day"), which re-submits the form with the refinement as the preferences.

---

## Try It Out

Let's see the multi-agent system in action!

### Test 1: Family Beach Vacation

Fill in the form with:

- **Destination**: `Italian Riviera`
- **Duration**: `5` days
- **Travelers**: `4`
- **Trip Type**: `Family Vacation`
- **Budget**: `Moderate (€1,000–€2,500)`
- **Preferences**: `We love coastal towns and good food`

Click **Generate Trip Plan**.

**What happens?**

- `BudgetReviewInitializer` writes a neutral `BudgetReview` to scope — "Plan freely within the given budget range"
- The `BudgetAwareResearch` loop starts: `ResearchPhase` runs `VehicleAdvisorAgent` and `ItineraryPlannerAgent` **in parallel** — both load the `family-trip` skill for family-specific expertise
- `CostEstimatorAgent` reads the vehicle and itinerary from the `AgenticScope` to produce cost estimates
- `BudgetReviewerAgent` compares costs to the budget — with a moderate budget and a family trip, costs should fit on the first iteration
- `TipsGeneratorAgent` then reads everything from scope to generate relevant tips
- Finally, the `@Output` method on `TripPlannerSystem` assembles all outputs into a `TripPlan` — pure Java, no LLM call
- The UI renders the vehicle recommendation, a day-by-day itinerary with overnight stops, cost estimates, and practical tips

### Test 2: Adventure Trip

Now try a different trip type:

- **Destination**: `Swiss Alps`
- **Trip Type**: `Adventure Trip`
- **Preferences**: `We want hiking and mountain passes`

**What happens?**

- The `VehicleAdvisorAgent` uses the `adventure-trip` skill and recommends a 4WD vehicle
- The `ItineraryPlannerAgent` uses the same skill to plan routes over legendary roads like Furka Pass with outdoor activities
- Notice how the vehicle recommendation, route, and tips are completely different from the family trip — the skill shapes each agent's response

### Test 3: Economy Budget (Trigger the Budget Loop)

Try a trip that pushes the budget:

- **Destination**: `French Riviera`
- **Duration**: `7` days
- **Travelers**: `4`
- **Trip Type**: `Adventure Trip`
- **Budget**: `Economy (under €1,000)`
- **Preferences**: `We want to explore the coastline and mountain villages`

**What happens?**

- The first iteration of the budget loop plans a 7-day adventure trip — likely suggesting a 4WD vehicle and varied activities, resulting in costs well above €1,000
- `BudgetReviewerAgent` rejects the plan and provides hints like "downgrade to a compact car" and "choose hostels or camping over hotels"
- On the second iteration, `VehicleAdvisorAgent` and `ItineraryPlannerAgent` see the cost reduction hints and adjust — a smaller vehicle, budget-friendly accommodation, and free activities
- If costs still exceed the budget, a third iteration refines further. After 3 iterations, the loop exits regardless (`maxIterations = 3`)
- Check the terminal logs: you'll see multiple rounds of vehicle/itinerary/cost/review LLM calls, with the later rounds reflecting the budget feedback

### Test 4: Refine the Plan

With a plan displayed, use the **Refine** input at the bottom:

```
Skip the first day in Zermatt and add a day in Verbier instead
```

The entire pipeline re-runs with your refinement, generating a new plan incorporating your adjustment.

---

## Project Dependencies

Open the `pom.xml` file. The key dependencies for this step are:

```xml
<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-agentic</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-openai</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-skills</artifactId>
</dependency>
```

- `quarkus-langchain4j-agentic`: The agent framework with `@Agent`, `@SequenceAgent`, `@ParallelAgent`, `@LoopAgent`, `@ExitCondition`, `@SystemMessageProviderSupplier`, and workflow support
- `quarkus-langchain4j-openai`: OpenAI model provider (GPT-4o)
- `quarkus-langchain4j-skills`: The skills extension that loads `SKILL.md` files and provides `SkillsToolProvider`

---

## The Leaf Agents

The trip planning work is split across six specialized agents (four AI agents, one non-AI initializer, and one budget reviewer). Each agent has a focused responsibility and communicates with others through the `AgenticScope` — a shared key-value store where each agent writes its output under an `outputKey` and reads inputs from keys written by previous agents.

### VehicleAdvisorAgent — Picking the Right Car

```java title="VehicleAdvisorAgent.java"
--8<-- "../../section-3/step-01/src/main/java/com/tripplanner/agentic/agents/VehicleAdvisorAgent.java"
```

**Key points:**

- `outputKey = "vehicle"` — the recommended vehicle is stored in scope under this key, so downstream agents can read it
- Returns `TripPlan.VehicleRecommendation` — a nested record with `type`, `model`, and `reasoning`
- Has `@SystemMessageProviderSupplier` to load skills — the `adventure-trip` skill recommends 4WD vehicles while the `family-trip` skill recommends spacious MPVs
- Accepts a `BudgetReview budgetReview` parameter — on the first loop iteration this is a neutral "plan freely" message; on subsequent iterations it contains specific cost reduction hints from the `BudgetReviewerAgent` (e.g., "downgrade to a compact car")
- Does **not** receive `days` — vehicle selection doesn't depend on trip duration

### ItineraryPlannerAgent — Planning the Route

```java title="ItineraryPlannerAgent.java"
--8<-- "../../section-3/step-01/src/main/java/com/tripplanner/agentic/agents/ItineraryPlannerAgent.java"
```

**Key points:**

- `outputKey = "itineraryResult"` — stores an `ItineraryResult` (route overview + list of day itineraries) in scope
- Also has `@SystemMessageProviderSupplier` for skills — the `adventure-trip` skill suggests legendary driving roads like Stelvio Pass, while the `family-trip` skill recommends kid-friendly stops every 2-3 hours
- Accepts a `BudgetReview budgetReview` parameter — on subsequent loop iterations, cost reduction hints guide the planner toward budget-friendly choices (e.g., "choose hostels over hotels", "prefer free activities")
- Does **not** receive `travelers` or `budget` — those are cost concerns, not route planning concerns

!!! note "Why ItineraryResult instead of separate fields?"
    Each agent can only write a single value to the scope via its `outputKey`. Since the itinerary planner produces both a route overview (String) and a day-by-day itinerary (List), they're grouped into an `ItineraryResult` record:

    ```java title="ItineraryResult.java"
    --8<-- "../../section-3/step-01/src/main/java/com/tripplanner/model/ItineraryResult.java"
    ```

### CostEstimatorAgent — Crunching the Numbers

```java title="CostEstimatorAgent.java"
--8<-- "../../section-3/step-01/src/main/java/com/tripplanner/agentic/agents/CostEstimatorAgent.java"
```

**Key points:**

- Reads `vehicle` and `itineraryResult` from the scope — these were written by the parallel research phase
- `outputKey = "costs"` — stores a `TripPlan.CostEstimate` with per-category breakdowns
- **No skills needed** — cost estimation is a reasoning task based on the concrete vehicle and itinerary data already in scope
- This agent **must run after** the research phase because it depends on its outputs

### TipsGeneratorAgent — Practical Travel Advice

```java title="TipsGeneratorAgent.java"
--8<-- "../../section-3/step-01/src/main/java/com/tripplanner/agentic/agents/TipsGeneratorAgent.java"
```

**Key points:**

- Reads `vehicle`, `itineraryResult`, `costs`, `tripType`, and `preferences` from scope — the most comprehensive view
- Returns `List<String>` — LangChain4j instructs the LLM to produce a JSON array and deserializes it
- Runs last among the AI agents, after costs are estimated, so tips can reference specific budget advice

### BudgetReviewerAgent — Budget Compliance Check

```java title="BudgetReviewerAgent.java"
--8<-- "../../section-3/step-01/src/main/java/com/tripplanner/agentic/agents/BudgetReviewerAgent.java"
```

**Key points:**

- Reads `costs` (CostEstimate) and `budget` (String) from scope — compares the estimated total against the stated budget range
- `outputKey = "budgetReview"` — writes a `BudgetReview` record with `approved` (boolean) and `hints` (String)
- If costs fit within the budget, sets `approved = true` — the `@ExitCondition` in `BudgetAwareResearch` exits the loop
- If costs exceed the budget, sets `approved = false` and provides **specific, actionable hints** (e.g., "downgrade from BMW X5 to a compact SUV", "choose hostels instead of hotels") — the research agents read these hints on the next iteration

### BudgetReviewInitializer — Seeding the Loop State

```java title="BudgetReviewInitializer.java"
--8<-- "../../section-3/step-01/src/main/java/com/tripplanner/agentic/agents/BudgetReviewInitializer.java"
```

**Key points:**

- A **non-AI agent** — a concrete Java class with a `public static` method annotated with `@Agent`. No LLM call is made.
- Writes a neutral `BudgetReview(false, "No previous budget review. Plan freely within the given budget range.")` to scope before the loop starts
- **Solves the first-iteration problem**: without this initializer, `VehicleAdvisorAgent` and `ItineraryPlannerAgent` would throw `MissingArgumentException` on the first loop iteration when trying to read `budgetReview` from scope (it hasn't been written yet by `BudgetReviewerAgent`)
- The `approved = false` value doesn't matter for the first iteration — the exit condition is only checked **after** each iteration completes

---

## The Workflow Orchestration

### ResearchPhase — Parallel Execution

The `VehicleAdvisorAgent` and `ItineraryPlannerAgent` don't depend on each other — choosing a vehicle and planning a route are independent tasks. `@ParallelAgent` runs them simultaneously:

```java title="ResearchPhase.java"
--8<-- "../../section-3/step-01/src/main/java/com/tripplanner/agentic/workflow/ResearchPhase.java"
```

**Key points:**

- `@ParallelAgent` runs both sub-agents concurrently — the wall-clock time is the **slower** of the two, not the sum
- Both sub-agents write to their own scope keys (`vehicle` and `itineraryResult`) — these persist in the `AgenticScope` for downstream agents
- The `@Output` method reads both results from scope and produces a summary string — this is stored under `researchComplete`, but downstream agents read the individual keys directly
- The method parameters include `BudgetReview budgetReview` — this ensures the budget review feedback is available in scope for both sub-agents during the loop

### BudgetAwareResearch — The Iterative Refinement Loop

The `@LoopAgent` wraps the research and cost estimation phases into a feedback loop that iterates until costs fit the budget:

```java title="BudgetAwareResearch.java"
--8<-- "../../section-3/step-01/src/main/java/com/tripplanner/agentic/workflow/BudgetAwareResearch.java"
```

**Key points:**

- `@LoopAgent` with `maxIterations = 3` — runs its sub-agents in sequence (`ResearchPhase` → `CostEstimatorAgent` → `BudgetReviewerAgent`) up to 3 times
- `@ExitCondition` — a static method that reads `budgetReview` from scope and returns `true` when `budgetReview.approved()`. The loop exits early when costs fit the budget.
- On each iteration, the sub-agents overwrite the previous values in scope — so `vehicle`, `itineraryResult`, `costs`, and `budgetReview` always reflect the latest attempt
- The `BudgetReview` written by `BudgetReviewerAgent` at the end of each iteration becomes the input for the research agents on the **next** iteration — closing the feedback loop

**How the loop works:**

1. **Iteration 1**: Research agents plan freely (neutral `budgetReview`), costs are estimated, budget reviewer checks
2. If costs fit → `BudgetReview(true, ...)` → exit condition fires → loop exits
3. If over budget → `BudgetReview(false, "Reduce vehicle cost, choose budget accommodation...")` → loop continues
4. **Iteration 2**: Research agents see the hints and adjust (cheaper vehicle, budget-friendly stops), new costs estimated, reviewed again
5. Repeat up to 3 iterations — the safety cap ensures the pipeline always moves forward

### TripPlannerSystem — The Full Pipeline

The top-level orchestrator chains everything together:

```java title="TripPlannerSystem.java"
--8<-- "../../section-3/step-01/src/main/java/com/tripplanner/agentic/workflow/TripPlannerSystem.java"
```

**Key points:**

- `@SequenceAgent` executes sub-agents in order: `BudgetReviewInitializer` → `BudgetAwareResearch` → `TipsGeneratorAgent`
- `BudgetReviewInitializer` seeds the scope with a neutral `BudgetReview` before the loop starts
- `BudgetAwareResearch` is the `@LoopAgent` that iterates on research/costs/review until the budget is satisfied (or 3 iterations)
- `TipsGeneratorAgent` runs after the loop with the final, budget-optimized plan
- The `planTrip` method signature matches the original API — same parameters, same `TripPlan` return type
- The `@Output` method assembles the final `TripPlan` from scope values — parameters `vehicle`, `itineraryResult`, `costs`, and `tips` are resolved by name from the `AgenticScope`. This is pure Java, saving an LLM call for simple data assembly.
- Extends `MonitoredAgent` — the framework automatically creates an `AgentMonitor` that records the full invocation tree (timing, tokens, inputs/outputs for every sub-agent), accessible via `agentMonitor()`. The monitor propagates to all sub-agents automatically.
- Quarkus auto-registers this as a CDI bean — just `@Inject` it in the REST resource

---

## The Structured Output Model

The `TripPlan` record defines the structured output schema that all agents contribute to:

```java title="TripPlan.java"
--8<-- "../../section-3/step-01/src/main/java/com/tripplanner/model/TripPlan.java"
```

**Key Points:**

- **Nested records**: `VehicleRecommendation`, `DayItinerary`, and `CostEstimate` are nested within `TripPlan`, providing a clean, hierarchical structure
- **Type-safe**: The compiler ensures each agent returns a properly structured component — no manual JSON parsing
- **Automatic deserialization**: LangChain4j handles converting each LLM's JSON response into the corresponding record

The `BudgetReview` record is used for the feedback loop between the `BudgetReviewerAgent` and the research agents:

```java title="BudgetReview.java"
--8<-- "../../section-3/step-01/src/main/java/com/tripplanner/model/BudgetReview.java"
```

- `approved` — `true` if costs fit the budget, `false` if over budget
- `hints` — specific cost reduction suggestions when over budget, or a confirmation message when approved

---

## The REST Endpoint

The `TripPlannerResource` exposes the multi-agent system as a REST API:

```java title="TripPlannerResource.java"
--8<-- "../../section-3/step-01/src/main/java/com/tripplanner/resource/TripPlannerResource.java"
```

**Key Points:**

- The `TripPlannerSystem` is injected as a CDI bean — Quarkus generates the implementation for all agents and workflows automatically
- A single `POST /trip/plan` endpoint receives a `TripRequest` and returns a `TripPlan`
- The REST layer is unaware of the multi-agent architecture — it just calls `planTrip()` and gets back a `TripPlan`

---

## The Skill Files

The application ships with three skill files in `src/main/resources/skills/`:

```java title="skills/family-trip/SKILL.md"
--8<-- "../../section-3/step-01/src/main/resources/skills/family-trip/SKILL.md"
```

Each skill follows the same structure:

- **YAML frontmatter**: `name` and `description` — used by `SkillsToolProvider` to index and present skills
- **Markdown body**: The actual expertise — vehicle recommendations, route planning guidelines, accommodation tips, and practical considerations

The other two skills (`adventure-trip/SKILL.md` and `business-trip/SKILL.md`) follow the same pattern with expertise tailored to their trip types.

Only the `VehicleAdvisorAgent` and `ItineraryPlannerAgent` load skills — they're the agents that benefit from trip-type-specific domain knowledge. The `CostEstimatorAgent` and `TipsGeneratorAgent` operate on the structured data already in scope and don't need skills.

---

## Configuration

The `application.properties` file configures the LLM and the skills directory:

```properties title="application.properties"
--8<-- "../../section-3/step-01/src/main/resources/application.properties"
```

**Key settings:**

- `quarkus.langchain4j.openai.chat-model.model-name=gpt-4o`: Uses GPT-4o for high-quality trip plans
- `quarkus.langchain4j.openai.chat-model.temperature=0.7`: Higher temperature for creative, varied trip suggestions
- `quarkus.langchain4j.openai.timeout=120`: Generous timeout per agent call — each agent's call is faster than the monolithic approach, but there are multiple calls
- `quarkus.langchain4j.skills.directories=classpath:skills`: Tells the skills extension where to find `SKILL.md` files

---

## How It All Works Together

Let's trace through a complete example:

### Scenario: Family Trip to the Italian Riviera (Within Budget)

```mermaid
sequenceDiagram
    participant User as Web UI
    participant REST as TripPlannerResource
    participant Seq as TripPlannerSystem
    participant Init as BudgetReviewInitializer
    participant VA as VehicleAdvisorAgent
    participant IP as ItineraryPlannerAgent
    participant Skills as SkillsToolProvider
    participant LLM as OpenAI GPT-4o
    participant CE as CostEstimatorAgent
    participant BR as BudgetReviewerAgent
    participant TG as TipsGeneratorAgent

    User->>REST: POST /trip/plan<br/>{destination: "Italian Riviera",<br/>days: 5, tripType: "family", ...}
    REST->>Seq: planTrip("Italian Riviera", 5, "family", 4, "moderate", "coastal towns")

    Note over Seq: Step 1: Initialize loop state (no LLM)
    Seq->>Init: initialize()
    Init-->>Seq: BudgetReview(false, "Plan freely...") → scope["budgetReview"]

    rect rgb(255, 245, 230)
        Note over Seq: Step 2: Budget-Aware Loop (iteration 1)

        par VehicleAdvisorAgent
            VA->>Skills: Load skills
            Skills-->>VA: family-trip, adventure-trip, business-trip
            VA->>LLM: "Recommend a vehicle... Budget review: Plan freely..."
            LLM-->>VA: {type: "MPV", model: "VW Multivan", reasoning: "..."}
            Note over VA: → scope["vehicle"]
        and ItineraryPlannerAgent
            IP->>Skills: Load skills
            Skills-->>IP: family-trip, adventure-trip, business-trip
            IP->>LLM: "Plan a 5-day itinerary... Budget review: Plan freely..."
            LLM-->>IP: {routeOverview: "...", itinerary: [{day: 1, ...}, ...]}
            Note over IP: → scope["itineraryResult"]
        end

        CE->>LLM: "Estimate costs for VW Multivan on this itinerary..."
        LLM-->>CE: {vehiclePerDay: "€120/day", fuel: "€180", total: "€1,800"}
        Note over CE: → scope["costs"]

        BR->>LLM: "Compare €1,800 total against moderate budget (€1,000-€2,500)..."
        LLM-->>BR: {approved: true, hints: "Costs fit within the moderate budget"}
        Note over BR: → scope["budgetReview"]
        Note over Seq: @ExitCondition: budgetReview.approved() = true → exit loop
    end

    Note over Seq: Step 3: Tips
    TG->>LLM: "Generate practical tips for this family trip..."
    LLM-->>TG: ["Book toll telepass in advance", "Pack snacks...", ...]
    Note over TG: → scope["tips"]

    Note over Seq: @Output assembles TripPlan (no LLM)
    Seq-->>REST: TripPlan
    REST-->>User: JSON response → rendered in UI
```

**Key Points:**

1. `BudgetReviewInitializer` seeds the scope with a neutral `BudgetReview` — no LLM call, just a Java method
2. The `BudgetAwareResearch` loop runs `ResearchPhase` → `CostEstimatorAgent` → `BudgetReviewerAgent` in sequence
3. Inside `ResearchPhase`, `VehicleAdvisorAgent` and `ItineraryPlannerAgent` run **in parallel** — both load skills via `@SystemMessageProviderSupplier`
4. `BudgetReviewerAgent` approves the costs on the first iteration (moderate budget is sufficient), so the `@ExitCondition` fires and the loop exits after one pass
5. If the budget were tighter (e.g., "economy"), the reviewer would reject and provide cost reduction hints — the loop would iterate, with research agents adjusting their recommendations based on the feedback
6. The `@Output` method on `TripPlannerSystem` assembles all outputs into a `TripPlan` with pure Java — no LLM call wasted on data assembly
7. The REST layer sees a single `planTrip()` call returning a `TripPlan` — the multi-agent orchestration is transparent

Since `TripPlannerSystem` extends `MonitoredAgent`, the Quarkus DevUI provides a full execution trace — showing every agent invocation with its duration, token usage, timeline, inputs, and outputs:

![Agentic System Execution](../images/section-3-execution.png)

Notice how `recommendVehicle` and `planItinerary` run in parallel (overlapping timelines), each activating the `family-trip` skill via tool calls. Also see how both these agents call the `activate_skill` tool in order to enrich the system message with the most appropiate skill for the current task. The `BudgetReviewerAgent` approves the costs on the first iteration (`approved=true`), so the loop exits after a single pass. The entire pipeline completes in ~30 seconds with ~3.8k tokens across 5 LLM calls.

---

## Key Takeaways

- **Multi-agent pipelines**: `@SequenceAgent` and `@ParallelAgent` let you split complex tasks into focused agents with clear data dependencies
- **Iterative refinement with `@LoopAgent`**: `@LoopAgent` with `@ExitCondition` enables feedback loops — agents iterate until a condition is met (e.g., costs fit the budget) or a safety cap (`maxIterations`) is reached
- **Non-AI agents**: Concrete Java classes with `@Agent`-annotated static methods can participate in workflows without making LLM calls — useful for initialization, data transformation, or validation
- **AgenticScope**: Agents exchange data through a shared scope — each agent writes its output under an `outputKey` and reads inputs by parameter name. In loops, each iteration overwrites the previous values, enabling the feedback pattern.
- **`@Output` for assembly**: The `@Output` static method on a workflow reads sub-agent results from the scope and combines them into a final return value — pure Java, no LLM call wasted on data assembly
- **Skills externalize expertise**: Domain knowledge lives in Markdown files, not in code — making agents modular and extensible
- **Dynamic system messages**: `@SystemMessageProviderSupplier` builds the agent's context at runtime, picking up new skills without recompilation
- **Structured output**: Returning record types (`VehicleRecommendation`, `ItineraryResult`, `CostEstimate`, `BudgetReview`) gives you type-safe, well-structured responses from each agent
- **Built-in observability**: Extending `MonitoredAgent` gives you a full invocation tree with timing, token counts, and inputs/outputs across all sub-agents — no extra wiring needed
- **Transparent orchestration**: The REST layer is unaware of the multi-agent architecture — composition happens inside the agent framework

---

## Experiment Further

### 1. Add a New Skill

Create a new skill file at `src/main/resources/skills/romantic-trip/SKILL.md`:

```markdown
---
name: romantic-trip
description: Instructions for planning romantic getaway road trips.
---

# Romantic Trip Planning

You are an expert at planning romantic road trips across Europe...

## Vehicle Recommendations
- Recommend convertibles or sporty coupés for scenic coastal drives...
```

Restart the application (or let Quarkus dev mode hot-reload) and try a trip with the **Romantic Getaway** type. Does the agent use your new skill?

### 2. Test Different Trip Types with the Same Destination

Try planning a trip to the **Swiss Alps** three times — once as `Family Vacation`, once as `Adventure Trip`, and once as `Business Travel`. Compare how the vehicle recommendations, routes, and tips change based on the skill.

### 3. Inspect the Loop Behavior

Since `log-requests` and `log-responses` are enabled in `application.properties`, check your terminal logs to see:

- On each loop iteration: two parallel LLM calls (VehicleAdvisor + ItineraryPlanner), followed by CostEstimator, then BudgetReviewer
- With a moderate budget, you should see a single iteration — the BudgetReviewer approves on the first pass
- With an economy budget, look for multiple rounds — the BudgetReviewer's hints change the vehicle and itinerary recommendations in subsequent iterations
- The VehicleAdvisor and ItineraryPlanner requests should have **overlapping timestamps** within each iteration, confirming parallel execution

### 4. Compare Prompt Sizes

Look at the LLM requests in the logs. Notice how each agent's prompt is **smaller and more focused** than a single monolithic prompt would be — the vehicle prompt doesn't mention costs, the cost prompt doesn't mention route preferences. Focused prompts tend to produce higher-quality outputs.

---

## Troubleshooting

??? warning "Error: OPENAI_API_KEY not set"
    Make sure you've exported the environment variable:

    ```bash
    export OPENAI_API_KEY=sk-your-key-here
    ```

    Then restart the application.

??? warning "Response takes too long or times out"
    The multi-agent pipeline makes at least 5 LLM calls per iteration (2 in parallel + 3 sequential: cost estimator, budget reviewer, tips generator). If the budget loop iterates, each additional round adds 4 more LLM calls (2 parallel research + cost estimator + budget reviewer). With `maxIterations = 3`, the worst case is 13 LLM calls. If you're getting timeouts:

    - Check the `quarkus.langchain4j.openai.timeout` value in `application.properties` (default is 120 seconds per call)
    - Ensure your internet connection is stable
    - Try a shorter trip (fewer days = less content to generate)
    - Try a generous budget to avoid loop iterations while testing

??? warning "Skills not being loaded"
    - Verify that `quarkus.langchain4j.skills.directories=classpath:skills` is set in `application.properties`
    - Check that skill files are named `SKILL.md` (case-sensitive) and placed in subdirectories under `src/main/resources/skills/`
    - Each `SKILL.md` must have valid YAML frontmatter with `name` and `description` fields
    - Only `VehicleAdvisorAgent` and `ItineraryPlannerAgent` load skills — the other agents don't use `@SystemMessageProviderSupplier`

---

## Cleanup

Before moving to the next step, let's clean up:

1. **Stop the running server** by pressing `Ctrl+C` in the terminal where Quarkus is running

2. **Return to the root project directory**:

    ```bash
    cd ..
    ```

---

## What's Next?

In this step, you built a **multi-agent Trip Planner** that splits planning across specialized agents, orchestrated by `@SequenceAgent`, `@ParallelAgent`, and `@LoopAgent`. You saw how agents exchange data through the `AgenticScope`, how a `@LoopAgent` with `@ExitCondition` enables iterative budget refinement, how non-AI agents initialize shared state, how skills provide dynamic domain expertise, how `@Output` handles data assembly without wasting LLM calls, and how `MonitoredAgent` provides built-in observability.

In **Step 02**, you'll learn how to add **guardrails and compliance checks** — ensuring that trip recommendations are safe, honest, and appropriate before they reach the customer!

[Continue to Step 02 - Guardrails and Compliance](step-02.md)
