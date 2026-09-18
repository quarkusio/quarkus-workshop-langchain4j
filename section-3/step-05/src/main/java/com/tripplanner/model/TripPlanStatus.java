package com.tripplanner.model;

public record TripPlanStatus(String requestId, String instanceId, TripRequest request, String status,
                             TripPlan plan, BookingConfirmation confirmation, String error, String message) {
    public TripPlanStatus outcome(String state, TripPlan tripPlan, BookingConfirmation booking) {
        return new TripPlanStatus(requestId, instanceId, request, state, tripPlan, booking, null, null);
    }

    public TripPlanStatus failed(Throwable failure) {
        TripError safe = TripError.from(failure, plan != null);
        return new TripPlanStatus(requestId, instanceId, request, "failed", plan, null, safe.error(), safe.message());
    }
}
