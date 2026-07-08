package com.tripplanner.agentic.agents;

import com.tripplanner.model.BudgetReview;
import dev.langchain4j.agentic.Agent;

public class BudgetReviewInitializer {

    @Agent(description = "Initializes budget review state before the loop",
           outputKey = "budgetReview")
    public static BudgetReview initialize(String budget) {
        return new BudgetReview(false,
                "No previous budget review. Plan freely within the given budget range.");
    }
}
