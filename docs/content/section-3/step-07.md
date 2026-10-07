---
title: "Step 7 - Testing, Evaluation, and Observability"
layout: page
content-toc: true
---
# Step 07 - Testing, Evaluation, and Observability

In Step 06, we connected the planner to weather and points-of-interest tools through MCP so it could use their results when recommending a trip. Even with those details, the model still has to choose activities that fit the customer's request. It might, for example, recommend three days of sightseeing in Rome's historical center when the family specifically asked to visit coastal towns. Miles of Smiles needs to catch that mismatch before relying on customer feedback.

We need a way to check whether changes to our prompts, skills, or models improve the results, or at least don't make them worse. Evaluation gives us a repeatable way to make that comparison.

## Evaluating with Langfuse

Evaluation gives the planner a known trip request and checks the resulting plan against a description of what a satisfactory answer should contain. For a family asking for three days around Rome with coastal towns, we would expect a complete three-day itinerary with activities that fit the family and include a coastal visit. Repeating this request after a prompt or model change lets us compare the plans against the same expectations.

[Langfuse](https://langfuse.com/docs/evaluation/core-concepts) is a platform for recording AI application runs and evaluating their results. We'll keep our trip requests and expectations in a dataset there, then compare runs as experiments. The [Quarkus Langfuse extension](https://docs.quarkiverse.io/quarkus-langfuse/dev/index.html) connects our application to Langfuse, sends planning traces, and lets the application set up the dataset and evaluator. This gives us the plan, its score, and the calls that produced it in one place.

```mermaid
flowchart LR
    sample["Same trip request<br/>and expectations"] --> first["First planning run"]
    sample --> second["Run after a change"]
    first --> compare["Compare plans<br/>and scores"]
    second --> compare
```

## Judge models

Some requirements can be checked directly, such as whether a three-day trip has three itinerary days. Others need judgment. A plan might mention the coast without offering a useful coastal visit. A judge model reads the completed plan and uses a rubric, or scoring instructions, to assess it against the request. It returns a score with an explanation that we can check against the plan.

We'll also add a judge to the vehicle budget guardrail. This judge checks a recommendation while the planner is working and can ask the vehicle agent to try again. The Langfuse judge scores a completed plan during an evaluation run, so its verdict helps us assess the planner without changing that plan.

## Testing with scripted responses

Before asking a judge to assess a live plan, we need to know that our checks work and that the workflow passes the right information between agents. Plans with known good and bad contents let us test the structural checks. Scripted model responses then give the workflow predictable replies while the MCP agents call the Trip Intelligence server. This lets us check whether weather and points of interest reach the itinerary planner without model variation obscuring a wiring problem.

```mermaid
flowchart LR
    fixtures["Known plans"] --> structure["Do the checks catch defects?"]
    scripts["Scripted replies + MCP data"] --> workflow["Does the workflow pass data correctly?"]
    live["Live model + saved request"] --> quality["Does the plan meet the request?"]
```

## Understanding scores and traces

A score is a starting point for reviewing a plan. The judge's explanation tells us which requirement it thought the plan missed, and a trace shows the agent calls and MCP tool results from that planning run. If the score is low, we'll compare the explanation with the plan, then use the trace to find where the recommendation came from. After a change, we can compare two experiments to see how their plans and scores differ.

## Prepare the working copy

#### Option 1: Continue from Step 06


<mark>Stop dev mode in your Step 06 working copy, then copy `section-3/step-07/trip-planner/pom.xml` to `trip-planner/pom.xml` in your working copy. Keep any local model-provider dependencies you added earlier.</mark> Use the completed Step 07 project for comparison if you get stuck.

<mark>Remove the Step 06 tests from your working copy's `trip-planner` directory: `src/test/java/com/tripplanner/agentic/`, `src/test/java/com/tripplanner/mcp/`, and `src/test/frontend/`.</mark> These tests remain available in Step 06. The Step 07 test configuration selects the evaluation suite.

<mark>Then follow the page from [the vehicle guardrail changes](#check-vehicle-budgets-with-a-judge-model) onwards.</mark> The first dev mode start after the POM change takes longer than usual, because Dev Services pulls and starts the Langfuse containers.

#### Option 2: Follow the completed Step 07 project


<mark>Copy `section-3/step-07` to a working directory and open that copy. Apply your model-provider settings.</mark> Every file on this page is already there, so you can follow along and join the hands-on part at [Run the offline tests](#run-the-offline-tests).

You still need a container runtime for Dev Services, and the live evaluation runs need the Trip Intelligence MCP server from Step 06 running on port 8085. The judge in Langfuse calls OpenAI, so dev mode needs `OPENAI_API_KEY` set for the quality run. If you use another provider, read the note in [Configure the Langfuse evaluator](#configure-the-langfuse-evaluator) first.

The paths below are relative to your working copy's `trip-planner` directory.

The updated POM adds the LangChain4j evaluation module for loading samples and running checks. It also adds OpenTelemetry and the Langfuse extension to record planning runs and send them to Langfuse.

**pom.xml (evaluation dependencies)**
```xml
{snippet:insert("section-3/step-07/trip-planner/pom.xml:76", "104")}
```

Maven runs the fixture-based tests with `./mvnw test`. The `evals` profile selects the live integration tests through Failsafe, so we'll use it when we reach the MCP and model evaluations.

## Check vehicle budgets with a judge model

An economy-budget customer should not receive an expensive vehicle recommendation. The existing brand list catches an obvious luxury brand such as Ferrari, but can miss an expensive model from another manufacturer. We'll add a judge to the guardrail so it can check the vehicle before the workflow continues and ask the vehicle agent to try again when needed. This judge acts during planning. The Langfuse judge we'll configure later scores a completed plan for an evaluation run; its score does not change the customer's plan.

The budget judge returns a verdict and a reason, which we'll represent with a Java record.

<mark>Create `src/main/java/com/tripplanner/guardrails/BudgetVerdict.java`:</mark>

**BudgetVerdict.java**
```java
{snippet:insert("section-3/step-07/trip-planner/src/main/java/com/tripplanner/guardrails/BudgetVerdict.java")}
```

<mark>Create `src/main/java/com/tripplanner/guardrails/VehicleBudgetJudge.java`:</mark>

**VehicleBudgetJudge.java**
```java
{snippet:insert("section-3/step-07/trip-planner/src/main/java/com/tripplanner/guardrails/VehicleBudgetJudge.java")}
```

The judge reads the budget tier together with the vehicle type, model, and the recommending agent's reasoning. Its prompt asks it to reject a recommendation when it is uncertain whether the vehicle fits an economy budget.

`@RegisterAiService` connects this service to the named `judgeModel`. The service also needs `@ApplicationScoped` because the guardrail runs on Quarkus Flow executor threads, where there is no HTTP request context. The default `@RequestScoped` service would fail there with `RequestScoped context was not active`.

<mark>Update the highlighted lines in `src/main/java/com/tripplanner/guardrails/TripAppropriatenessGuardrail.java` to inject and use the judge:</mark>

**TripAppropriatenessGuardrail.java**
```java
{snippet:insert("section-3/step-07/trip-planner/src/main/java/com/tripplanner/guardrails/TripAppropriatenessGuardrail.java")}
```

For economy budgets, the guardrail still rejects brands in `OBVIOUS_LUXURY_BRANDS` without calling the judge. Other recommendations go to the judge, whose verdict determines whether the guardrail asks the vehicle agent for an affordable alternative.

<mark>Add the `judgeModel` configuration to `src/main/resources/application.properties`:</mark>

**application.properties (judgeModel)**
```properties
# Model used by the vehicle budget judge
quarkus.langchain4j.judgeModel.chat-model.provider=openai
quarkus.langchain4j.openai.judgeModel.api-key=$\{OPENAI_API_KEY}
quarkus.langchain4j.openai.judgeModel.chat-model.model-name=gpt-4o-mini
quarkus.langchain4j.openai.judgeModel.chat-model.temperature=0
quarkus.langchain4j.openai.judgeModel.timeout=30
```

The budget judge uses its own model configuration, so you can change it independently of the agents that generate the plan.

## Define acceptable plans for sample requests

The Rome example gives us a repeatable question: does a plan for a three-day family trip include a coastal town? A sample saves the planner inputs alongside a description of an acceptable result. We can then change the planner and compare each new plan against the same requirements.

<mark>Create `src/main/resources/evaluation/samples.yaml`:</mark>

**samples.yaml**
```yaml
{snippet:insert("section-3/step-07/trip-planner/src/main/resources/evaluation/samples.yaml")}
```

In `rome-family-three-days`, the `parameters` are the trip request and `expected-output` describes what a satisfactory plan contains, including family-friendly activities and a coastal town. Several itineraries could meet those requirements, so the sample does not prescribe exact wording or stops.

The Java checks use the request parameters to check details such as the number of days. The Langfuse judge compares the generated plan with `expected-output`.

The samples belong in the main resources because dev mode reads them at startup and copies them into the `trip-plan-samples` dataset in Langfuse. Edit the YAML file to keep changes in your project, then restart dev mode to update the dataset.

## Write the evaluation rubric

Java can check whether the itinerary has three days, but assessing whether its activities suit a family requires judgment. We'll give the Langfuse evaluator a rubric that tells it how to compare the plan with the sample's requirements.

<mark>Create `src/main/resources/evaluation/rubric.txt`:</mark>

**rubric.txt**
```text
{snippet:insert("section-3/step-07/trip-planner/src/main/resources/evaluation/rubric.txt")}
```

For the Rome sample, Langfuse replaces `\{\{input}}` with the saved trip request, `\{\{output}}` with the generated plan JSON, and `\{\{ground_truth}}` with the requirements in `expected-output`. The rubric asks the judge to check each requirement without penalizing different wording, order, or extra detail. The result is a score with an explanation of any missed requirements.

## Configure the Langfuse evaluator

Starting dev mode will copy the YAML samples into a Langfuse dataset and register the rubric as an evaluator. When the quality test runs a sample, Langfuse receives the completed plan through its trace, applies the rubric, and records a score. `LangfuseEvaluationSetup` prepares the dataset, model connection, and evaluator through the extension's `LangfuseOperations` client.

<mark>Create `src/main/java/com/tripplanner/evaluation/LangfuseEvaluationSetup.java`:</mark>

**LangfuseEvaluationSetup.java**
```java
{snippet:insert("section-3/step-07/trip-planner/src/main/java/com/tripplanner/evaluation/LangfuseEvaluationSetup.java")}
```

At startup, `onStart()` checks whether evaluation setup is enabled. When it is, the class creates the model connection and the `trip-plan-samples` dataset, then registers the rubric as an evaluator named `plan-quality`. Its evaluation rule selects experiment items from that dataset and supplies the request, generated plan, and expected output to the rubric.

The evaluator returns a score between 0 and 1, where 1 means the plan meets every requirement, with a sentence explaining any missed requirements. The test will retrieve this score by its `plan-quality` name, so the evaluator and test must use the same name.

The setup reuses existing objects and updates dataset items by sample name, so restarting it against the same Langfuse instance does not add duplicate samples. If a setup call fails, the application logs a warning and continues starting. The quality test needs both the dataset and evaluator, so we'll check those startup messages before running it.

<mark>Add the setup flag to `src/main/resources/application.properties` to enable this initialization in dev mode:</mark>

**application.properties (evaluation setup)**
```properties
{snippet:insert("section-3/step-07/trip-planner/src/main/resources/application.properties:21", "23")}
```

> [!NOTE]
> **Using a provider other than OpenAI**
> The judge runs in the Langfuse worker container and calls the model through the LLM connection, so it needs an OpenAI API key even when your planner uses another provider. If your provider has an OpenAI-compatible endpoint, you can point the connection at it instead, either by editing the LLM connection in the Langfuse UI or by setting a base URL in `createLlmConnection()`. For Ollama, the base URL is `http://host.docker.internal:11434/v1`, because `localhost` inside the container is the container itself. If neither works for you, skip the quality run. The offline suite and the composition run work with any provider.

## Check plan structure

Before judging the recommendations, we need to know whether a plan is complete enough to evaluate. A missing itinerary day or invalid cost is a definite error, so Java can catch it without asking a model. We'll first test those checks against plans with known contents. Once they work, we'll test the workflow and then the quality of a live plan.

The supplied evaluation code loads the samples, checks plans, runs the planner, and retrieves scores. Copy it into the working project before running the tests.

<mark>For the hands-on route, copy these paths from `section-3/step-07/trip-planner` to the same paths in your working copy:</mark>

- `src/test/java/com/tripplanner/evaluation/` (the invariant strategy, the dataset loader, the experiment runner, and all the tests)
- `src/test/resources/evaluation/known-bad.yaml` (plans with deliberate defects)
- `src/test/resources/application.properties` (test profiles for the offline, composition, and quality runs)
- `src/main/java/com/tripplanner/agentic/TracingExecutorSetup.java` (keeps one planning run in one trace)

`TripPlanText` converts plans to JSON for evaluation and parses that JSON when the structural checks need to inspect a field.

A plan needs a complete itinerary and a valid cost before we assess its recommendations. `TripPlanInvariantStrategy` checks those fields in Java and implements `EvaluationStrategy<String>` so the evaluation module can run it against each sample.

**TripPlanInvariantStrategy.java (the checks)**
```java
{snippet:insert("section-3/step-07/trip-planner/src/test/java/com/tripplanner/evaluation/TripPlanInvariantStrategy.java:26", "71")}
```

The requested duration comes from the sample's third parameter. The strategy collects every problem it finds instead of stopping at the first one:

- The vehicle must have a type, model, and reasoning, and the plan needs a route overview.
- The itinerary must have exactly the requested number of days, each with a title, description, and overnight stop.
- Day numbers must be unique, fall between 1 and the requested duration, and leave no gaps.
- The total cost must be a plain, non-negative euro amount.

A plan with no problems scores 1, and anything else scores 0 with the list of problems as the reason.

To check that the strategy detects defects, `known-bad.yaml` contains plans with a missing vehicle, a gap in the itinerary, a negative or non-numeric cost, and an empty output. `TripPlanInvariantStrategyTest` checks that each fails for the expected reason and that a complete plan passes.

## Run the offline tests

`TripPlanEvaluationHarnessTest` checks that the harness loads the samples and applies the requested duration to each fixture plan. It also checks that a one-day plan fails a request for a longer trip and that the results can be saved as a JSON report. Maven runs this test alongside the invariant strategy tests by default.

<mark>Run the default test suite from your `trip-planner` directory:</mark>

#### Linux / macOS

```bash
./mvnw test
```

#### Windows

```cmd
.\mvnw.cmd test
```

These tests tell us whether the structural checks recognize known good and bad plans. If a deliberately broken plan passes, or a valid fixture fails, investigate the checks before relying on their results. They do not tell us whether the agents exchange the right information during a real planning run, which is what we'll test next.

## Test the workflow with scripted responses

Now we can test whether the agents pass the right data through the workflow. The composition test uses scripted model responses while the two `@McpClientAgent` subagents call the running Trip Intelligence server. With model responses fixed, a failed assertion points to the workflow or its MCP data rather than a change in the model's wording.

`TripPlannerCompositionLiveIT` has two tests:

- The first plans a Rome trip, checks that the weather and points of interest from the MCP server reached the itinerary planner's prompt, and runs the invariant strategy on the saved output.
- The second plans two trips in a row and checks that each plan keeps its own request's destination.

The test's `mcp` profile disables Langfuse because these assertions use the plan and the captured prompts. Before each test, it probes the MCP server on port 8085 and skips if the server isn't running the sunny fixture.

<mark>With the MCP server running, run the composition IT from your `trip-planner` directory:</mark>

#### Linux / macOS

```bash
./mvnw -Pevals -Dit.test=TripPlannerCompositionLiveIT -Dquarkus.http.test-port=0 verify
```

#### Windows

```cmd
.\mvnw.cmd -Pevals -Dit.test=TripPlannerCompositionLiveIT -Dquarkus.http.test-port=0 verify
```

## Evaluating recommendation quality

The composition test confirms that the workflow connects its parts, but scripted replies cannot tell us whether the configured model recommends suitable activities. The quality test generates a new plan for the Rome sample, checks its structure, and asks Langfuse to assess its recommendations against the saved requirements.

Before planning, the test checks that the MCP server is running the sunny fixture and that the `trip-plan-samples` dataset exists in Langfuse. If the dataset is missing, the test fails and tells you to start dev mode first, since dev mode is what creates it. The test then loads the `rome-family-three-days` item from the dataset and passes it to a small helper bean that runs the planner:

**TripPlanQualityEvaluationLiveIT.java (running the dataset item)**
```java
{snippet:insert("section-3/step-07/trip-planner/src/test/java/com/tripplanner/evaluation/TripPlanQualityEvaluationLiveIT.java:49", "56")}
```

The runner records the planning calls in a trace and returns the plan together with its trace id. The test checks the plan's structure before waiting for the judge's score:

**TripPlanQualityEvaluationLiveIT.java (waiting for the score)**
```java
{snippet:insert("section-3/step-07/trip-planner/src/test/java/com/tripplanner/evaluation/TripPlanQualityEvaluationLiveIT.java:63", "72")}
```

Langfuse evaluates the plan asynchronously in its worker container, so the test polls for the trace's `plan-quality` score for up to 90 seconds. It passes when the structural checks succeed, the score is at least 0.7, and the judge has supplied a reason.

The test's `evals` profile connects to the Langfuse instance started by dev mode, where the dataset and evaluator have been created. It uses the Dev Services credentials and disables the generic OTLP exporter because the Langfuse extension exports the traces itself.

**src/test/resources/application.properties (quality evaluation profile)**
```properties
{snippet:insert("section-3/step-07/trip-planner/src/test/resources/application.properties:43", "46")}
```

Keep dev mode running while you evaluate plans and inspect their traces. The test is skipped unless you pass its Langfuse URL through `quarkus.langfuse.base-url`.

==Start dev mode in your working project with `./mvnw quarkus:dev` (`.\mvnw.cmd quarkus:dev` on Windows), with `OPENAI_API_KEY` set, and keep the MCP server running. Open the Dev UI at [http://localhost:8080/q/dev](http://localhost:8080/q/dev), go to **Dev Services**, and copy the `quarkus.langfuse.base-url` value from the Langfuse entry.==

![The Dev UI Dev Services page showing the Langfuse container with its injected configuration: API endpoint, host, port, UI URL, login credentials, and API keys](/images/section-3-step-07-devui-dev-services.png)

The same page lists the Langfuse login, `quarkus@quarkus.io` with the password `quarkuslangfuse`. The Extensions page also has a Quarkus Langfuse card with a direct link to the Langfuse UI.

<mark>Check the dev mode log for the `Langfuse evaluation setup` messages before running the test.</mark> Each completed setup operation logs a ready message. If the log reports a warning, resolve the reported setup failure before continuing.

<mark>In a second terminal, run the quality IT from your `trip-planner` directory, using the URL you copied:</mark>

#### Linux / macOS

```bash
./mvnw -Pevals -Dit.test=TripPlanQualityEvaluationLiveIT -Dquarkus.http.test-port=0 \
    -Dquarkus.langfuse.base-url=http://localhost:<port> verify
```

#### Windows

```cmd
.\mvnw.cmd -Pevals -Dit.test=TripPlanQualityEvaluationLiveIT -Dquarkus.http.test-port=0 -Dquarkus.langfuse.base-url=http://localhost:<port> verify
```

<details>
<summary>Complete quality IT</summary>

**TripPlanQualityEvaluationLiveIT.java**
```java
--8<-- "../../section-3/step-07/trip-planner/src/test/java/com/tripplanner/evaluation/TripPlanQualityEvaluationLiveIT.java"
```

</details>


## Inspect the score and the planning trace

A passing test tells us that the plan met the structural checks and scored at least 0.7. To understand which requirements affected the score, we need to read the judge's explanation alongside the plan.

<mark>Open Langfuse from the Quarkus Langfuse card, log in, and go to **Tracing**. Open the `trip-plan-evaluation` trace, select its root span in the tree, and select the **Scores** tab.</mark>

![The trip-plan-evaluation trace in Langfuse with the full agent tree, from the MCP weather and points-of-interest calls through the vehicle, itinerary, evaluator, and cost agents, and a plan-quality score of 0.85 on the root span with the judge's reasoning](/images/section-3-step-07-langfuse-score.png)

Read the `plan-quality` score and its explanation first. Compare any missed requirement with the saved sample and the generated plan before deciding whether the planner needs a change. A low score can also reflect a mistaken judgment, so the explanation matters as much as the number.

The trace shows the calls that produced the plan. The MCP weather and points-of-interest calls feed the vehicle advisor and itinerary planner; the evaluators and any vehicle revision follow, then the cost estimator calls its pricing tool. The header shows latency, cost, and token counts for the run. The score is on the root span, where the experiment records the completed plan.

<mark>Open the score's comment and find any requirement the judge says was missed.</mark> Check the plan against that requirement. If the plan is wrong, inspect the relevant agent's prompt and response. For a recommendation that depends on weather or points of interest, check the MCP tool result as well. If the explanation misreads the plan, review the rubric before treating the score as evidence of a regression.

### Keep parallel calls in one trace

The vehicle and itinerary research run in parallel. By default, the executor used for those branches does not carry the current OpenTelemetry context to the new threads, so one planning run would appear as several unrelated traces. The `TracingExecutorSetup` copied earlier wraps that executor and carries the context into each branch.

**TracingExecutorSetup.java**
```java
{snippet:insert("section-3/step-07/trip-planner/src/main/java/com/tripplanner/agentic/TracingExecutorSetup.java:20", "31")}
```

At startup, `Context.taskWrapping()` wraps the Quarkus managed executor that LangChain4j uses for parallel branches. Their agent calls then appear under the same planning trace you just inspected.

<details>
<summary>How the trace becomes a dataset experiment</summary>

The experiment name is `step-07-` followed by the current time, so each run of the IT shows up as a separate run of the dataset in Langfuse. The helper bean runs the planner inside a root span:

**TripPlanExperimentRunner.java**
```java
--8<-- "../../section-3/step-07/trip-planner/src/test/java/com/tripplanner/evaluation/TripPlanExperimentRunner.java:34:55"
```

`@WithSpan("trip-plan-evaluation")` opens a span for the whole planning run, so every agent, guardrail, and MCP call is recorded beneath it. The attributes on that span turn the trace into an experiment item:

- `langfuse.experiment.dataset.id` and `langfuse.experiment.item.id` tie the trace to the dataset item it came from.
- `langfuse.experiment.id` and `langfuse.experiment.name` both get the experiment name, which groups the trace with the other items of the same run.
- `langfuse.experiment.item.root_observation_id` is the span's own id, which tells Langfuse where the result of the run is.
- `langfuse.experiment.item.expected_output` carries the sample's expected output, which the judge reads as `\{\{ground_truth}}`.
- `langfuse.observation.input` and `langfuse.observation.output` hold the trip request and the rendered plan, which become `\{\{input}}` and `\{\{output}}`.
- `langfuse.trace.tags` tags the trace with `evaluation`, and the two `langfuse.trace.metadata.*` attributes record the sample id and the planner's model name, so you can filter traces by them when you run the IT several times.

</details>


## Compare plans after a change

To compare results, we need a second evaluation of the same request. <mark>Change a planner prompt or skill, then rerun the quality test with dev mode still running.</mark> Keep the sample requirements and rubric unchanged so both plans are assessed against the same criteria.

<mark>Go to **Datasets**, open `trip-plan-samples`, and select the **Experiments** tab. Select two runs and click **Compare**.</mark>

![Two step-07 experiment runs of the trip-plan-samples dataset compared in Langfuse, with plan-quality scores of 0.85 and 0.80 for the rome-family-three-days item, the difference between them, the item's input, and its expected output](/images/section-3-step-07-langfuse-dataset-runs.png)

Each quality test adds an experiment for the `rome-family-three-days` item. The comparison shows both generated plans and their scores beside the request and expected output. <mark>Open the speech-bubble icon next to each score to read the judge's reasoning.</mark> Compare the explanations with the plans to see whether the change addressed the requirement you were interested in. A single pair of runs is only a starting point because both generation and judging can vary between runs.

### Adjust the rubric

If the judge repeatedly misinterprets a requirement, try making the rubric more specific. <mark>Open **Evaluators** and select **plan-quality**.</mark>

The evaluator's prompt contains the rubric from `rubric.txt`. You can edit it here and rerun the quality test to inspect the judge's explanation with the revised instructions. Changing the rubric also changes the basis for the score, so treat this as an adjustment to the evaluation itself.

<mark>Copy any rubric changes you want to keep into `src/main/resources/evaluation/rubric.txt`.</mark> Dev Services removes the Langfuse containers when dev mode stops, and the next start recreates the evaluator from that file.

### Inspect model costs and latency

<mark>Open the project dashboard to inspect model costs and timing across the recorded runs.</mark> The trace names identify the agents and tools, which helps locate the calls responsible for a slow or expensive plan.

![The Langfuse project dashboard showing 8 traces, model costs, and per-agent trace names including getWeatherForecast, getPointsOfInterest, and the AI service agents](/images/section-3-step-07-langfuse-dashboard.png)

<mark>Scroll down to the latency charts and hover over a trace name.</mark> The tooltip shows the agent class and method so you can locate the corresponding code.

![The Langfuse dashboard latencies view with per-trace, per-generation, and per-observation timing percentiles](/images/section-3-step-07-langfuse-latencies.png)

## Further reading

Eric Deandrea's [non-deterministic-no-problem](https://github.com/edeandrea/non-deterministic-no-problem) project uses the same Quarkus Langfuse extension and goes further than this step. It detects drift in a running application with a guardrail and scores whole sessions as well as single traces. The Langfuse documentation on [running experiments through OpenTelemetry](https://langfuse.com/integrations/native/opentelemetry/experiments) describes the `langfuse.experiment.*` attributes the quality run sets.

You can now rerun a trip request after changing the planner and inspect how the generated plan meets its requirements. This completes Section 3. Continue to the [conclusion](conclusion.md) to review the workshop.
