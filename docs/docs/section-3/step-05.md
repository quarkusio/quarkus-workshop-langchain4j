# Step 05 - Voting, Loops, and Adaptive Model Selection

## Parallel evaluation and iterative agent workflows

The customer can return to a saved trip after Step 04, but approval still depends on their own review. A plan might fit the budget while leaving too little time between stops, so Miles of Smiles wants to check its quality before asking the customer to decide.

The planned exercise adds evaluator agents that review the plan in parallel and vote on whether it needs another pass. Their feedback will guide a refinement loop, with an iteration limit to stop repeated revisions. Adaptive model selection will use a faster, cheaper model for straightforward requests and a more capable model when the request or refinement needs it.

The intended check is to follow a plan through evaluation and revision, then inspect whether the loop stopped because the quality threshold was met or the iteration limit was reached.

!!! note "Coming soon"
    This step is under development. The hands-on instructions and verification exercise are not yet available.
