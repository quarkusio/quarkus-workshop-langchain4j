package com.tripplanner.agentic.workflow;

import com.tripplanner.agentic.agents.BudgetReviewerAgent;
import com.tripplanner.agentic.agents.CostEstimatorAgent;
import com.tripplanner.model.BudgetReview;
import dev.langchain4j.agentic.declarative.ExitCondition;
import dev.langchain4j.agentic.declarative.LoopAgent;

public interface BudgetAwareResearch {

    @LoopAgent(
            description = "Iterates on research and cost estimation until costs fit the budget",
            outputKey = "budgetReview",
            maxIterations = 3,
            subAgents = {
                    ResearchPhase.class,
                    CostEstimatorAgent.class,
                    BudgetReviewerAgent.class
            })
    BudgetReview research(String destination,
                          Integer days,
                          String tripType,
                          Integer travelers,
                          String budget,
                          String preferences,
                          BudgetReview budgetReview);

    @ExitCondition
    static boolean withinBudget(BudgetReview budgetReview) {
        return budgetReview.approved();
    }
}
