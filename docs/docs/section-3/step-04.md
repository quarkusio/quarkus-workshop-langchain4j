# Step 04 - Voting, Loops, and Adaptive Model Selection

## Multi-Agent Assessment with Consensus

Step 03 introduced event-driven human approval. In this step, the focus shifts to iterative decision-making. Imagine a 7-day California coast trip for a family of 4: one agent prefers an SUV for comfort, another prefers a sedan for cost, and a third suggests a hybrid SUV for fuel efficiency. Instead of accepting the first answer, the system evaluates those recommendations, votes, and loops until it reaches a stable result.

You will combine `VotingPlanner`, `@LoopAgent` with `@ExitCondition`, and `@ChatModelSupplier` to build a workflow that supports parallel assessment, iterative convergence, and cost-aware model routing. Simple requests can stay on a faster model, while more complex requests can escalate to a stronger one.

!!! note "Coming soon"
    This step is under development. Check back for the full hands-on instructions.
