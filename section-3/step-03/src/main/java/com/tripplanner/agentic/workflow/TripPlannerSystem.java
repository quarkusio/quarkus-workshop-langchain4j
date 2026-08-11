package com.tripplanner.agentic.workflow;

import com.tripplanner.agentic.agents.CostEstimatorAgent;
import com.tripplanner.model.ItineraryResult;
import com.tripplanner.model.TripPlan;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.MonitoredAgent;

public interface TripPlannerSystem extends MonitoredAgent {

    @SequenceAgent(
            outputKey = "tripPlan",
            subAgents = {
                    ResearchPhase.class,
                    CostEstimatorAgent.class
            })
    TripPlan planTrip(String destination,
                      String startDate,
                      Integer days,
                      String tripType,
                      Integer travelers,
                      String budget,
                      String preferences);

    @Output
    static TripPlan output(TripPlan.VehicleRecommendation vehicle,
                           ItineraryResult itineraryResult,
                           TripPlan.CostEstimate costs) {
        return new TripPlan(
                vehicle,
                itineraryResult.routeOverview(),
                itineraryResult.itinerary(),
                costs,
                itineraryResult.tips());
    }
}
