package com.tripplanner.agentic.workflow;

import com.tripplanner.agentic.agents.CostEstimatorAgent;
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
                    ResearchPhase.class,
                    CostEstimatorAgent.class,
                    TipsGeneratorAgent.class
            })
    TripPlan planTrip(String destination,
                      int days,
                      String tripType,
                      int travelers,
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
