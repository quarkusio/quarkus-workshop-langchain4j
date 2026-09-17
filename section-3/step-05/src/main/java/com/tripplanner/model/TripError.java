package com.tripplanner.model;

import dev.langchain4j.guardrail.GuardrailException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public record TripError(String error, String message) {
    public static TripError from(Throwable failure, boolean finalizing) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof GuardrailException) {
                return new TripError("guardrail_violation",
                        finalizing
                                ? "The simulated booking could not pass the recommendation checks. Your trip plan is still available."
                                : "The trip plan could not pass the recommendation checks. Please revise your trip details and try again.");
            }
        }
        return finalizing
                ? new TripError("finalization_failed", "Could not finalize the simulated booking. Your trip plan is still available.")
                : new TripError("planning_failed", "Could not generate the trip plan. Please try again later.");
    }

    public int httpStatus() {
        return "guardrail_violation".equals(error) ? 422 : 500;
    }
}
