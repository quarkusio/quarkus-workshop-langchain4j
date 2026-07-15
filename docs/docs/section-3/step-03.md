# Step 03 - Event-Driven Workflows with Quarkus Flow

## From Synchronous Calls to Suspendable Workflows

So far, trip planning happens synchronously: a REST call comes in, the multi-agent pipeline runs, and a response goes back. That works fine when everything completes in one go, but real-world trip planning isn't like that. A partner confirms a booking, the system generates a trip plan, sends it to the customer for approval, and then... waits. The customer might not respond for hours. Holding a thread open the whole time isn't an option.

In this step, you'll wrap the existing trip planning pipeline inside a [Quarkus Flow](https://docs.quarkiverse.io/quarkus-flow/dev/){target="_blank"} workflow connected to Kafka via CloudEvents. The workflow generates a trip plan and emits it for approval, then **suspends execution entirely**. No thread held, no resource consumed. When the customer eventually responds, the engine correlates the approval event to the correct workflow instance and resumes right where it left off.

You'll write a single `Flow` class with five tasks in a fluent DSL chain. The starter project provides everything else: the Kafka configuration, the REST endpoint that bridges approvals into CloudEvents, and the adapter that connects to your existing agents.

---

## What Is Quarkus Flow?

Quarkus Flow is a Java-native workflow engine built on the [CNCF Serverless Workflow specification](https://serverlessworkflow.io/){target="_blank"}. It discovers workflow definitions at build time from CDI beans, connects to messaging systems for event-driven execution, and integrates tightly with Quarkus Dev Services (Kafka, databases, etc.).

The key concept for this step is the **listen-emit pattern**. A workflow can emit a CloudEvent, then call `listen()` to suspend itself until a matching event arrives. The engine releases the thread, keeps the workflow state in memory, and resumes execution when the event shows up. This is the foundation of human-in-the-loop (HITL) workflows: the system does its work, asks for a human decision, and goes to sleep until the answer comes.

!!! note "In-memory only"
    In this step, workflow state lives in memory. If you restart the application while a workflow is suspended, that instance is lost. This is a deliberate limitation. In **Step 04**, you'll add `quarkus-flow-jpa` and a database to make workflows durable with zero changes to the `Flow` class you write here.

---

## Prerequisites

=== "Option 1: Continue from Step 02"

    If you want to continue building on top of Step 02 code, stay in the `section-3/step-02` directory. You'll add the new dependencies and files as described below.

=== "Option 2: Follow along using the completed solution"

    If you prefer to follow along (without making any code changes), navigate to the completed `section-3/step-03` directory:

    === "Linux / macOS"
        ```bash
        cd section-3/step-03
        ./mvnw quarkus:dev
        ```

    === "Windows"
        ```cmd
        cd section-3\step-03
        mvnw quarkus:dev
        ```

---

## New Dependencies

This step adds three new dependencies to `pom.xml`. ==Open `section-3/step-02/pom.xml` and add the following:==

```xml
<dependency>
    <groupId>io.quarkiverse.flow</groupId>
    <artifactId>quarkus-flow</artifactId>
    <version>0.10.2</version>
</dependency>
<dependency>
    <groupId>io.quarkiverse.flow</groupId>
    <artifactId>quarkus-flow-langchain4j</artifactId>
    <version>0.10.2</version>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-messaging-kafka</artifactId>
</dependency>
```

`quarkus-flow` is the workflow engine itself. `quarkus-flow-langchain4j` scans your agentic interfaces at build time and registers each `@SequenceAgent` and `@ParallelAgent` as a Flow workflow definition automatically. The `TripPlannerSystem` and `ResearchPhase` from Step 02 will show up in the Flow Dev UI as generated workflows alongside the hand-written one you create in this step. `quarkus-messaging-kafka` brings SmallRye Reactive Messaging with Kafka support, and Quarkus Dev Services automatically provisions a Kafka broker so you don't need to install anything.

---

## Kafka Channel Configuration

==Add the following to `application.properties`:==

```properties
# Quarkus Flow messaging bridge
quarkus.flow.messaging.defaults-enabled=true

# Kafka channels for Flow events
mp.messaging.incoming.flow-in.connector=smallrye-kafka
mp.messaging.incoming.flow-in.topic=flow-in
mp.messaging.incoming.flow-in.value.deserializer=org.apache.kafka.common.serialization.ByteArrayDeserializer
mp.messaging.incoming.flow-in.key.deserializer=org.apache.kafka.common.serialization.StringDeserializer

mp.messaging.outgoing.flow-out.connector=smallrye-kafka
mp.messaging.outgoing.flow-out.topic=flow-out
mp.messaging.outgoing.flow-out.value.serializer=org.apache.kafka.common.serialization.ByteArraySerializer
mp.messaging.outgoing.flow-out.key.serializer=org.apache.kafka.common.serialization.StringSerializer

mp.messaging.outgoing.flow-in-producer.connector=smallrye-kafka
mp.messaging.outgoing.flow-in-producer.topic=flow-in
mp.messaging.outgoing.flow-in-producer.value.serializer=org.apache.kafka.common.serialization.ByteArraySerializer
mp.messaging.outgoing.flow-in-producer.key.serializer=org.apache.kafka.common.serialization.StringSerializer
```

The `flow-in` channel is where the engine listens for incoming CloudEvents (booking confirmations, approval responses). The `flow-out` channel is where it publishes outgoing events (approval requests, confirmed plans). The `flow-in-producer` channel is a separate outgoing channel that the approval REST endpoint uses to publish CloudEvents back onto the same `flow-in` Kafka topic. SmallRye Reactive Messaging doesn't allow the same channel name for both incoming and outgoing, so the producer needs its own channel name pointing to the same underlying topic. The `defaults-enabled=true` setting activates the Quarkus Flow messaging bridge, which handles the CloudEvent envelope serialization and routing between the engine and Kafka.

---

## What the Starter Provides

Before you write the Flow class, three supporting files need to be in place. These bridge the existing trip planning agents to the event-driven world.

### TripPlannerFlowAdapter

The existing `TripPlannerSystem.planTrip()` takes six individual parameters, but a Flow `function()` task needs a single-argument function. This adapter bridges the gap.

==Create `src/main/java/com/tripplanner/agentic/flow/TripPlannerFlowAdapter.java`:==

```java title="TripPlannerFlowAdapter.java"
--8<-- "../../section-3/step-03/src/main/java/com/tripplanner/agentic/flow/TripPlannerFlowAdapter.java"
```

The adapter destructures a `TripRequest` into the six parameters that `planTrip()` expects. This keeps the Flow class clean: `function("planTrip", adapter::planFromRequest, TripRequest.class)` reads naturally.

### TripApproval

==Create `src/main/java/com/tripplanner/model/TripApproval.java`:==

```java title="TripApproval.java"
--8<-- "../../section-3/step-03/src/main/java/com/tripplanner/model/TripApproval.java"
```

### TripApprovalResource

This REST endpoint is the human-in-the-loop bridge. It takes an approval decision from the customer and turns it into a CloudEvent that the suspended workflow is waiting for.

==Create `src/main/java/com/tripplanner/resource/TripApprovalResource.java`:==

```java title="TripApprovalResource.java"
--8<-- "../../section-3/step-03/src/main/java/com/tripplanner/resource/TripApprovalResource.java"
```

The endpoint receives a `TripApproval` body and an `X-Flow-Instance-Id` header. It builds a CloudEvent with the `flowinstanceid` extension attribute set to that header value, serializes it, and sends it to the `flow-in-producer` channel. This is a separate outgoing channel that publishes to the same `flow-in` Kafka topic that the Flow engine consumes. SmallRye Reactive Messaging doesn't allow the same channel name for both incoming and outgoing, so the producer uses a distinct channel name pointing to the same underlying topic. The engine picks the event up, matches it to the suspended workflow instance by `flowinstanceid`, and resumes execution.

---

## Implementing the Event-Driven Workflow

This is the file you write. A single class, five tasks, about 25 lines.

==Create `src/main/java/com/tripplanner/agentic/flow/TripPlannerFlow.java`:==

```java title="TripPlannerFlow.java"
--8<-- "../../section-3/step-03/src/main/java/com/tripplanner/agentic/flow/TripPlannerFlow.java"
```

The class extends `io.quarkiverse.flow.Flow` and is discovered at build time by the engine. The five tasks form a linear chain: wait for a booking event, generate the plan, emit it for approval, suspend until approval arrives, then emit the confirmed plan.

The first `listen()` is event-started, meaning each incoming booking CloudEvent creates a new workflow instance. Note the `.outputAs()` call: `listen()` always returns a `Collection`, even for `toOne()`, so you need to unwrap it. The `function()` call runs the full multi-agent pipeline from Step 02 through the adapter.

After the plan is generated, `emitJson()` publishes it as a `com.tripplanner.trip.approval.requested` CloudEvent on the `flow-out` channel. The engine automatically attaches a `flowinstanceid` extension to the event, identifying this specific workflow instance. Downstream consumers use this ID to route the response back.

The second `listen()` is where the workflow suspends. Unlike the first one, this `listen()` is event-resumed: the workflow already exists and is waiting for a specific event correlated by `flowinstanceid`. Between this point and the eventual approval, no thread is held. The `.exportAs()` call here is important because by default the approval payload would replace the workflow context, overwriting the `TripPlan`. The lambda returns `wf.context().asJavaObject()` to preserve the current context (the trip plan) so the final `emitJson()` serializes the correct payload.

!!! tip "Event-started vs. event-resumed"
    The first `listen()` creates a new workflow instance per incoming event, no correlation needed. The second `listen()` routes the approval event to the correct suspended instance via `extensionByInstanceId("flowinstanceid")`. This distinction matters when multiple trip plans are in flight simultaneously.

---

## Running the Application

==Start the application in dev mode:==

=== "Linux / macOS"
    ```bash
    cd section-3/step-03
    ./mvnw quarkus:dev
    ```

=== "Windows"
    ```cmd
    cd section-3\step-03
    mvnw quarkus:dev
    ```

Quarkus Dev Services will start a Kafka broker automatically. Open the Dev UI at [http://localhost:8080/q/dev](http://localhost:8080/q/dev){target="_blank"} and look for the **Quarkus Flow** card. You should see three registered workflows: `trip-planner-flow` (the one you just wrote), plus two auto-generated definitions from the `@SequenceAgent` and `@ParallelAgent` interfaces in Steps 01-02. This is the `quarkus-flow-langchain4j` extension at work: it discovers your agentic patterns at build time and registers them as Flow workflow definitions so they show up in the Dev UI and can be composed into larger workflows.

---

## Try It Out

### Trigger a booking event

This workflow is event-started: the first task is a `listen()` waiting for a CloudEvent on the `flow-in` Kafka topic. You trigger it by publishing a booking confirmation CloudEvent to that topic.

Open the Quarkus Dev UI at [http://localhost:8080/q/dev](http://localhost:8080/q/dev){target="_blank"} and navigate to the **Kafka Dev UI** card. Select the `flow-in` topic and publish the following CloudEvent message:

```json
{
  "specversion": "1.0",
  "type": "com.tripplanner.booking.confirmed",
  "source": "workshop/booking",
  "id": "1",
  "datacontenttype": "application/json",
  "data": {
    "destination": "California Coast",
    "days": 5,
    "tripType": "family",
    "travelers": 4,
    "budget": "$3000",
    "preferences": "beach and scenic drives"
  }
}
```

Watch the terminal output. The multi-agent pipeline runs just as before: vehicle selection, itinerary planning, cost estimation, and tips generation. The workflow emits a `com.tripplanner.trip.approval.requested` event with the generated plan.

### Check the suspended workflow

==Open the Quarkus Flow Dev UI== and look at the workflow instances. You should see one instance in **suspended** state, waiting at the `waitApproval` task. The workflow is asleep. No thread is held.

### Approve the trip

Copy the workflow instance ID from the Dev UI, then call the approval endpoint:

```bash
curl -X PUT http://localhost:8080/trip/approve \
  -H "Content-Type: application/json" \
  -H "X-Flow-Instance-Id: <paste-instance-id>" \
  -d '{"status": "approved", "feedback": ""}'
```

The engine receives the approval CloudEvent, matches it to the suspended instance via `flowinstanceid`, and resumes execution. The workflow emits a `com.tripplanner.trip.confirmed` event and completes.

### See what happens on restart

==Trigger another workflow but don't approve it.== While the workflow is suspended, press `Ctrl+C` to stop the application, then start it again with `./mvnw quarkus:dev`. Check the Dev UI. The suspended workflow is gone. In-memory state doesn't survive restarts. In **Step 04**, you'll add a database to make everything persistent.

---

## What's Next?

In this step you wrapped the trip planning pipeline in a Quarkus Flow workflow with a human-in-the-loop checkpoint. The workflow suspends when waiting for customer approval and resumes when the event arrives, without holding any threads. But as you saw, restarting the app loses suspended workflows and all conversation history.

In **Step 04**, you'll add a PostgreSQL database that makes two things persistent at once: a `DatabaseChatMemoryStore` so customers can continue planning the next day, and `quarkus-flow-jpa` so suspended workflows survive restarts with zero changes to the `TripPlannerFlow` class.

[Continue to Step 04 - Persistent State](step-04.md)
