# Step 03: Voting, loops, and adaptive model selection

This step extends the guardrails-aware trip planner from Step 02 with a vehicle review loop that evaluates and refines the vehicle recommendation before cost estimation. Three evaluator agents vote on the recommended vehicle in parallel using a custom `VotingPlanner`, a reviser agent refines the recommendation based on the aggregated evaluation, and an `@ExitCondition` stops the loop once the score reaches a threshold.

The `VehicleReviewLoop` is inserted between the `ResearchPhase` and `CostEstimatorAgent` in the planning sequence. The cost estimator seamlessly receives the refined vehicle because the loop's `outputKey` overwrites the original recommendation in the workflow scope.

The reviser agent uses `@ChatModelSupplier` with a `DynamicModelSelector` CDI bean that chooses between the base model (`gpt-4o-mini`) and an enhanced model (`gpt-4o`) based on the current evaluation score. This adaptive selection applies a more capable model only when the recommendation is already close to the target quality.

## Architecture

```
TripPlannerSystem (@SequenceAgent)
├── ResearchPhase (@ParallelAgent)
│   ├── VehicleAdvisorAgent  → outputKey="vehicle"
│   └── ItineraryPlannerAgent → outputKey="itineraryResult"
├── VehicleReviewLoop (@LoopAgent, maxIterations=3, outputKey="vehicle")
│   ├── VehicleEvaluators (@PlannerAgent/VotingPlanner, outputKey="evaluation")
│   │   ├── ComfortEvaluator   → outputKey="comfortEval"
│   │   ├── CostEvaluator      → outputKey="costEval"
│   │   └── FuelEfficiencyEvaluator → outputKey="fuelEval"
│   └── VehicleReviser (@Agent, outputKey="vehicle", @ChatModelSupplier)
└── CostEstimatorAgent → outputKey="costs"
```

The `VotingPlanner` and `VotingStrategy` are custom classes implementing the `dev.langchain4j.agentic.planner.Planner` interface. They dispatch evaluator subagents in parallel, collect their outputs from the workflow scope, and aggregate them into a single `VehicleEvaluation` using an averaging strategy.

## Run

Set `OPENAI_API_KEY`, then run `./mvnw quarkus:dev` (`.\mvnw.cmd quarkus:dev` on Windows). The trip form is at http://localhost:8080 and the Dev UI is at http://localhost:8080/q/dev-ui.

When another step owns port 8080, use `./mvnw quarkus:dev -Dquarkus.http.port=8082` and open http://localhost:8082 instead. Do not run `clean` while dev mode is running.

The base model is `gpt-4o-mini` for most agents. The reviser switches to `gpt-4o` (named `enhancedModel`) when the evaluation score exceeds 6.0. Both models require the same `OPENAI_API_KEY`.

## Test

```bash
./mvnw test -Dquarkus.http.test-port=0
```

The default Surefire configuration runs `VehicleEvaluationAggregatorTest` (plain JUnit), `TripPlanContractTest` (pipeline with voting loop), `TripPlanningFailureTest`, guardrail unit tests, and `GuardrailExceptionMapperTest`. All tests use scripted chat models and do not call a live model.

The scripted test profiles include an `@Alternative` for both the default and `@ModelName("enhancedModel")` chat models. The enhanced model alternative delegates to the default scripted model so all agent calls route through the same test fixture.

## References

- [LangChain4j voting agentic pattern](https://docs.langchain4j.dev/tutorials/agents/#voting-agentic-pattern)
- [Quarkus voting pattern blog post](https://quarkus.io/blog/introducing-voting-pattern/)
- [Quarkus LangChain4j agentic workflows](https://docs.quarkiverse.io/quarkus-langchain4j/dev/agentic.html)
- [Quarkus LangChain4j guardrails](https://docs.quarkiverse.io/quarkus-langchain4j/dev/guardrails.html)
- [Quarkus testing guide](https://quarkus.io/guides/getting-started-testing)
