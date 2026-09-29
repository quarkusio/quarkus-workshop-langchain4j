# Step 03 - Voting, Loops, and Adaptive Model Selection

The guardrails from Step 02 catch obviously unsuitable recommendations, like suggesting a sports car for a family of five. They cannot however tell us whether the accepted vehicle is actually the *best* choice. E.g. is it comfortable enough for a long road trip? Does it fit the budget? Is it fuel-efficient for the planned route?

In this step we'll add three evaluator agents that each score the vehicle recommendation, combine their scores with a voting pattern, and hand the result to a refinement loop where a reviser agent improves the recommendation until it's good enough. We'll also add adaptive model selection, so the reviser starts on a lightweight model and only moves to a more capable one when the recommendation is nearly there.

## Parallel assessment with the voting pattern

The voting pattern sends the same question to several agents in parallel and collects their answers. The difference from a plain parallel workflow is the last step, where a voting strategy combines the answers into one decision. It could average the scores, take a majority, or apply whatever logic you like.

Voting spreads the judgement across agents that each have one narrow job. An agent that only thinks about cost will catch cost problems that a general-purpose evaluator might quietly trade away for extra legroom. Each agent's score is also visible on its own, so when a vehicle fails you can see exactly who voted against it.

```mermaid
flowchart LR
    accTitle: Voting pattern with three evaluators
    accDescr: The vehicle recommendation is sent to three evaluators in parallel. Each returns a score and suggestions. A voting strategy aggregates the scores into a single evaluation.
    Vehicle[Vehicle recommendation] --> E1[Comfort evaluator]
    Vehicle --> E2[Cost evaluator]
    Vehicle --> E3[Fuel efficiency evaluator]
    E1 -->|score + suggestions| Agg[Voting strategy]
    E2 -->|score + suggestions| Agg
    E3 -->|score + suggestions| Agg
    Agg --> Result[Aggregated evaluation]

    classDef evaluator fill:#e3f2fd,stroke:#1565c0,color:#0d3b66
    classDef strategy fill:#fff3e0,stroke:#b56500,color:#593200
    class E1,E2,E3 evaluator
    class Agg strategy
```

We'll implement this with the `VotingPlanner` provided by the LangChain4j agentic patterns module. The planner starts all evaluator subagents in parallel and records each result as a vote when that evaluator completes. Once every evaluator has reported, it passes the votes to a `VotingStrategy` for aggregation.

## Iterative refinement with @LoopAgent

A single round of voting tells us how good the recommendation is, but it doesn't make it any better. For that we need a loop: the evaluators vote, and if the score is too low, a reviser agent improves the recommendation and the evaluators vote again. A numeric score and an explicit exit condition also make quality something you can write a test for.

```mermaid
flowchart TD
    accTitle: Vehicle review loop
    accDescr: The evaluators vote on the vehicle from the research phase. If the average score reaches 7.5, the loop exits and the vehicle goes to cost estimation. Otherwise the reviser improves the recommendation and the evaluators vote again. If the score is still too low after three revisions, the request fails with quality_not_met.
    Start[Vehicle from research phase] --> Eval[VehicleEvaluators vote]
    Eval --> Check{Average score ≥ 7.5?}
    Check -->|Yes| Cost[CostEstimatorAgent]
    Check -->|No, revisions left| Revise[VehicleReviser improves the vehicle]
    Revise --> Eval
    Check -->|No, 3 revisions used| Fail[quality_not_met error]

    classDef loop fill:#e8f5e9,stroke:#2e7d32,color:#16351a
    classDef check fill:#fff3e0,stroke:#b56500,color:#593200
    classDef fail fill:#ffebee,stroke:#c62828,color:#5f1111
    class Eval,Revise loop
    class Check check
    class Fail fail
```

The `@LoopAgent` annotation turns this cycle into an agent with a maximum number of iterations, and an `@ExitCondition` method decides when to stop. The check runs right after the evaluators vote, before the reviser gets a turn, so a recommendation that already scores 7.5 or more goes straight to cost estimation untouched. If the reviser has had three attempts and the evaluators still aren't convinced, the loop gives up and the planner returns a `quality_not_met` error. Miles of Smiles management would rather tell a customer "we couldn't find a car we're proud of" than hand over the keys to a dud. Mostly.

