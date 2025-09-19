# Step 04 - Using remote agents (A2A)

## Time to get rid of some cars!

The Miles of Smiles team has decided they need to get rid of cars that are no longer in good enough shape to rent out. When car return feedback warrants disposing of a car, they want to send the car to a car broker who will then sell, donate, or scrap it. The Miles of Smiles team will still make the determination of which cars to get rid of, and the car broker will take action on their request. The car broker has their own agent that handles car dispositions.

## Agent2Agent (A2A)

The Agent2Agent protocol can be used to enable agents to communicate to each other remotely. 

LangChain4j provides the `langchain4j-agentic-a2a` module, which can be used with `langchain4j-agentic` to add remote agents to a workflow in the same way as you have been adding local agents. We will add an A2A agent to our existing app that we will then add to our workflow. Communicating with this local agent causes a2a to send the agent request to the remotely connected agent.

You will see how the A2A sdk handles the protocol in our remote A2A server built on Quarkus. As part of the protocol, agents defined in the server must provide an AgentCard which describes:

- The name and description of the agent
- The agent's capabilities (the parts of the A2A protocol it supports)
- The agent's skills (what the agent's purpose is)
- etc.

A2A Agents must also define an AgentExecutor. The A2A sdk calls the `AgentExecutor.execute` method when it wants to invoke your agent. Your implementation of the AgentExecutor interface is responsible for calling your agent (for example your LangChain4j AI service or Agent). The AgentExecutor.execute method has the following signature:

```java
public void execute(RequestContext context, EventQueue eventQueue)
```

The execute method is invoked when a task or message need to be handled. 

Tasks have unique IDs, have a state (submitted, working, input-required, auth-required, completed, canceled, failed, rejected or unknown), and can be reference across requests to the A2A agent. As such, tasks are created for tracking work on a specific topic (eg. a hotel booking) that may not complete within a few seconds.

Messages have unique IDs but no tracked state. They are good for short requests that do not require more than the recent message history to provide responses for.

Our DispositionAgent will handle getting rid of cars, where each disposition of a car is a task.

## What are we going to build?

![App Blueprint](../images/agentic-app-4.png){: .center}

Starting from our app in `step-03`, we need to do the following for the original Quarkus Runtime 1:

1. Create a new DispositionFeedbackAgent
2. Create a new DispositionAgent (for the client side)
3. Modify the ActionWorkflow to accept the disposition feedback agent's output
4. Modify the agents and workflows in CarManagementService
5. Define the DispositionFeedbackAgent and DispositionAgent
6. Include the DispositionFeedbackAgent in the parallel workflow
7. Include the DispositionAgent in the conditional workflow
8. Modify the CarConditionFeedbackAgent to use input from the DispositionFeedbackAgent

## Before you begin

