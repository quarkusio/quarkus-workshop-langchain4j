package com.tripplanner.agentic.flow;

import com.tripplanner.agentic.workflow.TripPlannerSystem;
import com.tripplanner.model.BookingConfirmation;
import com.tripplanner.model.TripApproval;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class TripPlannerFlowAdapter {

    @Inject
    TripPlannerSystem tripPlannerSystem;

    public TripPlan planFromRequest(TripRequest request) {
        return tripPlannerSystem.planTrip(
                request.destination(),
                request.startDate(),
                String.valueOf(request.days()),
                request.tripType(),
                String.valueOf(request.travelers()),
                request.budget(),
                request.preferences());
    }

    public BookingConfirmation finalizeBooking(TripApproval approval) {
        return new BookingConfirmation(
                "MOS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "Your trip is confirmed. Vehicle reserved.");
    }
}
