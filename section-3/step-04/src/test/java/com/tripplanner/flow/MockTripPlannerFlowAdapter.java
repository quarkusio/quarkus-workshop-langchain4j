package com.tripplanner.flow;

import com.tripplanner.agentic.flow.TripPlannerFlowAdapter;
import com.tripplanner.model.BookingConfirmation;
import com.tripplanner.model.TripApproval;
import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripRequest;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@Mock
@ApplicationScoped
public class MockTripPlannerFlowAdapter extends TripPlannerFlowAdapter {

    @Override
    public TripPlan planFromRequest(TripRequest request) {
        return new TripPlan(
                new TripPlan.VehicleRecommendation("SUV", "Volvo XC60", "Spacious and comfortable for families"),
                "A scenic coastal route from start to finish",
                List.of(
                        new TripPlan.DayItinerary(1, "Arrival Day", "Settle in and explore the harbour", "Coastal Hotel"),
                        new TripPlan.DayItinerary(2, "Coastal Drive", "Drive along the cliff road with stops", "Seaside Inn")
                ),
                new TripPlan.CostEstimate("€80/day", "€60", "€20", "€150/night", "€50/day", "€30/day", "€800 total"),
                List.of("Book accommodation in advance", "Bring sunscreen")
        );
    }

    @Override
    public BookingConfirmation finalizeBooking(TripApproval approval) {
        return new BookingConfirmation("MOS-TEST1234", "Your trip is confirmed. Vehicle reserved.");
    }
}
