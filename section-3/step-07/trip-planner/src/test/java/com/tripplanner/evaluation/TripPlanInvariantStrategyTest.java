package com.tripplanner.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripPlan.CostEstimate;
import com.tripplanner.model.TripPlan.DayItinerary;
import com.tripplanner.model.TripPlan.VehicleRecommendation;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationResult;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationSample;
import io.quarkiverse.langchain4j.testing.evaluation.Parameters;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The invariant strategy is a deterministic gate: it must accept a structurally
 * valid plan and reject every kind of broken plan, with no model involved.
 */
class TripPlanInvariantStrategyTest {

    private final TripPlanInvariantStrategy strategy = new TripPlanInvariantStrategy();

    @Test
    void aValidThreeDayPlanPasses() {
        EvaluationResult result = strategy.evaluate(sample("valid"), render(validPlan(3)));

        assertTrue(result.passed());
        assertEquals(1.0, result.score());
        @SuppressWarnings("unchecked")
        List<String> invariants = (List<String>) result.metadata().get("invariants");
        assertNotNull(invariants);
        assertTrue(invariants.contains("vehicle-present"));
        assertTrue(invariants.contains("route-present"));
        assertTrue(invariants.contains("itinerary-3-contiguous-days"));
        assertTrue(invariants.contains("cost-total-valid"));
    }

    @Test
    void everyKnownBadOutputFails() {
        for (Map<String, Object> bad : KnownBadOutputs.load()) {
            EvaluationResult result = strategy.evaluate(sample((String) bad.get("name")), (String) bad.get("output"));

            assertFalse(result.passed(), "known-bad '" + bad.get("name") + "' must fail: " + result.explanation());
            assertEquals(0.0, result.score());
            assertTrue(result.explanation().contains((String) bad.get("reason")),
                    "explanation should mention the violation for '" + bad.get("name") + "': " + result.explanation());
        }
    }

    @Test
    void aGapInTheItineraryIsNamed() {
        TripPlan plan = new TripPlan(
                new VehicleRecommendation("MPV", "Family MPV", "Room for two"),
                "Coastal route",
                List.of(
                        new DayItinerary(1, "Arrival", "d", "Hotel"),
                        new DayItinerary(3, "Departure", "d", "Hotel")),
                new CostEstimate("90", "10", "0", "100", "50", "20", "310"));

        EvaluationResult result = strategy.evaluate(sample("gap"), render(plan));

        assertFalse(result.passed());
        assertTrue(result.explanation().contains("day 2"));
    }

    @Test
    void dayCountIsDerivedFromThePlanNotHardcoded() {
        EvaluationResult result = strategy.evaluate(sample("five-days"), render(validPlan(5)));

        assertTrue(result.passed());
        @SuppressWarnings("unchecked")
        List<String> invariants = (List<String>) result.metadata().get("invariants");
        assertTrue(invariants.contains("itinerary-5-contiguous-days"));
    }

    private static TripPlan validPlan(int days) {
        return new TripPlan(
                new VehicleRecommendation("MPV", "Family MPV", "Room for two travelers and luggage"),
                "Coastal route through the region",
                IntStream.rangeClosed(1, days)
                        .mapToObj(d -> new DayItinerary(d, "Day " + d, "description", "Hotel"))
                        .collect(Collectors.toList()),
                new CostEstimate("90", "10", "0", "100", "50", "20", "310"));
    }

    private static String render(TripPlan plan) {
        return TripPlanText.render(plan);
    }

    private static EvaluationSample<String> sample(String name) {
        return EvaluationSample.<String>builder()
                .withName(name)
                .withParameters(Parameters.of("Rome", "2027-07-10", "3", "family", "2", "moderate", "coastal towns"))
                .withExpectedOutput("")
                .build();
    }
}
