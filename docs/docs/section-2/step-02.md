# Step 02 - Composing Simple Agent Workflows

## Tracking the Condition of Cars

The Miles of Smiles management team now wants to keep track of the condition of its cars.

In the previous step, cars could be returned by the team processing returns or the car wash team — and in either case comments could be provided from the teams about the car. We would like to automatically update the recorded condition of the car based on those comments.

In this step you will be introduced to using multiple agents together in a workflow.

## Workflows

With LangChain4j you can set up a set of agents to work together to solve problems. Much like the building blocks of a programming language, `langchain4j-agentic` provides some basic constructs you can use to build agentic workflows:

- **Sequence Workflows** - Agents execute one after another in a predetermined order.
- **Parallel Workflows** - Agents execute at the same time on separate threads.
- **Loop Workflows** - A sequence of agents runs repeatedly, until some condition is satisfied.
- **Conditional Workflows** - A sequence of agents runs in a predetermined order, but each agent in the sequence only runs if a specified condition is satisfied.

To satisfy management's new requirement, let's use a **sequence** of agents to first call the car wash agent, and then call another agent to update the car condition.

To enable agents to better work together, `langchain4j-agentic` includes a shared context class called `AgenticScope`. The agent framework uses the `AgenticScope` to maintain context between calls to each agent in a workflow. When calling an agent in a workflow, the agent framework attempts to use an internal map in the `AgenticScope` to read inputs corresponding to the list of inputs declared in the agent method's signature. When an agent returns a result, the agent framework writes the result into the `AgenticScope`'s map using the output name specified by the agent.

## What Are We Going to Build?

![App Blueprint](../images/agentic-app-2.png){: .center}

We'll create a workflow that processes car returns, updates car conditions based on feedback, and manages the car washing process.

Starting from our app in step-01, we need to:

Create/Update agents and workflows:

- Create a `CarConditionFeedbackAgent`
- Create a `CarProcessingWorkflow` agent interface to use for the sequence workflow

Define the agents and workflows:

- Define the sequence workflow in `CarManagementService`
- Modify the `CarManagementService` to use the sequence workflow

## Before You Begin

You can either use the code from `step-01` and continue from there, or check the final code of the step located in the `step-02` directory.
    
??? important "Do not forget to close the application"
    If you have the application running from the previous step and decide to use the `step-02` directory, make sure to stop it (CTRL+C) before continuing.

