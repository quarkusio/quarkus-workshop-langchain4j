package com.tripplanner.agentic.workflow;

import com.tripplanner.agentic.agents.ItineraryPlannerAgent;
import com.tripplanner.agentic.agents.VehicleAdvisorAgent;
import com.tripplanner.model.BudgetReview;
import com.tripplanner.model.ItineraryResult;
import com.tripplanner.model.TripPlan;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.ParallelAgent;

public interface ResearchPhase {

    @ParallelAgent(
            description = "Researches vehicle and itinerary in parallel",
            outputKey = "researchComplete",
            subAgents = { VehicleAdvisorAgent.class, ItineraryPlannerAgent.class })
    String research(String destination,
                    Integer days,
                    String tripType,
                    Integer travelers,
                    String budget,
                    String preferences,
                    BudgetReview budgetReview);

    @Output
    static String output(TripPlan.VehicleRecommendation vehicle, ItineraryResult itineraryResult) {
        return "Research complete: %s selected, %d-day itinerary planned".formatted(
                vehicle.model(), itineraryResult.itinerary().size());
    }
}
