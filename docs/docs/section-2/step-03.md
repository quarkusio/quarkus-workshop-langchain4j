# Step 03 - Building Nested Agent Workflows

## New Requirement: Comprehensive Car Management

The Miles of Smiles management team wants (again!) a more sophisticated car management system.
When cars are returned, the system should automatically:

1. **Analyze feedback** for both cleaning needs AND maintenance requirements
2. **Route cars appropriately** — send to maintenance if needed, otherwise to cleaning if needed
3. **Track all feedback sources** — from rentals, cleaning, and maintenance teams
4. **Update car conditions** based on all collected feedback

This requires a more complex workflow that can handle **parallel analysis** and **conditional routing**.

---

## What You'll Learn

In this step, you will:

- Build **nested workflows**, workflows that contain other workflows
- Use [**parallel workflows**](https://docs.langchain4j.dev/tutorials/agents#parallel-workflow){target="_blank"} to run multiple agents concurrently
- Implement *[*conditional workflows**](https://docs.langchain4j.dev/tutorials/agents#conditional-workflow){target="_blank"} that route execution based on conditions
- Understand **activation conditions** that control when agents execute
- See how to compose complex agentic systems from simple building blocks

---

## Understanding Nested Workflows

In [Step 02](step-02.md){target="_blank"}, you built a simple sequence workflow with two agents running one after another. In this step, you'll build a three-level nested workflow:

```mermaid
graph TD
    A[CarProcessingWorkflow<br/>Sequence] --> B[1. FeedbackWorkflow<br/>Parallel]
    A --> C[2. ActionWorkflow<br/>Conditional]
    A --> D[3. CarConditionFeedbackAgent<br/>Single Agent]

    B --> B1[CleaningFeedbackAgent]
    B --> B2[MaintenanceFeedbackAgent]

    C --> C1{Condition:<br/>Maintenance needed?}
    C1 -->|Yes| C2[MaintenanceAgent]
    C1 -->|No| C3{Condition:<br/>Cleaning needed?}
    C3 -->|Yes| C4[CleaningAgent]
    C3 -->|No| C5[Skip]
```

**The Flow:**

1. **FeedbackWorkflow** (Parallel): Analyzes feedback simultaneously from two perspectives:
    1. Does the car need maintenance?
    2. Does the car need cleaning?

2. **ActionWorkflow** (Conditional): Routes the car based on the analysis:
    1. If maintenance needed → send to maintenance team
    2. Else if cleaning needed → send to cleaning
    3. Else → do nothing

3. **CarConditionFeedbackAgent** (Single): Updates the car's condition based on all feedback

---

## What Are We Going to Build?

![App Blueprint](../images/agentic-app-3.png){: .center}

We'll transform the car management system to handle:

- **Three feedback sources**: rental returns, cleaning returns, maintenance returns
- **Parallel analysis**: concurrent evaluation for cleaning and maintenance needs
- **Conditional routing**: intelligent decision-making about where to send each car
- **Comprehensive tracking**: updated car conditions based on all feedback

---

## Architecture Overview

### The Nested Workflow Structure

```mermaid
sequenceDiagram
    participant User
    participant Main as CarProcessingWorkflow<br/>(Sequence)
    participant Feedback as FeedbackWorkflow<br/>(Parallel)
    participant Action as ActionWorkflow<br/>(Conditional)
    participant Condition as CarConditionFeedbackAgent
    participant Scope as AgenticScope

    User->>Main: processCarReturn(feedback)
    Main->>Scope: Initialize state

    Note over Main,Feedback: Step 1: Parallel Analysis
    Main->>Feedback: analyzeFeedback()
    par Parallel Execution
        Feedback->>Scope: CleaningFeedbackAgent<br/>Write: cleaningRequest
    and
        Feedback->>Scope: MaintenanceFeedbackAgent<br/>Write: maintenanceRequest
    end

    Note over Main,Action: Step 2: Conditional Routing
    Main->>Action: processAction()
    Action->>Action: Check conditions
    alt Maintenance Required
        Action->>Scope: MaintenanceAgent executes
    else Cleaning Required
        Action->>Scope: CleaningAgent executes
    else Neither Required
        Action->>Action: Skip both agents
    end

    Note over Main,Condition: Step 3: Update Condition
    Main->>Condition: analyzeForCondition()
    Condition->>Scope: Write: carCondition

    Main->>User: Return CarConditions
```

---

## Prerequisites

Before starting:

- Completed [Step 02](step-02.md){target="_blank"} (or have the `section-2/step-02` code available)
- Application from Step 02 is stopped (Ctrl+C)

---

!!! warning "Warning: this chapter involves many steps"
    In order to build out the solution, you will need to go through quite a few steps.
    While it is entirely possible to make the code changes manually (or via copy/paste),
    we recommend starting fresh from Step 03 with the changes already applied.
    You will then be able to walk through this chapter and focus on the examples and suggested experiments at the end of this chapter.

=== "Option 1: Start Fresh from Step 03 [Recommended]"

    Navigate to the complete `section-2/step-03` directory:
    
    ```bash
    cd section-2/step-03
    ```

=== "Option 2: Continue from Step 02"

    If you want to continue building on your previous code, place yourself at the root of your project and copy the updated files:

    === "Linux / macOS"
        ```bash
        cp ../step-03/src/main/resources/META-INF/resources/css/styles.css ./src/main/resources/META-INF/resources/css/styles.css
        cp ../step-03/src/main/resources/META-INF/resources/js/app.js ./src/main/resources/META-INF/resources/js/app.js
        cp ../step-03/src/main/resources/META-INF/resources/index.html ./src/main/resources/META-INF/resources/index.html
        cp ../step-03/src/main/resources/import.sql ./src/main/resources/import.sql
        cp ../step-03/src/main/java/com/carmanagement/model/CarStatus.java ./src/main/java/com/carmanagement/model/CarStatus.java
        ```

    === "Windows"
        ```cmd
        copy ..\step-03\src\main\resources\META-INF\resources\css\styles.css .\src\main\resources\META-INF\resources\css\styles.css
        copy ..\step-03\src\main\resources\META-INF\resources\js\app.js .\src\main\resources\META-INF\resources\js\app.js
        copy ..\step-03\src\main\resources\META-INF\resources\index.html .\src\main\resources\META-INF\resources\index.html
        copy ..\step-03\src\main\resources\import.sql .\src\main\resources\import.sql
        copy ..\step-03\src\main\java\com\carmanagement\service\CarService.java .\src\main\java\com\carmanagement\service\CarService.java
        copy ..\step-03\src\main\java\com\carmanagement\model\CarStatus.java .\src\main\java\com\carmanagement\model\CarStatus.java
        ```

---

## Part 1: Create Feedback Analysis Agents

We need two specialized agents to analyze feedback from different perspectives.

### Step 1: Create the MaintenanceFeedbackAgent

This agent determines if a car needs maintenance based on feedback.

In `src/main/java/com/carmanagement/agentic/agents`, create `MaintenanceFeedbackAgent.java`:

```java hl_lines="10 12-21 30-32 35" title="MaintenanceFeedbackAgent.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/agents/MaintenanceFeedbackAgent.java"
```

**Key Points:**

- **System message**: Focuses on mechanical issues, performance problems, and maintenance needs
- **Specific output format**: Returns "MAINTENANCE_NOT_REQUIRED" when no maintenance is needed (for easy conditional checking)
- **outputKey**: `"maintenanceRequest"` — stores the result in AgenticScope's state
- **Three feedback sources**: Analyzes rental, cleaning, AND maintenance feedback

### Step 2: Create the CleaningFeedbackAgent

This agent determines if a car needs cleaning based on feedback.

In `src/main/java/com/carmanagement/agentic/agents`, create `CleaningFeedbackAgent.java`:

```java hl_lines="17 33" title="CleaningFeedbackAgent.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/agents/CleaningFeedbackAgent.java"
```

**Key Points:**

- **System message**: Focuses on cleanliness issues — dirt, stains, smells
- **Specific output format**: Returns "CLEANING_NOT_REQUIRED" when no cleaning is needed
- **outputKey**: `"cleaningRequest"` — stores the result in AgenticScope's state
- **Same inputs**: Also analyzes all three feedback sources

---

## Part 2: Create the Parallel Feedback Workflow

Now we'll create a workflow that runs both feedback agents **concurrently**.

### Step 3: Create the FeedbackWorkflow

In `src/main/java/com/carmanagement/agentic/workflow`, create `FeedbackWorkflow.java`:

```java hl_lines="15-16" title="FeedbackWorkflow.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/workflow/FeedbackWorkflow.java"
```

**Let's break it down:**

#### `@ParallelAgent` Annotation

```java
@ParallelAgent(outputKey = "feedbackResult",
            subAgents = { CleaningFeedbackAgent.class, MaintenanceFeedbackAgent.class })
```

This defines a **parallel workflow**:

- Both agents execute **concurrently**
- Improves performance, no waiting for one to finish before the other starts
- Each agent has its own `outputKey` to store results independently

!!! note "Why Parallel Here?"
    The two feedback agents analyze different aspects (cleaning vs. maintenance) and don't depend on each other. 
    Running them in parallel cuts the total execution time roughly in half!

---

## Part 3: Create Action Agents

We need agents that can actually request maintenance and cleaninges.

### Step 4: Create the MaintenanceAgent

This agent uses a tool to request maintenance services.

In `src/main/java/com/carmanagement/agentic/agents`, create `MaintenanceAgent.java`:

```java hl_lines="14-18 31 37" title="MaintenanceAgent.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/agents/MaintenanceAgent.java"
```

**Key Points:**

- **Input**: `maintenanceRequest` — reads the output from `MaintenanceFeedbackAgent`
- **Tool**: `MaintenanceTool` — can request oil changes, brake service, etc. (We will create this tool later)
- **System message**: Interprets the maintenance request and calls the appropriate tool

### Step 5: Update the CleaningAgent

The `CleaningAgent` needs to read from the `CleaningFeedbackAgent`'s output.

Update `src/main/java/com/carmanagement/agentic/agents/CleaningAgent.java`:

```java hl_lines="18-23 31-32 42" title="CleaningAgent.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/agents/CleaningAgent.java"
```

**Key change:**

Now takes `cleaningRequest` as input (instead of analyzing raw feedback itself).
This follows the separation of concerns principle:

- Feedback agents: Analyze and decide
- Action agents: Execute based on decisions

---

## Part 4: Create the ***Conditional Action*** Workflow

Now we'll create a workflow that **conditionally** executes agents based on the feedback analysis.

### Step 6: Create the ActionWorkflow

In `src/main/java/com/carmanagement/agentic/workflow`, create `ActionWorkflow.java`:

```java hl_lines="16-17 27-30 32-35" title="ActionWorkflow.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/workflow/ActionWorkflow.java"
```

**Let's break it down:**

#### `@ConditionalAgent` Annotation

```java
@ConditionalAgent(outputKey = "actionResult",
            subAgents = { MaintenanceAgent.class, CleaningAgent.class })
```

A **conditional workflow** is a sequence where each agent only runs if its condition is met.

#### `@ActivationCondition` Methods

```java
@ActivationCondition(MaintenanceAgent.class)
static boolean activateMaintenance(String maintenanceRequest) {
    return isRequired(maintenanceRequest);
}

@ActivationCondition(CleaningAgent.class)
static boolean activateCleaning(String cleaningRequest) {
    return isRequired(cleaningRequest);
}
```

These methods control when each agent executes:

- **`activateMaintenance`**: Returns `true` if maintenance is needed
- **`activateCleaning`**: Returns `true` if cleaning is needed

This logic is defined in the `isRequired` method:
```java
private static boolean isRequired(String value) {
    return value != null && !value.isEmpty() && !value.toUpperCase().contains("NOT_REQUIRED");
}
```

The parameters are automatically extracted from ***AgenticScope***'s state by name.

#### Execution Logic

```
if (activateMaintenance(maintenanceRequest) == true)
    → Execute MaintenanceAgent
    → Skip CleaningAgent (regardless of its condition)
else if (activateCleaning(cleaningRequest) == true)
    → Execute CleaningAgent
else
    → Skip both agents
```

This implements _priority routing_: maintenance takes precedence over cleaning.

---

## Part 5: Update the Car Condition Agent

### Step 7: Update CarConditionFeedbackAgent

The condition agent should now use the analyzed requests instead of raw feedback.

Update `src/main/java/com/carmanagement/agentic/agents/CarConditionFeedbackAgent.java`:

```java hl_lines="25-27 37-38" title="CarConditionFeedbackAgent.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/agents/CarConditionFeedbackAgent.java"
```

**Key changes:**

- Now takes `cleaningRequest` and `maintenanceRequest` as inputs
- Uses the analyzed requests (which include reasoning) to determine condition
- More accurate condition updates based on professional analysis

---

## Part 6: Create Supporting Infrastructure

### Step 8: Create the MaintenanceTool

In `src/main/java/com/carmanagement/agentic/tools`, create `MaintenanceTool.java`:

```java title="MaintenanceTool.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/tools/MaintenanceTool.java"
```

Similar to `CleaningTool`, this tool:

- Uses `@Dependent` scope (required for tool detection)
- Provides maintenance options: oil change, tire rotation, brake service, etc.
- Updates car status to `IN_MAINTENANCE`
- Returns a summary of requested services

### Step 9: Create the RequiredAction Model

We need a model to represent what action is required for a car.

In `src/main/java/com/carmanagement/model`, create `RequiredAction.java`:

```java title="RequiredAction.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/model/RequiredAction.java"
```

### Step 10: Update the CarConditions Model

Update `src/main/java/com/carmanagement/model/CarConditions.java`:

```java title="CarConditions.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/model/CarConditions.java"
```
Notice how it has changed from `boolean cleaningRequired` to `RequiredAction requiredAction` to support three states.

### Step 11: Car Management API

Update `src/main/java/com/carmanagement/resource/CarManagementResource.java`:

```java hl_lines="32 54 66-85" title="CarManagementResource.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/resource/CarManagementResource.java"
```

This adds a new endpoint for the maintenance team to return cars with feedback, and adjusts the existing endpoints.

---

## Part 7: Update the Main Workflow

### Step 12: Update CarProcessingWorkflow

This is where everything comes together! Update the workflow to use nested workflows.

Update `src/main/java/com/carmanagement/agentic/workflow/CarProcessingWorkflow.java`:

```java hl_lines="17-18 29-41" title="CarProcessingWorkflow.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/workflow/CarProcessingWorkflow.java"
```

**Let's break it down:**

#### The Sequence

```java
@SequenceAgent(outputKey = "carProcessingAgentResult",
            subAgents = { FeedbackWorkflow.class, ActionWorkflow.class, CarConditionFeedbackAgent.class })
```

Notice the subagents include **workflows** (`FeedbackWorkflow`, `ActionWorkflow`), not just agents!

This is **workflow composition**: workflows can contain other workflows.

#### The Execution Order

1. **FeedbackWorkflow** (parallel): Analyzes feedback for both maintenance and cleaning needs
2. **ActionWorkflow** (conditional): Routes the car to maintenance or cleaning based on analysis
3. **CarConditionFeedbackAgent** (single): Updates the car's overall condition

#### The @Output Method

```java
@Output
static CarConditions output(String carCondition, String maintenanceRequest, String cleaningRequest) {
    RequiredAction requiredAction;
    // Check maintenance first (higher priority)
    if (isRequired(maintenanceRequest)) {
        requiredAction = RequiredAction.MAINTENANCE;
    } else if (isRequired(cleaningRequest)) {
        requiredAction = RequiredAction.CLEANING;
    } else {
        requiredAction = RequiredAction.NONE;
    }
    return new CarConditions(carCondition, requiredAction);
}
```

This static method extracts three values from AgenticScope's state and combines them into the final result.

---

## Part 8: Update the Service Layer

### Step 13: Update CarManagementService

And to get things over the finish line, we need to update the car management service to handle the new workflow structure.

Update `src/main/java/com/carmanagement/service/CarManagementService.java`:

```java hl_lines="28 47 52-55" title="CarManagementService.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/service/CarManagementService.java"
```

**Key changes:**

- Now passes `maintenanceFeedback` parameter to the workflow
- Uses `RequiredAction` enum to determine car status
- Sets status to `IN_MAINTENANCE` or `AT_CLEANING` based on the required action

---

## Try It Out

Whew!! That was a lot, right? But we've built a fully functional car management system with LangChain4j's Agentic AI functionality! High-fives to everyone around you are encouraged!
Let's test it out to understand how everything works together.

Start the application (if it wasn't already running):

```bash
./mvnw quarkus:dev
```

Open [http://localhost:8080](http://localhost:8080){target="_blank"}.

### Notice the New UI

The **Returns** section now has a **Maintenance Return** tab:

![Maintenance Returns Tab](../images/agentic-UI-maintenance-returns-tab.png){: .center}

### Test the Complete Workflow

Enter feedback on the Maintenance Return tab for the Ford F-150:

```
buffed out the scratch. car could use a wash now.
```

Click **Return**.

**What happens?**

1. **Parallel Analysis** (FeedbackWorkflow):
    1. `MaintenanceFeedbackAgent`: "MAINTENANCE_NOT_REQUIRED" (scratch fixed)
    2. `CleaningFeedbackAgent`: "Cleaning needed for general cleaning"

2. **Conditional Routing** (ActionWorkflow):
    1. Maintenance condition: `false` (not required)
    2. Cleaning condition: `true` (required)
    3. → Executes `CleaningAgent`, sends car to cleaning

3. **Condition Update** (CarConditionFeedbackAgent):
    1. Updates car condition: "Scratch removed, clean overall"

4. **UI Update**:
    1. Car status → `AT_CLEANING`
    2. Condition column updates

### Check the Logs

Look for evidence of parallel execution:

```
🚗 CleaningTool result: Cleaning requested for Ford F-150 (2021), Car #12:
- Exterior wash
Additional notes: Recommend an exterior cleaning. The feedback suggests the car could use a wash after the scratch was buffed out.
```

---

## How It All Works Together

Let's trace a complete example with maintenance needed:

### Example: "Strange engine noise"

```
Rental feedback: "Engine making strange knocking sound"
```

```mermaid
sequenceDiagram
    participant User
    participant Main as CarProcessingWorkflow
    participant Feedback as FeedbackWorkflow
    participant Action as ActionWorkflow
    participant Scope as AgenticScope State

    User->>Main: Return car with feedback
    Main->>Scope: Store: rentalFeedback="Engine knocking"

    rect rgb(255, 243, 205)
    Note over Main,Feedback: Parallel Workflow
    Main->>Feedback: Execute
    par
        Feedback->>Scope: MaintenanceFeedbackAgent<br/>Write: "Engine service needed - knocking sound"
    and
        Feedback->>Scope: CleaningFeedbackAgent<br/>Write: "CLEANING_NOT_REQUIRED"
    end
    end

    rect rgb(248, 215, 218)
    Note over Main,Action: Conditional Workflow
    Main->>Action: Execute
    Action->>Scope: Read: maintenanceRequest, cleaningRequest
    Action->>Action: activateMaintenance() = true
    Action->>Scope: MaintenanceAgent executes<br/>Write: "Maintenance requested: engine service"
    Note over Action: CleaningAgent skipped (condition false)
    end

    Main->>Scope: CarConditionFeedbackAgent<br/>Write: "Requires engine inspection"

    Main->>Main: @Output combines results
    Main->>User: CarConditions(condition="Requires engine inspection", action=MAINTENANCE)
```

---

## Understanding Workflow Composition

The power of this system comes from **composability** — complex workflows built from simple pieces.
We keep control on the flow, while letting agents focus on their specific tasks.

### Building Blocks

| Component | Type | Purpose |
|-----------|------|---------|
| `CleaningFeedbackAgent` | Agent | Analyzes cleaning needs |
| `MaintenanceFeedbackAgent` | Agent | Analyzes maintenance needs |
| `FeedbackWorkflow` | Parallel Workflow | Runs both analyses concurrently |
| `CleaningAgent` | Agent | Requests cleaninging |
| `MaintenanceAgent` | Agent | Requests maintenance |
| `ActionWorkflow` | Conditional Workflow | Routes to appropriate action |
| `CarConditionFeedbackAgent` | Agent | Updates car condition |
| `CarProcessingWorkflow` | Sequence Workflow | Orchestrates everything |

### Composition Hierarchy

```
CarProcessingWorkflow (Sequence)
├── FeedbackWorkflow (Parallel)
│   ├── CleaningFeedbackAgent
│   └── MaintenanceFeedbackAgent
├── ActionWorkflow (Conditional)
│   ├── MaintenanceAgent (with MaintenanceTool)
│   └── CleaningAgent (with CleaningTool)
└── CarConditionFeedbackAgent
```

This is a **three-level** nested workflow!

---

## Key Takeaways

- **Workflows are composable**: Build complex systems by nesting simple workflows
- **Parallel workflows improve response time**: Independent tasks run concurrently
- **Conditional workflows enable routing**: Execute different paths based on runtime conditions
- **Activation conditions are powerful**: Control agent execution with simple boolean logic
- **Separation of concerns**: Separate analysis agents from action agents for clarity
- **Type safety throughout**: Compile-time checks on the entire workflow structure

---

## Experiment Further

### 1. Add Priority Levels

What if you wanted to add a third level of priority (e.g., emergency repairs)?

- Add an `EmergencyRepairFeedbackAgent`
- Update `ActionWorkflow` with a third condition
- Ensure emergency repairs take highest priority

### 2. Make Feedback Analysis Sequential

Try changing `FeedbackWorkflow` from `@ParallelAgent` to `@SequenceAgent`. How does this affect performance? When might you want sequential analysis?

### 3. Add More Sophisticated Conditions

The `ActionWorkflow` currently uses simple `isRequired()` checks. Try adding:

- Cost-based conditions (only send to maintenance if estimated cost < $500)
- Time-based conditions (skip cleaning if it was cleaned in last 24 hours)
- Severity-based conditions (emergency repairs vs. routine maintenance)

### 4. Visualize the Workflow

Add logging to each agent and workflow to print when they start and finish. Observe the parallel execution in the logs!

---

## Troubleshooting

??? warning "Parallel agents not executing in parallel"
    Check that your system has multiple CPU cores and that the thread pool is configured properly. In development mode, Quarkus should handle this automatically.

??? warning "Conditional workflow always/never executing certain agents"
    - Verify your `@ActivationCondition` methods are correctly named
    - Check that parameter names match the `outputKey` values exactly
    - Add logging to the condition methods to see what values they're receiving

??? warning "Error: Cannot find symbol 'RequiredAction'"
    Make sure you created both:

    - The `RequiredAction` enum
    - Updated `CarConditions` to use it

??? warning "Agents getting wrong input values"
    Remember that parameter names must match the `outputKey` from previous agents or workflow inputs. Check for typos!

---

## What's Next?

You've built a sophisticated multi-level nested workflow combining sequence, parallel, and conditional execution!

In **Step 04**, you'll learn about **Agent-to-Agent (A2A) communication** — connecting your workflows to remote agents running in separate systems!

[Continue to Step 04 - Using Remote Agents (A2A)](step-04.md)
