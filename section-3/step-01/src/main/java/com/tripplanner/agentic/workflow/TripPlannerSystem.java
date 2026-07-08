package com.tripplanner.agentic.workflow;

import com.tripplanner.agentic.agents.BudgetReviewInitializer;
import com.tripplanner.agentic.agents.TipsGeneratorAgent;
import com.tripplanner.model.ItineraryResult;
import com.tripplanner.model.TripPlan;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.MonitoredAgent;

import java.util.List;

public interface TripPlannerSystem extends MonitoredAgent {

    @SequenceAgent(
            outputKey = "tripPlan",
            subAgents = {
                    BudgetReviewInitializer.class,
                    BudgetAwareResearch.class,
                    TipsGeneratorAgent.class
            })
    TripPlan planTrip(String destination,
                      Integer days,
                      String tripType,
                      Integer travelers,
                      String budget,
                      String preferences);

    @Output
    static TripPlan output(TripPlan.VehicleRecommendation vehicle,
                           ItineraryResult itineraryResult,
                           TripPlan.CostEstimate costs,
                           List<String> tips) {
        return new TripPlan(
                vehicle,
                itineraryResult.routeOverview(),
                itineraryResult.itinerary(),
                costs,
                tips);
    }
}
