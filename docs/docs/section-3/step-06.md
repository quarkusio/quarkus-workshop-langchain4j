# Step 06 - Custom Orchestration with PlannerAgent

## Custom control flow with the `Planner` API

The evaluation loop from Step 05 can revise a plan, but a customer's next request may affect only part of it. After reviewing a family trip, the customer says, "Keep the route, but remove the child-seat rental. We're bringing our own." The planner needs to remove the rental charge without generating another itinerary, while retaining the advice to confirm that the family's seat suits the child and vehicle.

A custom planner can choose the next action from the trip goal and the results already available. The `Planner` lifecycle methods control these decisions, allowing an extras-only change to reuse the route while a changed destination calls for a new itinerary. The execution trace shows which agents ran again and which earlier results were reused.

!!! note "Coming soon"
    This step is under development.
