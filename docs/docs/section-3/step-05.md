# Step 05 - Voting, Loops, and Adaptive Model Selection

## Parallel evaluation and iterative agent workflows

The customer can return to a saved trip after Step 04, but approval still depends on their own review. Miles of Smiles wants to check whether its recommendations are worth the customer's money, including the rental extras it proposes. A convenience evaluator might favor a beach umbrella for several beach days, while a budget evaluator questions the expense for a single short stop.

Evaluator agents can review the plan in parallel, assessing the extras against the itinerary, customer preferences, and rental prices before voting on whether it needs another pass. Their feedback guides a refinement loop that stops when the plan meets a quality threshold or reaches an iteration limit. Adaptive model selection allows straightforward requests to use a faster, cheaper model while more demanding revisions use a more capable one.

An unnecessary umbrella rental gives the evaluators a concrete reason to request a revision. Safety requirements, such as a suitable child restraint or equipment required for a particular road, cannot be overridden by a vote about cost or convenience.

!!! note "Coming soon"
    This step is under development.
