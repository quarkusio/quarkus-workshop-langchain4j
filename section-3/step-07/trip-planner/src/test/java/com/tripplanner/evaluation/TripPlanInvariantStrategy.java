package com.tripplanner.evaluation;

import com.tripplanner.model.TripPlan;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationResult;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationSample;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationStrategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TripPlanInvariantStrategy implements EvaluationStrategy<String> {

    @Override
    public EvaluationResult evaluate(EvaluationSample<String> sample, String output) {
        if (output == null || output.isBlank()) {
            return EvaluationResult.failed(0, "planner returned no output");
        }
        try {
            return evaluate(sample, TripPlanText.read(output));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return EvaluationResult.failed(0, "planner returned invalid plan JSON");
        }
    }

    public EvaluationResult evaluate(EvaluationSample<String> sample, TripPlan plan) {
        if (plan == null) return EvaluationResult.failed(0, "planner returned no plan");
        int requestedDays = Integer.parseInt(sample.parameters().get(2).toString());
        if (requestedDays < 1 || requestedDays > 30) {
            throw new IllegalArgumentException("Sample duration must be between 1 and 30 days");
        }
        List<String> errors = new ArrayList<>();
        if (plan.vehicle() == null) {
            errors.add("missing vehicle");
        } else {
            requireText(plan.vehicle().type(), "vehicle type", errors);
            requireText(plan.vehicle().model(), "vehicle model", errors);
            requireText(plan.vehicle().reasoning(), "vehicle reasoning", errors);
        }
        requireText(plan.routeOverview(), "route", errors);
        if (plan.itinerary() == null || plan.itinerary().size() != requestedDays) {
            errors.add("itinerary must contain " + requestedDays + " days");
        }
        var numbers = new HashSet<Integer>();
        if (plan.itinerary() != null) {
            for (var day : plan.itinerary()) {
                if (day == null) {
                    errors.add("missing itinerary entry");
                    continue;
                }
                if (!numbers.add(day.day())) errors.add("duplicate day " + day.day());
                if (day.day() < 1 || day.day() > requestedDays) errors.add("invalid day " + day.day());
                requireText(day.title(), "day title", errors);
                requireText(day.description(), "day description", errors);
                requireText(day.overnightStop(), "overnight stop", errors);
            }
        }
        for (int day = 1; day <= requestedDays; day++) {
            if (!numbers.contains(day)) errors.add("itinerary missing day " + day);
        }
        if (plan.costs() == null) {
            errors.add("missing costs");
        } else {
            String total = plan.costs().total();
            if (total == null || !total.matches("\\s*(?:€|EUR)?\\s*\\d+(?:[.,]\\d+)*(?:\\s*(?:€|EUR))?\\s*")) {
                errors.add("cost total must be a non-negative EUR amount");
            }
        }
        return errors.isEmpty() ? EvaluationResult.passed(1)
                : EvaluationResult.failed(0, String.join("; ", errors));
    }

    private static void requireText(String value, String field, List<String> errors) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("null")) errors.add("missing " + field);
    }
}