## Adaptive model selection with @ChatModelSupplier

Not every revision needs the most expensive model. While the recommendation is still rough, a smaller model can make broad improvements just fine, for a fraction of the price. Once the score is close to the threshold and the reviser is only polishing, it's worth paying for a more capable model. The finance department at Miles of Smiles was very keen on this part.

The `@ChatModelSupplier` annotation on the reviser agent delegates model selection to a `DynamicModelSelector` CDI bean. This bean injects both the base model (`gpt-4o-mini`) and an enhanced model (`gpt-4o`) and chooses between them based on the current evaluation score.

| Evaluation score | Model selected | Rationale |
|---|---|---|
| ≤ 6.0 | `gpt-4o-mini` (base) | Broad improvements still needed, so a lighter model suffices |
| > 6.0 | `gpt-4o` (enhanced) | Fine-tuning a near-ready recommendation benefits from more capability |

If you did Section 2, this will look familiar. [Section 2 Step 07](../section-2/step-07.md) used the same pattern to pick a model based on the value of the car.

## Prepare the working copy

As always, you can keep building on the previous step or read along with the finished solution.

=== "Option 1: Continue from Step 02"

    ==Keep working in your Step 02 working copy and apply the changes below.== If you get stuck, compare your code with `section-3/step-03`.

