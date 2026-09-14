# Step 09 - Testing and Evaluation

## Evaluating LLM outputs with similarity scoring and model-based judging

An individual trip can look convincing while hiding a poor vehicle choice or an impractical route. Before offering the planner to customers, Miles of Smiles needs repeatable checks across sample requests, including cases where the model gives a different answer on each run.

The planned exercise combines model-output evaluation with ordinary assertions for deterministic rules. Similarity scoring and model-based judging will help assess generated recommendations, while tests can still check required fields and business constraints directly. We'll inspect the scores alongside the plans to understand what each check catches and what it misses.

!!! note "Coming soon"
    This step is under development. The hands-on instructions and verification exercise are not yet available.
