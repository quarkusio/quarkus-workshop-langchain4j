package com.tripplanner.agentic.flow;

import com.tripplanner.agentic.workflow.TripPlannerSystem;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TripPlannerFlowAdapter {

    @Inject
    TripPlannerSystem tripPlannerSystem;

    public TripPlan planFromRequest(TripRequest request) {
        return tripPlannerSystem.planTrip(
                request.destination(),
                request.days(),
                request.tripType(),
                request.travelers(),
                request.budget(),
                request.preferences());
    }
}