=== "Option 2: Use the completed Step 03 project"

    ==Copy `section-3/step-03` to a working directory and open that copy.== It already contains the changes below, so you can read through the code and join in at [Inspecting the voting loop](#inspecting-the-voting-loop).

## Add the vehicle evaluation model

The evaluator agents need a shared return type to represent their assessment.

==Create `src/main/java/com/tripplanner/model/VehicleEvaluation.java`:==

```java title="VehicleEvaluation.java"
--8<-- "../../section-3/step-03/src/main/java/com/tripplanner/model/VehicleEvaluation.java"
```

Each evaluator will return a score between 1 and 10, along with textual suggestions for improvement. The voting strategy will average the scores and concatenate the suggestions.

## Create the evaluator agents

Each evaluator cares about exactly one thing: comfort, cost, or fuel efficiency. Very good at their jobs, and terrible company on a road trip.

==Create `src/main/java/com/tripplanner/agentic/agents/ComfortEvaluator.java`:==

```java title="ComfortEvaluator.java"
--8<-- "../../section-3/step-03/src/main/java/com/tripplanner/agentic/agents/ComfortEvaluator.java"
```

==Create `src/main/java/com/tripplanner/agentic/agents/CostEvaluator.java`:==

```java title="CostEvaluator.java"
--8<-- "../../section-3/step-03/src/main/java/com/tripplanner/agentic/agents/CostEvaluator.java"
```

==Create `src/main/java/com/tripplanner/agentic/agents/FuelEfficiencyEvaluator.java`:==

```java title="FuelEfficiencyEvaluator.java"
--8<-- "../../section-3/step-03/src/main/java/com/tripplanner/agentic/agents/FuelEfficiencyEvaluator.java"
```

- Each evaluator uses `@Agent` with a unique `outputKey`, so its individual score remains in the workflow scope next to the aggregated result.
- The evaluators take the current `vehicle` recommendation from the scope, plus trip context parameters for their specific assessment dimension.
- All three return `VehicleEvaluation`, the same record type, so the aggregation strategy can process them uniformly.

## Add the voting planner

LangChain4j provides ready-made planners for common orchestration patterns in its `langchain4j-agentic-patterns` module, and its `VotingPlanner` does exactly what the evaluators need. The Quarkus LangChain4j BOM already manages the module's version, so the dependency does not need a `<version>` element.

==Add the agentic patterns dependency to `pom.xml`:==

```xml title="pom.xml (agentic patterns dependency)"
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-agentic-patterns</artifactId>
</dependency>
```

The planner only coordinates the evaluators, while the decision about how to combine their votes comes from a `VotingStrategy`. This is a functional interface that receives the collected votes and returns the aggregate, so any lambda or method reference can act as a strategy. The library includes `majority()`, `average()`, and `highest()` strategies for votes that are plain values such as labels or numbers. Our evaluators return a `VehicleEvaluation` that combines a score with suggestions, so we'll write our own strategy that averages the scores and keeps every evaluator's suggestions for the reviser.

## Wire evaluators with @PlannerAgent

The `@PlannerAgent` annotation connects the evaluator subagents to the voting planner through a `@PlannerSupplier` method.

==Create `src/main/java/com/tripplanner/agentic/workflow/VehicleEvaluators.java`:==

```java title="VehicleEvaluators.java"
--8<-- "../../section-3/step-03/src/main/java/com/tripplanner/agentic/workflow/VehicleEvaluators.java"
```

`@PlannerAgent` lists the three evaluator interfaces as `subAgents` and stores the aggregated score under `outputKey = "evaluation"`, where the exit condition and the reviser can find it. The `@PlannerSupplier` method returns a new `VotingPlanner` that uses `aggregateVotes` as its voting strategy. `aggregateVotes` expects exactly three `VehicleEvaluation` results with scores between 1 and 10, and throws `IllegalStateException` if one is missing or malformed. The method signature also lists the trip details the evaluators need, and the framework passes them along through the workflow scope.

??? info "How does the `VotingPlanner` drive the evaluators?"
    A planner decides which subagents run next each time the framework asks it for an action. The `VotingPlanner` answers the first request with `call(subagents)`, which starts all evaluators in parallel. The framework then asks for the next action once for every evaluator that completes. The planner adds that evaluator's output to its votes and returns `noOp()` while others are still running. After the last one reports, it returns `done(...)` with the strategy's result, which becomes the output of `VehicleEvaluators`. Its `topology()` method returns `PARALLEL`, so the Dev UI renders the evaluators as parallel branches.

    The framework calls the `@PlannerSupplier` method on every invocation, so each loop iteration collects its votes in a new planner. The [`VotingPlanner` source](https://github.com/langchain4j/langchain4j/blob/main/langchain4j-agentic-patterns/src/main/java/dev/langchain4j/agentic/patterns/voting/VotingPlanner.java){target="_blank"} is a compact example of the `Planner` interface if you want to write a planner of your own.

## Add the vehicle reviser with @ChatModelSupplier

The reviser agent takes the current recommendation and evaluation feedback and produces an improved recommendation. It uses adaptive model selection to pick the right model for the current quality level.

==Create `src/main/java/com/tripplanner/agentic/agents/DynamicModelSelector.java`:==

```java title="DynamicModelSelector.java"
--8<-- "../../section-3/step-03/src/main/java/com/tripplanner/agentic/agents/DynamicModelSelector.java"
```

==Create `src/main/java/com/tripplanner/agentic/agents/VehicleReviser.java`:==

```java title="VehicleReviser.java"
--8<-- "../../section-3/step-03/src/main/java/com/tripplanner/agentic/agents/VehicleReviser.java"
```

`DynamicModelSelector` is a `@Singleton` bean that injects both the default `ChatModel` and the `@ModelName("enhancedModel")` model, and picks one based on the current evaluation score. The reviser keeps the Step 02 content guardrail through `@OutputGuardrails(TripAppropriatenessGuardrail.class, maxRetries = 3)`, so a revision that breaks the rules gets up to three more tries. Its `outputKey = "vehicle"` overwrites the vehicle in the scope, which means the cost estimator and everything after it receive the refined version without extra wiring.

## Wrap the review cycle with @LoopAgent

The loop wraps the evaluators and reviser into an iterative cycle with an exit condition.

==Create `src/main/java/com/tripplanner/agentic/workflow/VehicleReviewLoop.java`:==

```java title="VehicleReviewLoop.java"
--8<-- "../../section-3/step-03/src/main/java/com/tripplanner/agentic/workflow/VehicleReviewLoop.java"
```

`@LoopAgent` runs `VehicleEvaluators` and then `VehicleReviser` on each iteration. With `maxIterations = MAX_REVISIONS + 1` that makes four iterations: the first evaluation, plus one more after each of the three allowed revisions.

`@ExitCondition(testExitAtLoopEnd = false)` checks the condition right after each evaluation, before the reviser gets its turn. `shouldExit` receives the latest `VehicleEvaluation` and the `AgenticScope`. A score of 7.5 or more returns `true` and ends the loop. If the evaluators have run more than `MAX_REVISIONS` times without reaching the threshold, it throws `TripQualityException`, which reaches the browser as a 422 with error code `quality_not_met`. The loop's own `outputKey = "vehicle"` writes the final vehicle back to the scope, replacing the one from the research phase.

Before the loop can throw `TripQualityException`, you need the exception class itself.

==Create `src/main/java/com/tripplanner/model/TripQualityException.java`:==

```java title="TripQualityException.java"
--8<-- "../../section-3/step-03/src/main/java/com/tripplanner/model/TripQualityException.java"
```

The `CODE` and `MESSAGE` constants are used by the exception mapper and the frontend to display a consistent error when the loop exhausts its revision budget.

## Update the main workflow

==Open `src/main/java/com/tripplanner/agentic/workflow/TripPlannerSystem.java` and add `VehicleReviewLoop.class` to the `subAgents` array, between `ResearchPhase` and `CostEstimatorAgent`:==

```java hl_lines="5" title="TripPlannerSystem.java"
--8<-- "../../section-3/step-03/src/main/java/com/tripplanner/agentic/workflow/TripPlannerSystem.java"
```

The sequence now runs: parallel research → voting evaluation loop → cost estimation. The `@Output` method is unchanged because it still assembles the final `TripPlan` from `vehicle`, `itineraryResult`, and `costs`. The loop simply refines which vehicle reaches the cost estimator.

## Configure adaptive model selection

==Update `src/main/resources/application.properties` to add the enhanced model configuration:==

```properties title="application.properties"
--8<-- "../../section-3/step-03/src/main/resources/application.properties"
```

- The base model is now `gpt-4o-mini`, which is cost-effective for most agents.
- The `enhancedModel` is configured as a separate named model using `gpt-4o`. The `@ModelName("enhancedModel")` qualifier in `DynamicModelSelector` resolves to this configuration.
- Both models share the same `OPENAI_API_KEY`. The enhanced model has its own temperature and timeout settings.

## Inspecting the voting loop

If the application is not already running, start it from the project directory you chose above:

=== "Linux / macOS"
    ```bash
    ./mvnw quarkus:dev
    ```

=== "Windows"
    ```cmd
    .\mvnw.cmd quarkus:dev
    ```

==Open [http://localhost:8080](http://localhost:8080){target="_blank"} and fill in the form:==

- Destination: `Italian Riviera`
- Start date: a future date
- Duration: `5` days
- Travelers: `4`
- Trip Type: `Family Vacation`
- Budget: `Moderate (€1,000–€2,500)`

==Click **Generate Trip Plan**, wait for it to finish, and look for the evaluation and model selection messages in the terminal.== You should see lines like:

```text
Score 6.3 > 6.0 — switching to enhanced model for final refinement
```

This line means the `DynamicModelSelector` handed the revision to the enhanced model. It only appears when the reviser actually runs with a score above 6.0. If the evaluators liked the first vehicle enough to pass it outright, there's nothing to revise and nothing to log, so try another run or two.

==Open the [Quarkus Dev UI](http://localhost:8080/q/dev-ui){target="_blank"} and select **Topology** on the LangChain4j Agentic card.== The graph shows the full agent structure: the `planTrip` sequence contains the parallel research phase, the `vehicleReviewLoop`, and `estimateCosts`. Inside the loop you can see the `vehicleEvaluators` node, rendered as a parallel fan-out with the three evaluators, and the `vehicleReviser`.

![The Dev UI topology view showing the planTrip sequence with the research phase, vehicleReviewLoop containing three parallel evaluators and a reviser, and estimateCosts](../images/section-3-step-03-devui-topology.png)

==Switch to **Executions** on the same card and expand the latest run.== Each agent has its own row with its duration, inputs, and outputs. Under `vehicleReviewLoop`, the `vehicleEvaluators` rows show the three individual scores and the average they produced. If the average reached 7.5 on the first vote, the loop ends right there and `revise` never runs. If it fell short, you'll see a `revise` row followed by another round of evaluations, each tagged with its iteration number. Run the same request a few times and the number of rounds will probably change, because the evaluators are language models and have their moods like the rest of us.

![The Dev UI execution view for a run where the first vote passed, so vehicleReviewLoop has one vehicleEvaluators round and no revise row before estimateCosts](../images/section-3-step-03-devui-executions.png)

Now and then `planItinerary` also tries to activate a skill that doesn't exist, such as `route-overview`. Those calls show up marked failed, the tool replies with the list of available skills, and the agent carries on with the one it did load.

==Compare the output of `recommendVehicle` in the research phase with the output of `vehicleReviewLoop`.== If the reviser ran, the two recommendations differ. If the first candidate passed, they're the same vehicle.

??? info "Verifying with tests"
    The supplied tests verify the voting aggregation, pipeline assembly, and end-to-end HTTP contract without calling a live model.

    ==Run the Step 03 test suite:==

    === "Linux / macOS"
        ```bash
        ./mvnw test
        ```

    === "Windows"
        ```cmd
        .\mvnw.cmd test
        ```

    `VehicleEvaluationAggregatorTest` checks the averaging strategy. Three scores produce the right average, blank suggestions are skipped, and a missing, incomplete, or out-of-range set of votes throws `IllegalStateException`.

    `TripPlanContractTest` checks that `VehicleReviewLoop` sits between `ResearchPhase` and `CostEstimatorAgent` in the workflow, and that the trip plan JSON still has no `tips` field.

    `VehicleReviewWorkflowTest` calls the `/trip/plan` endpoint with a scripted model. It covers a first candidate that passes outright, a low-scoring candidate that gets revised and re-evaluated, three failed revisions ending in `quality_not_met`, and the guardrail rejecting a revised recommendation, including the case where the guardrail runs out of retries. Each scripted profile also replaces the `@ModelName("enhancedModel")` model with an `@Alternative`, so the `DynamicModelSelector` works without a live API key.

## Taking it further

As an optional exercise, try adding a fourth evaluator that assesses the vehicle's suitability for the planned route terrain (mountain roads, coastal highways, city driving). Use a different `outputKey` and update the `aggregateVotes` method to handle four votes.

You can also play with the exit threshold. Lowering it to 6.0 makes the loop exit sooner. Raising it to 9.5 will probably use up all three revisions, even though the scores tend to creep up with each one, and the browser then shows the `quality_not_met` message. Add a log line inside `shouldExit` to watch the score change from one round to the next.

![The trip planner showing an error that the vehicle recommendation did not meet the quality threshold after three revisions](../images/section-3-step-03-quality-not-met.png)

For a more advanced experiment, try a weighted voting strategy where the comfort evaluator counts double for family trips and the cost evaluator counts double for economy budgets. Pass the trip type into the aggregation to select the weights.

## Troubleshooting

??? warning "The enhanced model is not configured"
    ==Check that `application.properties` has the `quarkus.langchain4j.enhancedModel.*` properties and that `OPENAI_API_KEY` is set.== Both the base and enhanced models use the same API key. If the enhanced model configuration is missing, the `@ModelName("enhancedModel")` injection will fail at startup.

??? warning "Every request ends with quality_not_met"
    ==Check the evaluator prompts and the exit condition threshold.== If the evaluators consistently return low scores, three revisions may not be enough to get the recommendation over the line. Try lowering the threshold in `VehicleReviewLoop.shouldExit()` or adjusting the evaluator prompts to be more generous. Inspect the evaluation scores in the Dev UI execution view.

??? warning "Tests fail with missing enhancedModel bean"
    ==Check that each test profile's `getEnabledAlternatives()` includes both `ScriptedModel.class` and `ScriptedEnhancedModel.class`.== The `ScriptedEnhancedModel` is an `@Alternative @ModelName("enhancedModel")` bean that delegates to the default scripted model.

??? warning "OPENAI_API_KEY is not set"
    ==Set `OPENAI_API_KEY` in the shell used to start the application, then restart it.== Both models require the same key.

## What's next?

The planner now has a panel of evaluators that vote on every vehicle and a reviser that keeps trying until they're happy. In Step 04, we'll wrap the planning pipeline in an event-driven Quarkus Flow workflow, so the customer can take their time before approving or rejecting a trip.

[Continue to Step 04 - Event-Driven Agentic Workflows with Quarkus Flow](step-04.md)