If you are continuing to build the app in the `step-01` directory, start by copying some files (which don't relate to the experience of building agentic AI apps) from `step-02`:

For Linux/macOS:
```bash
cd ./step-01
cp ../step-02/src/main/resources/static/css/styles.css ./src/main/resources/static/css/styles.css
cp ../step-02/src/main/resources/static/js/app.js ./src/main/resources/static/js/app.js
cp ../step-02/src/main/resources/templates/index.html ./src/main/resources/templates/index.html
cp ../step-02/src/main/java/com/carmanagement/service/CarService.java ./src/main/java/com/carmanagement/service/CarService.java
cp ../step-02/src/main/java/com/carmanagement/model/CarInfo.java ./src/main/java/com/carmanagement/model/CarInfo.java
```

For Windows:
```batch
cd .\step-01
copy ..\step-02\src\main\resources\static\css\styles.css .\src\main\resources\static\css\styles.css
copy ..\step-02\src\main\resources\static\js\app.js .\src\main\resources\static\js\app.js
copy ..\step-02\src\main\resources\templates\index.html .\src\main\resources\templates\index.html
copy ..\step-02\src\main\java\com\carmanagement\service\CarService.java .\src\main\java\com\carmanagement\service\CarService.java
copy ..\step-02\src\main\java\com\carmanagement\model\CarInfo.java .\src\main\java\com\carmanagement\model\CarInfo.java
```

## Create/Update agents and workflows

### Create a `CarConditionFeedbackAgent`

In the `src/main/java/com/carmanagement/agentic/agents` directory, create the `CarConditionFeedbackAgent`:

```java title="CarConditionFeedbackAgent.java"
--8<-- "../../section-2/step-02/src/main/java/com/carmanagement/agentic/agents/CarConditionFeedbackAgent.java"
```

As we've seen before, the interface for an agent defines the system message, user message and indicates which method is the agent method. The car condition feedback agent will assess the car's condition based on its previous known condition and the feedback provided.

### Create a `CarProcessingWorkflow` Agent Interface to Use for the Sequence Workflow

First, create the directory:

For Linux/macOS:
```bash
mkdir -p ./src/main/java/com/carmanagement/agentic/workflow
```

For Windows:
```batch
mkdir .\src\main\java\com\carmanagement\agentic\workflow
```

Then create the agent interface in that directory for the sequence workflow:

```java hl_lines="16" title="CarProcessingWorkflow.java"
--8<-- "../../section-2/step-02/src/main/java/com/carmanagement/agentic/workflow/CarProcessingWorkflow.java"
```

`CarProcessingWorkflow` is a type-safe interface that we can use as our sequence workflow. Notice that the `CarProcessingWorkflow` interface looks a lot like a regular agent. Workflows can be thought of as containers for sets of agents, not agents themselves. Since workflows do not interact with LLMs, they do not have `@SystemMessage` or `@UserMessage` annotations. Notice that the `processCarReturn` method returns a result with type `ResultWithAgenticScope<String>` -- this is a special type that causes the generated code to return not just the text response from the agent, but also the `AgenticScope` that is created and used in the workflow.

## Define the agents and workflows

We'll need to make a few changes to our `CarManagementService` to define our new agent and workflow.

Complete the steps below in your `CarManagementService` file, or simply copy the following code to the file in your `src/main/java/com/carmanagement/service` directory.

```java hl_lines="43-45 48-59 61-67 83-91 93-100 102-105" title="CarManagementService.java"
--8<-- "../../section-2/step-02/src/main/java/com/carmanagement/service/CarManagementService.java"
```

### Define the Sequence Workflow in `CarManagementService`

First, we'll define the sequence workflow in `CarManagementService`.

- Let's modify the `initialize` method to initialize the `CarProcessingWorkflow` when the service is instantiated.
- The `createCarProcessingWorkflow` method needs to define the `CarWashAgent` and `CarConditionFeedbackAgent` — the 2 agents we want to include in our sequence workflow.
- The `createCarProcessingWorkflow` method then needs to define the sequence workflow, `CarProcessingWorkflow`, including the `CarWashAgent` and `CarConditionFeedbackAgent` as subagents (the subagent list represents the list of agents that are in the workflow).

### Modify the `CarManagementService` to Use the Sequence Workflow

In the `CarManagementService`, let's modify the `processCarReturn` method to call the `carProcessingWorkflow` and process its results. 

First, we need to invoke `carProcessingWorkflow.processCarReturn`, the agent method, to cause each of the subagents to be executed in sequence. 

Next, retrieve the `carCondition` value from the `AgenticScope`, and use that value as the new condition for the car.

As before, check the results from the car wash agent to decide whether to change the car state.

## Try Out the New Workflow

Now that we have updated the workflow to update the car condition we can try it in the UI. 

In your browser, access [http://localhost:8080](http://localhost:8080){target="_blank"}.

Notice that the **Fleet Status** section of the UI now has a "Condition" column, indicating the last known condition of the car.

On the **Rental Return** tab choose a car and enter some feedback that would indicate something has changed about the condition of the car. For example:

```
there has clearly been a fire in the trunk of this car
```

After submitting the feedback (by hitting the **Return** button), and a brief pause, you should see the condition of the car gets updated in the **Fleet Status** section.

![Agentic UI](../images/agentic-UI-2.png){: .center}

You should also see in the log file that the car wash agent and car condition agent ran in sequence. Here you can see that the car wash agent requested an interior wash of car, and the car condition feedback agent came up with a new car condition.

Take a moment to look at the logs from your Quarkus runtime. You should be able to identify the following sequence of events:

1. an HTTP request to the car wash agent
2. an HTTP response from the car wash agent, requesting to run the requestCarWash function
3. the CarWashTool output, requesting interior cleaning of the car (and possibly other options)
4. an HTTP request to the car wash agent, including the response from the car wash tool
5. an HTTP response from the car wash agent, stating that the car wash has been requested
6. an HTTP request to the car condition feedback agent
7. an HTTP response from the car condition feedback agent, providing a new car condition summary

## When to Use Parallel Workflows

In this step, we could have run the car wash agent and the car condition feedback agent in parallel since the car condition feedback agent doesn't depend on the output from the car wash agent. We chose to do them in sequence to simplify later steps in this lab, but you can try changing the sequence workflow to a parallel workflow (which should complete faster than the sequence workflow).
