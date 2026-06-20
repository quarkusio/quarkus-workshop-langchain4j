# Step 03 - Persistent State and Event-Driven Workflows

## Surviving Restarts and Reacting to Events

Trip planning spans multiple sessions — a customer says "I'll think about it" and comes back the next day. The system remembers everything. When a partner later confirms a booking, a Kafka event triggers the workflow to update the trip plan automatically.

In this step, you'll implement a custom `ChatMemoryStore` backed by a database and use Quarkus Flow to make your workflow event-driven with Kafka triggers — all with zero changes to your existing agent code.

!!! note "Coming soon"
    This step is under development. Check back for the full hands-on instructions.