If you are continuing to build the app in the `step-01` directory, start by copying some files (which don't relate to the experience of building agentic AI apps) from `step-04`:

For Linux/macOS:
```bash
cd ./step-01
cp ../step-04/multi-agent-system/pom.xml ./multi-agent-system/pom.xml
cp ../step-04/multi-agent-system/src/main/java/com/carmanagement/model/CarInfo.java ./multi-agent-system/src/main/java/com/carmanagement/model/CarInfo.java
cp ../step-04/multi-agent-system/src/main/java/com/carmanagement/model/CarStatus.java ./multi-agent-system/src/main/java/com/carmanagement/model/CarStatus.java
cp ../step-04/multi-agent-system/src/main/java/com/carmanagement/service/CarService.java ./multi-agent-system/src/main/java/com/carmanagement/service/CarService.java
cp ../step-04/multi-agent-system/src/main/resources/static/css/styles.css ./multi-agent-system/src/main/resources/static/css/styles.css
cp ../step-04/multi-agent-system/src/main/resources/static/js/app.js ./multi-agent-system/src/main/resources/static/js/app.js
cp ../step-04/multi-agent-system/src/main/resources/templates/index.html ./multi-agent-system/src/main/resources/templates/index.html
```

For Windows:
```batch
cd .\step-01
copy ..\step-04\multi-agent-system\pom.xml .\multi-agent-system\pom.xml
copy ..\step-04\multi-agent-system\src\main\java\com\carmanagement\model\CarInfo.java .\multi-agent-system\src\main\java\com\carmanagement\model\CarInfo.java
copy ..\step-04\multi-agent-system\src\main\java\com\carmanagement\model\CarStatus.java .\multi-agent-system\src\main\java\com\carmanagement\model\CarStatus.java
copy ..\step-04\multi-agent-system\src\main\java\com\carmanagement\service\CarService.java .\multi-agent-system\src\main\java\com\carmanagement\service\CarService.java
copy ..\step-04\multi-agent-system\src\main\resources\static\css\styles.css .\multi-agent-system\src\main\resources\static\css\styles.css
copy ..\step-04\multi-agent-system\src\main\resources\static\js\app.js .\multi-agent-system\src\main\resources\static\js\app.js
copy ..\step-04\multi-agent-system\src\main\resources\templates\index.html .\multi-agent-system\src\main\resources\templates\index.html
```

### Create a new DispositionFeedbackAgent

```java title="DispositionFeedbackAgent.java"
--8<-- "../../section-2/step-04/multi-agent-system/src/main/java/com/carmanagement/agentic/agents/DispositionFeedbackAgent.java"
```

As we've done with the other agents, we set the system message, user message, and annotate the method that we want to be the agent method.

### Create a new DispositionAgent (for the client side)

```java title="DispositionAgent.java"
--8<-- "../../section-2/step-04/multi-agent-system/src/main/java/com/carmanagement/agentic/agents/DispositionAgent.java"
```

This agent, which we'll use as the A2A client, doesn't have system message or user message annotations since it is just acting as a type-safe interface for us to invoke the remote A2A agent. Notice this is very similar to how we define workflows (since neither workflows nor A2A client agents interact directly with LLMs).

### Modify the ActionWorkflow to accept the disposition feedback agent's output

```java title="ActionWorkflow.java"
--8<-- "../../section-2/step-04/multi-agent-system/src/main/java/com/carmanagement/agentic/workflow/ActionWorkflow.java:actionWorkflow"
```

### Modify the agents and workflows in CarManagementService

```java title="CarManagementService.java"
--8<-- "../../section-2/step-04/multi-agent-system/src/main/java/com/carmanagement/service/CarManagementService.java"
```

### Define the DispositionFeedbackAgent and DispositionAgent

We use the `a2abuilder` method to create an A2A agent out of the DispositionAgent interface.

### Include the DispositionFeedbackAgent in the parallel workflow

This enables the 3 feedback agents to run at the same time.

### Include the DispositionAgent in the conditional workflow

The condition specified checks to see if the disposition agent is the one of the 3 selected to run, based on the output from the feedback agents.

### Modify the CarConditionFeedbackAgent to use input from the DispositionFeedbackAgent

```java title="CarConditionFeedbackAgent.java"
--8<-- "../../section-2/step-04/multi-agent-system/src/main/java/com/carmanagement/agentic/agents/CarConditionFeedbackAgent.java:carConditionFeedback"
```

## Starting from our app in step-03, we need to do the following for Quarkus Runtime 2 (the Remote A2A Agent)

1. Create a new Quarkus project for the remote A2A agent
2. Create a new DispositionAgent
3. Create a new DispositionTool
4. Create a new DispositionAgentCard
5. Create a new DispositionAgentExecutor

## Before you begin

Run the following commands to get your second Quarkus project set up with some initial files.

For Linux/macOS:
```bash
cd ./step-01
mkdir remote-a2a-agent
cp ../step-04/remote-a2a-agent/mvnw.cmd ./remote-a2a-agent/mvnw.cmd
cp ../step-04/remote-a2a-agent/pom.xml ./remote-a2a-agent/pom.xml
cp ../step-04/remote-a2a-agent/README.md ./remote-a2a-agent/README.md
cp ../step-04/remote-a2a-agent/.gitignore ./remote-a2a-agent/.gitignore
cp ../step-04/remote-a2a-agent/.mvn/wrapper/.gitignore ./remote-a2a-agent/.mvn/wrapper/.gitignore
cp ../step-04/remote-a2a-agent/mvnw ./remote-a2a-agent/mvnw
cp ../step-04/remote-a2a-agent/src/main/resources/application.properties ./remote-a2a-agent/src/main/resources/application.properties
```

For Windows:
```batch
cd .\step-01
mkdir remote-a2a-agent
copy ..\step-04\remote-a2a-agent\mvnw.cmd .\remote-a2a-agent\mvnw.cmd
copy ..\step-04\remote-a2a-agent\pom.xml .\remote-a2a-agent\pom.xml
copy ..\step-04\remote-a2a-agent\README.md .\remote-a2a-agent\README.md
copy ..\step-04\remote-a2a-agent\.gitignore .\remote-a2a-agent\.gitignore
mkdir .\remote-a2a-agent\.mvn\wrapper
copy ..\step-04\remote-a2a-agent\.mvn\wrapper\.gitignore .\remote-a2a-agent\.mvn\wrapper\.gitignore
copy ..\step-04\remote-a2a-agent\mvnw .\remote-a2a-agent\mvnw
mkdir .\remote-a2a-agent\src\main\resources
copy ..\step-04\remote-a2a-agent\src\main\resources\application.properties .\remote-a2a-agent\src\main\resources\application.properties
```

### Create a new Quarkus project for the remote A2A agent

The files you have already copied from step-04 include the Quarkus project setup. Take a moment to look at the pom.xml file to see the new dependency added for `langchain4j-agentic-a2a`.

### Create a new DispositionAgent

```java title="DispositionAgent.java (remote)"
--8<-- "../../section-2/step-04/remote-a2a-agent/src/main/java/com/demo/DispositionAgent.java"
```

Similar to our CarWashAgent and MaintenanceAgent.

### Create a new DispositionTool

```java title="DispositionTool.java"
--8<-- "../../section-2/step-04/remote-a2a-agent/src/main/java/com/demo/DispositionTool.java"
```

Similar to our CarWashTool and MaintenanceTool.

### Create a new DispositionAgentCard

```java title="DispositionAgentCard.java"
--8<-- "../../section-2/step-04/remote-a2a-agent/src/main/java/com/demo/DispositionAgentCard.java"
```

The agent card provides:

- A description of the agent
- A URL to invoke the agent with
- A URL for the agent's documentation 
- An indication of the agent's supported A2A capabilities
- Input and output modes
- A description of the skills the agent provides
- An A2A protocol version

This information is provided to clients that connect to the A2A server so that they know when and how to use the agent.

### Create a new DispositionAgentExecutor

```java hl_lines="39-43 48-56 59-65 68-71" title="DispositionAgentExecutor.java"
--8<-- "../../section-2/step-04/remote-a2a-agent/src/main/java/com/demo/DispositionAgentExecutor.java"
```

In the execute method we need to take action on the task:

- Line 39: We initialize a TaskUpdater, which is responsible for making updates to the Task status and recording events (such as Task status updates or additions of artifacts to the task) in the EventQueue.

- Lines 40-43: If there is no task currently in the RequestContext we put the task into the initial submitted state. If the task already exists, we put it into working state.

- Lines 48-56: We extract the message parts from the request context. When an A2A agent has multiple parameters, each parameter gets stored as a separate message part.

- Lines 59-65: We invoke our LangChain4j DispositionAgent with the same parameters as were provided to the A2A agent.

- Lines 68-71: We put the agent's response into an artifact in the Task and mark the task complete. This will result in the response being made available to the A2A client agent.

## Trying out the new workflow

Ensure both Quarkus runtimes are running. From each of the multi-agent-system and remote-a2a-agent directories, run the following command (if it is not already running):

```bash
mvn quarkus:dev
```

After reloading the UI, you should see the Returns section is now called ==Returns and Dispositions==. You'll also notice that there is a new tab to list the cars that are pending disposition.

On the Maintenance Return tab, try entering feedback that would suggest there is something wrong with the car (so that it should be disposed of). For example:

![Maintenance Returns Tab](../images/agentic-UI-maintenance-returns-2.png){: .center}

In the logs of Quarkus runtime 2, you should be able to see that the disposition agent called its disposition tool and the tool initiated the disposition of the vehicle.

```
DispositionTool result: Car disposition requested for Ford Mustang (2022), Car #11: Scrap the car
```