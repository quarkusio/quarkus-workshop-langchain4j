# Bug Report: Nested `@SequenceAgent` / `@ParallelAgent` Deadlocks in `quarkus-flow-langchain4j`

**Component:** `quarkus-flow-langchain4j` 0.10.2  
**Upstream repo:** https://github.com/quarkiverse/quarkus-flow  
**Severity:** Blocking — any nested agentic pattern silently deadlocks at runtime

---

## Summary

When a `@SequenceAgent` contains a `@ParallelAgent` (or another `@SequenceAgent`) as a sub-agent, invoking the outer agent causes a permanent deadlock. The inner `FlowPlanner` overwrites the outer `FlowPlanner`'s reference in the shared `AgenticScope` execution context. After the inner agent completes, subsequent tasks in the outer workflow read the stale inner planner and put exchanges on a queue that nobody consumes.

---

## Reproducer

### Agents (standard LangChain4j agentic pattern)

```java
// Outer: sequential pipeline
public interface TripPlannerSystem {
    @SequenceAgent(outputKey = "tripPlan",
        subAgents = { ResearchPhase.class, CostEstimatorAgent.class, TipsGeneratorAgent.class })
    TripPlan planTrip(String destination, int days, String tripType,
                      int travelers, String budget, String preferences);
}

// Inner: parallel sub-workflow (one of the sub-agents above)
public interface ResearchPhase {
    @ParallelAgent(outputKey = "researchResult",
        subAgents = { VehicleAdvisorAgent.class, ItineraryPlannerAgent.class })
    void research(String destination, int days, String tripType,
                  int travelers, String budget, String preferences);
}

// Leaf agents — plain @Agent, no nesting
public interface VehicleAdvisorAgent {
    @Agent(outputKey = "vehicle") TripPlan.VehicleRecommendation recommendVehicle(/*...*/);
}
public interface ItineraryPlannerAgent {
    @Agent(outputKey = "itineraryResult") ItineraryResult planItinerary(/*...*/);
}
public interface CostEstimatorAgent {
    @Agent(outputKey = "costs") TripPlan.CostEstimate estimateCosts(/*...*/);
}
```

### Invocation

```java
@Inject TripPlannerSystem tripPlannerSystem;

// This call deadlocks:
tripPlannerSystem.planTrip("Swiss Alps", 7, "family", 2, "$3000", "hiking");
```

### Observed behavior

The `ResearchPhase` (parallel: vehicle + itinerary) completes successfully. The next sequential task (`CostEstimatorAgent`) starts according to the trace log but never makes an LLM call. The application hangs indefinitely.

```
Task 'research-0' completed at ... output=AgenticScope{...}
Task 'estimateCosts-1' started at ... pos=do/1/estimateCosts-1
<-- hangs here forever, no further output -->
```

---

## Root Cause

### 1. Planner reference stored in scope-level execution context

`FlowPlanner.firstAction()` writes `this` into the shared `AgenticScope`:

```java
// FlowPlanner.java line ~35
context.agenticScope().writeExecutionContext(FlowPlanner.class, this);
```

### 2. Nested agent creates a second planner that overwrites the first

When the outer planner dispatches `research-0`, the LangChain4j runtime invokes `ResearchPhase.research()`. Because `quarkus-flow-langchain4j` intercepts `@ParallelAgent` methods, a **new** `FlowPlanner` is created for the inner workflow. That inner planner also calls:

```java
context.agenticScope().writeExecutionContext(FlowPlanner.class, this); // overwrites outer!
```

The key is `FlowPlanner.class` — a single, global key. The inner planner overwrites the outer planner's reference.

### 3. Outer workflow reads the dead inner planner

After the inner planner completes (its exchange queue is drained, `signalTermination()` was called), the outer Flow engine dispatches the next sequential task (`estimateCosts-1`). The task function in `AgenticFlow.executeAgent()` reads:

