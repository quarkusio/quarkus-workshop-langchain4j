package com.tripplanner.evaluation;

import com.tripplanner.model.TripPlan;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationSample;
import io.quarkiverse.langchain4j.testing.evaluation.Parameters;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class TripPlanInvariantStrategyTest {
    private final TripPlanInvariantStrategy strategy = new TripPlanInvariantStrategy();

    @Test
    void acceptsCurrencyAndDecimalTotals() {
        for (String total : List.of("310", "€310", "EUR 310.50", "310,50 EUR")) {
            var plan = validPlan(3);
            var withCost = new TripPlan(plan.vehicle(), plan.routeOverview(), plan.itinerary(),
                    new TripPlan.CostEstimate("90", "10", "0", "100", "50", "20", total));
            assertTrue(strategy.evaluate(sample(3), withCost).passed(), total);
        }
    }

    @Test
    void rejectsWrongCountDuplicateAndOutOfRangeDays() {
        assertFalse(strategy.evaluate(sample(7), validPlan(1)).passed());
        var plan = validPlan(3);
        for (List<TripPlan.DayItinerary> days : List.of(
                List.of(day(1), day(1), day(3)), List.of(day(0), day(1), day(2)),
                List.of(day(1), day(2), day(4)))) {
            assertFalse(strategy.evaluate(sample(3),
                    new TripPlan(plan.vehicle(), plan.routeOverview(), days, plan.costs())).passed());
        }
    }

    @Test
    void rejectsMissingFieldsWithoutCrashing() {
        var plan = validPlan(3);
        for (TripPlan broken : List.of(
                new TripPlan(null, plan.routeOverview(), plan.itinerary(), plan.costs()),
                new TripPlan(plan.vehicle(), null, plan.itinerary(), plan.costs()),
                new TripPlan(plan.vehicle(), plan.routeOverview(), null, plan.costs()),
                new TripPlan(plan.vehicle(), plan.routeOverview(), plan.itinerary(), null),
                new TripPlan(new TripPlan.VehicleRecommendation("MPV", null, "Room"),
                        "Rome", List.of(new TripPlan.DayItinerary(1, "Arrival", null, null)), plan.costs()))) {
            assertFalse(strategy.evaluate(sample(3), TripPlanText.render(broken)).passed());
        }
        assertFalse(strategy.evaluate(sample(3), "null").passed());
        assertFalse(strategy.evaluate(sample(3), "not JSON").passed());
    }

    @Test
    void savedPlanRetainsDetailsAndKnownBadFixturesFail() {
        assertTrue(TripPlanText.render(validPlan(3)).contains("overnightStop"));
        assertTrue(strategy.evaluate(sample(3), TripPlanText.render(validPlan(3))).passed());
        for (var bad : KnownBadOutputs.load()) {
            var result = strategy.evaluate(sample(3), (String) bad.get("output"));
            assertFalse(result.passed(), bad.get("name").toString());
            assertTrue(result.explanation().contains((String) bad.get("reason")), result.explanation());
        }
    }

    static TripPlan validPlan(int days) {
        return new TripPlan(new TripPlan.VehicleRecommendation("MPV", "Family MPV", "Room for luggage"),
                "Rome", IntStream.rangeClosed(1, days).mapToObj(TripPlanInvariantStrategyTest::day).toList(),
                new TripPlan.CostEstimate("€90/day", "€10", "€0", "€100", "€50", "€20", "€310"));
    }

    static EvaluationSample<String> sample(int days) {
        return EvaluationSample.<String>builder().withName("test")
                .withParameters(Parameters.of("Rome", "2027-07-10", String.valueOf(days), "family", "2", "moderate", "parks"))
                .withExpectedOutput("Follow the request and supplied evidence.").build();
    }

    private static TripPlan.DayItinerary day(int day) {
        return new TripPlan.DayItinerary(day, "Day " + day, "Explore on foot", "Hotel");
    }
}
