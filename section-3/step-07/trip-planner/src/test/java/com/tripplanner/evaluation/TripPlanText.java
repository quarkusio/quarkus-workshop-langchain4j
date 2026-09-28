package com.tripplanner.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.model.TripPlan;

public final class TripPlanText {
    private static final ObjectMapper JSON = new ObjectMapper();

    private TripPlanText() {
    }

    public static String render(TripPlan plan) {
        try {
            return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(plan);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot save trip plan", e);
        }
    }

    public static TripPlan read(String output) throws JsonProcessingException {
        return JSON.readValue(output, TripPlan.class);
    }
}