```java
// AgenticFlow.java
protected Object executeAgent(DefaultAgenticScope scope, int index) {
    FlowPlanner planner = scope.executionContextAs(FlowPlanner.class);
    //          ^^^^^^^ returns the INNER (dead) planner, not the OUTER one
    return planner.executeAgent(index).join();
}
```

This calls `executeAgent(1)` on the **inner** planner, which puts an `AgentExchange` on the inner planner's `agentExchangeQueue`. But the REST/calling thread is blocking on the **outer** planner's queue via `internalNextAction() → agentExchangeQueue.take()`. Nobody consumes the inner queue → deadlock.

### Call sequence diagram

```
REST thread                  ForkJoinPool (outer)          ForkJoinPool (inner)
    │                              │                              │
    ├─ firstAction()               │                              │
    │  scope.write(FP.class,       │                              │
    │              outerPlanner)   │                              │
    │  supplyAsync(start)─────────►│                              │
    │  take() ◄── blocks           │                              │
    │                              ├─ research-0 task             │
    │                              │  executeAgent(0)             │
    │                              │  put(exchange) ──────────────┼─►
    │  ◄────────── wakes up        │  join() ◄── blocks           │
    │  invoke ResearchPhase        │                              │
    │    └─ inner firstAction()    │                              │
    │       scope.write(FP.class,  │                              │
    │                   INNER) ◄── OVERWRITES OUTER               │
    │       supplyAsync(start) ───────────────────────────────────►│
    │       take() on inner queue  │                              │
    │       ... inner completes    │                              │
    │    └─ nextAction() done      │                              │
    │                              │                              │
    │  nextAction() on outer       │                              │
    │  continuation.complete()────►│                              │
    │  take() on OUTER queue       │  (unblocked)                 │
    │           │                  ├─ estimateCosts-1 task         │
    │           │                  │  scope.read(FP.class)         │
    │           │                  │  → gets INNER planner! ◄──── BUG
    │           │                  │  innerPlanner.executeAgent(1) │
    │           │                  │  put on INNER queue           │
    │           │                  │  join() ◄── blocks forever    │
    │           │                  │                               │
    │  ◄── blocks forever          │                               │
    │  (waiting on OUTER queue,    │                               │
    │   exchange was put on INNER) │                               │
    ▼                              ▼                               ▼
                            DEADLOCK
```

---

## Impact

- **Any** nested `@SequenceAgent` / `@ParallelAgent` topology deadlocks when `quarkus-flow-langchain4j` is on the classpath.
- This affects both direct CDI invocations (REST endpoints) and manual Flow `function()` calls via an adapter, since both go through the intercepted CDI proxy.
- Flat (non-nested) agents work fine.

---

## Suggested Fix

The `FlowPlanner` reference should be scoped per workflow level rather than stored globally in the `AgenticScope`. Options:

1. **Stack-based context:** use a `Deque<FlowPlanner>` instead of a single `FlowPlanner` entry, pushing on `firstAction()` and popping on completion.
2. **Unique key per level:** key the execution context by `FlowPlanner.class` + a nesting depth or workflow instance ID.
3. **Thread-local planner:** store the planner in a `ThreadLocal` or the Flow engine's task context instead of the shared `AgenticScope`.

---

## Workaround

Remove `quarkus-flow-langchain4j` from the classpath. The base `quarkus-flow` dependency is sufficient for writing manual `Flow` classes. Without the extension, `@SequenceAgent` / `@ParallelAgent` invocations use LangChain4j's default runtime (which handles nesting correctly). The trade-off is losing the auto-generated workflow visualizations in the Flow Dev UI for the agentic patterns.

---

## Environment

- Quarkus 3.37.2
- quarkus-langchain4j 1.12.0 / langchain4j-agentic 1.1.0.CR1
- quarkus-flow 0.10.2
- quarkus-flow-langchain4j 0.10.2
- Java 21, macOS aarch64
