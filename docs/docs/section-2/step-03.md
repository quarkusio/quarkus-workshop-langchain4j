# Step 03 - Building nested agent workflows

## Expanding requirements

The Miles of Smiles management team has decided to get more serious about car maintenance. When cars are returned, the provided feedback should be analyzed — to see if car cleaning is needed and also to see if car maintenance is needed. If maintenance is needed then the car should be given to the maintenance team. If the car doesn't need maintenance but does need cleaning then it should be given to the car wash team. 

There are a number of things that we now need our car management app to handle:

- Car returns from rentals, the car wash, or the maintenance department
- Analyzing the return feedback to see if a car wash and/or maintenance are required
- Based on the feedback, getting the maintenance department to work on the car
- Based on the feedback, getting the car wash team to clean the car
- Automatically updating the car condition based on the analysis of the feedback

## Nested Workflows

In the previous step, we used a sequence workflow, which ran the car wash agent followed by the car condition feedback agent. In this step, we will build a sequence workflow that contains a parallel workflow, a conditional workflow, and a single agent (see diagram below). 

At each step in the workflow, the agentic framework checks the inputs needed by the next workflow or agent that needs to run. For the root of the workflow (in this case our sequence workflow), parameters are provided by the caller of the workflow interface. In subsequent steps within the workflow, the framework gathers values for input parameters from the AgenticScope. The output from each agent or workflow is added to the AgenticScope (using the agent's outputName setting). The output from a workflow is typically the output of the last agent in the workflow. When building the agent/workflow, you can also specify an output method, which will be run after the response from the agent/workflow is created — this is particularly useful for parallel workflows, to customize what to fill into the corresponding outputName for that agent/workflow.

## What are we going to build?

![App Blueprint](../images/agentic-app-3.png){: .center}

Starting from our app in step-02, we need to:

Add classes related to maintenance:

   - Modify CarManagementResource to add a maintenance returns API
   - Create a MaintenanceTool
   - Create a MaintenanceAgent

Create new feedback agents:

   - Create a MaintenanceFeedbackAgent
   - Create a CarWashFeedbackAgent

Change the agents that previously processed the feedback to use the output from the feedback agents instead:

   - Modify the CarWashAgent to use the output from the CarWashFeedbackAgent
   - Modify the CarConditionFeedbackAgent to use the output from the feedback agents

Create our nested workflow:

   - Create a parallel workflow, FeebackWorkflow, including the CarWashFeedbackAgent and MaintenanceFeedbackAgent
   - Create a conditional workflow, ActionWorkflow, including the CarWashAgent and MaintenanceAgent
   - Modify the sequence workflow, defined in the CarManagementService, to include the feedback workflow, the action workflow and the car condition feedback agent
   - Modify the CarProcessingWorkflow to add the maintenance feedback

## Before you begin
    
If you are continuing to build the app in the `step-01` directory, start by copying some files (which don't relate to the experience of building agentic AI apps) from `step-03`:

For Linux/macOS:
```bash
cd ./step-01
cp ../step-03/src/main/resources/static/css/styles.css ./src/main/resources/static/css/styles.css
cp ../step-03/src/main/resources/static/js/app.js ./src/main/resources/static/js/app.js
cp ../step-03/src/main/resources/templates/index.html ./src/main/resources/templates/index.html
cp ../step-03/src/main/java/com/carmanagement/service/CarService.java ./src/main/java/com/carmanagement/service/CarService.java
cp ../step-03/src/main/java/com/carmanagement/model/CarStatus.java ./src/main/java/com/carmanagement/model/CarStatus.java
```

For Windows:
```batch
cd .\step-01
copy ..\step-03\src\main\resources\static\css\styles.css .\src\main\resources\static\css\styles.css
copy ..\step-03\src\main\resources\static\js\app.js .\src\main\resources\static\js\app.js
copy ..\step-03\src\main\resources\templates\index.html .\src\main\resources\templates\index.html
copy ..\step-03\src\main\java\com\carmanagement\service\CarService.java .\src\main\java\com\carmanagement\service\CarService.java
copy ..\step-03\src\main\java\com\carmanagement\model\CarStatus.java .\src\main\java\com\carmanagement\model\CarStatus.java
```

## Add classes related to maintenance

Modify the CarManagementResource to add a maintenance returns API

```java title="CarManagementResource.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/resource/CarManagementResource.java:maintenanceReturn"
```

Create a MaintenanceTool

```java title="MaintenanceTool.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/tools/MaintenanceTool.java"
```

Similar to the CarWashTool, the MaintenanceTool can be used by the MaintenanceAgent to select maintenance options. If this was a real scenario it would potentially update a database to formally check in a car for maintenance.

Create a MaintenanceAgent

```java title="MaintenanceAgent.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/agents/MaintenanceAgent.java"
```

Similar to the CarWashAgent, the MaintenanceAgent interacts with the MaintenanceTool to request maintenance suggested by the MaintenanceFeedbackAgent.

## Create new feedback agents

Create a MaintenanceFeedbackAgent

```java title="MaintenanceFeedbackAgent.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/agents/MaintenanceFeedbackAgent.java"
```

The MaintenanceFeedbackAgent analyzes the feedback from rental returns, car wash returns and maintenance returns and decides if maintenance is required on the car.

We ask the LLM to include ==MAINTENANCE_NOT_REQUIRED== in its response if no maintenance is needed so that we can easily check for that string when we build our conditional agents.

Create a CarWashFeedbackAgent

```java title="CarWashFeedbackAgent.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/agents/CarWashFeedbackAgent.java"
```

Same concept as the MaintenanceFeedbackAgent, except that it checks to see if any car cleaning is required and includes ==CARWASH_NOT_REQUIRED== in its response otherwise.

## Use the output from the feedback agents

Modify the CarWashAgent to use the output from the CarWashFeedbackAgent

```java title="CarWashAgent.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/agents/CarWashAgent.java"
```

The CarWashAgent is no longer be responsible for interpreting feedback, so instead we modify it to rely on the output from the CarWashFeedbackAgent.

Modify the CarConditionFeedbackAgent to use the output from the feedback agents

```java title="CarConditionFeedbackAgent.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/agents/CarConditionFeedbackAgent.java:carConditionFeedbackSnippet"
```

Similarly to the CarWashAgent and MaintenanceAgent, we will have the CarConditionFeedbackAgent rely on the output from the feedback agents rather than interpreting the returns feedback directly itself.

## Modify the CarManagementService to create our nested workflow

Create a parallel workflow, FeebackWorkflow, including the CarWashFeedbackAgent and MaintenanceFeedbackAgent

```java title="FeedbackWorkflow.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/workflow/FeedbackWorkflow.java"
```

We need to analyze feedback from car returns both from the perspective of car cleanliness and needed repairs/maintenance. Since those are independent considerations we can do those analyses in parallel (to improve responsiveness of the overall workflow).

Create a conditional workflow, ActionWorkflow, including the CarWashAgent and MaintenanceAgent

```java title="ActionWorkflow.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/workflow/ActionWorkflow.java"
```

If there is maintenance required we want to send the car to get maintenance. If there is no maintenance required but there is car cleaning required we want to send the car to the car wash. To implement this we will use a conditional workflow. Conditional workflows are sequence workflows where each agent in the workflow is paired with a condition that must evaluate to true in order for the agent to be called (otherwise the agent is skipped).

Modify the sequence workflow, defined in the CarManagementService, to include the feedback workflow, the action workflow and the car condition feedback agent

```java title="CarManagementService.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/service/CarManagementService.java:sequenceWorkflow"
```

Notice that the CarProcessingWorkflow is a nested workflow (workflows within workflows).

Modify the CarProcessingWorkflow to add the maintenance feedback

```java title="CarProcessingWorkflow.java"
--8<-- "../../section-2/step-03/src/main/java/com/carmanagement/agentic/workflow/CarProcessingWorkflow.java"
```

## Trying out the new workflow

In the Returns section of the UI you should now be able to see a Maintenance Return tab in the Returns section. This is where the Miles of Smiles maintenance team will enter their feedback when they are finished working on the car. 

![Maintenance Returns Tab](../images/agentic-UI-maintenance-returns-tab.png){: .center}

On the Maintenance Return tab, for car 3, enter feedback to indicate the scratch (mentioned in the car condition) has been fixed, but the car needs to be cleaned:

```
buffed out the scratch. car could use a wash now.
```

Once the request completes, you should see that the car's status has been updated in the Fleet Status section.

![Updated Fleet Status](../images/agentic-UI-fleet-status.png){: .center}

Take a look at the logs. You should see that the car wash feedback agent and maintenance feedback agent both ran (in parallel, which may be evident from when the responses from those agents were logged). You should then see the car wash agent and car wash tool responses in the log (since there should now be no request for maintenance, but there should be a request for a car wash). Finally, you should see the response from the car condition feedback agent.
