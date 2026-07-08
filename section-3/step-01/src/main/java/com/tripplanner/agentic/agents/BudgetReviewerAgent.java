package com.tripplanner.agentic.agents;

import com.tripplanner.model.BudgetReview;
import com.tripplanner.model.TripPlan;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;

public interface BudgetReviewerAgent {

    @UserMessage("""
            You are a budget compliance reviewer for trip plans.
            Compare the estimated total cost against the stated budget range
            and determine if the plan is affordable.

            Estimated costs: {costs}
            Budget range: {budget}

            If the estimated total fits within the budget range, set approved to true
            and provide a brief confirmation.

            If the estimated total exceeds the budget range, set approved to false
            and provide specific, actionable cost reduction hints, for example:
            - Suggest a cheaper vehicle category
            - Recommend budget-friendly accommodation alternatives
            - Suggest reducing paid activities in favor of free ones
            - Recommend cheaper dining options
            """)
    @Agent(description = "Reviews trip costs against the budget and provides reduction hints if over budget",
           outputKey = "budgetReview")
    BudgetReview reviewBudget(TripPlan.CostEstimate costs,
                              String budget);
}
