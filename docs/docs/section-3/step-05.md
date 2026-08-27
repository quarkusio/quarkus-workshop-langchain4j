# Step 05 - Voting, Loops, and Adaptive Model Selection

## Iterative Plan Refinement

Step 03 asked "should we finalize this plan?" and waited for a single human approval. This step introduces iterative convergence: multiple evaluator agents assess the plan in parallel, vote on whether it meets quality thresholds, and the workflow loops back to refine until the evaluators agree — or a maximum iteration guard fires.

Alongside the voting loop, adaptive model selection routes simple requests through a faster, cheaper model and escalates to a stronger model when complexity or late-loop refinement demands it.

In this step, you'll implement parallel evaluator agents, a vote-aggregation structure, a loop condition with a max-iteration safety guard, and a model supplier that routes based on request complexity.

!!! note "Coming soon"
    This step is under development. Check back for the full hands-on instructions.
