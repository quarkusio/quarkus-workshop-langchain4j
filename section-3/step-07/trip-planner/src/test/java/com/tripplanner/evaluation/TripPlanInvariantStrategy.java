package com.tripplanner.evaluation;

import io.quarkiverse.langchain4j.testing.evaluation.EvaluationResult;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationSample;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic structural checks on the saved trip plan text. No model is
 * consulted: this is the cheap, repeatable gate that catches a broken plan
 * (missing vehicle, non-contiguous days, or a malformed cost) before any
 * expensive judge or similarity run.
 *
 * <p>The checks are expressed against the {@link TripPlanText} rendering, so a
 * plan that would serialize into an invalid shape fails here.
 */
public class TripPlanInvariantStrategy implements EvaluationStrategy<String> {

    private static final Pattern DAY = Pattern.compile("^" + Pattern.quote(TripPlanText.DAY_PREFIX)
            + "(\\d+): .*$", Pattern.MULTILINE);
    private static final int MAX_DAYS = 31;

    @Override
    public EvaluationResult evaluate(EvaluationSample<String> sample, String output) {
        if (output == null || output.isBlank()) {
            return EvaluationResult.failed(0.0, "planner returned no output");
        }
        List<String> violations = new ArrayList<>();
        List<String> invariants = new ArrayList<>();

        checkVehicle(output, violations, invariants);
        checkRoute(output, violations, invariants);
        checkDays(output, violations, invariants);
        checkCost(output, violations, invariants);

        if (violations.isEmpty()) {
            return EvaluationResult.passed(1.0).withMetadata(java.util.Map.of("invariants", invariants));
        }
        return EvaluationResult.failed(0.0, String.join("; ", violations));
    }

    private void checkVehicle(String output, List<String> violations, List<String> invariants) {
        String line = lineStartsWith(output, TripPlanText.VEHICLE_PREFIX);
        if (line == null) {
            violations.add("missing vehicle line");
        } else if (line.substring(TripPlanText.VEHICLE_PREFIX.length()).isBlank()) {
            violations.add("vehicle is blank");
        } else {
            invariants.add("vehicle-present");
        }
    }

    private void checkRoute(String output, List<String> violations, List<String> invariants) {
        String line = lineStartsWith(output, TripPlanText.ROUTE_PREFIX);
        if (line == null) {
            violations.add("missing route line");
        } else if (line.substring(TripPlanText.ROUTE_PREFIX.length()).isBlank()) {
            violations.add("route is blank");
        } else {
            invariants.add("route-present");
        }
    }

    private void checkDays(String output, List<String> violations, List<String> invariants) {
        Matcher m = DAY.matcher(output);
        Set<Integer> days = new TreeSet<>();
        while (m.find()) {
            days.add(Integer.parseInt(m.group(1)));
        }
        if (days.isEmpty()) {
            violations.add("itinerary has no days");
            return;
        }
        int max = days.stream().max(Integer::compare).orElse(0);
        if (max < 1 || max > MAX_DAYS) {
            violations.add("itinerary day range out of bounds: " + days);
            return;
        }
        boolean contiguous = true;
        for (int d = 1; d <= max; d++) {
            if (!days.contains(d)) {
                contiguous = false;
                violations.add("itinerary missing day " + d + " (days: " + days + ")");
                break;
            }
        }
        if (contiguous) {
            invariants.add("itinerary-" + max + "-contiguous-days");
        }
    }

    private void checkCost(String output, List<String> violations, List<String> invariants) {
        String line = lineStartsWith(output, TripPlanText.COST_PREFIX);
        if (line == null) {
            violations.add("missing cost line");
            return;
        }
        String total = line.substring(TripPlanText.COST_PREFIX.length()).trim();
        try {
            int value = Integer.parseInt(total);
            if (value < 0) {
                violations.add("cost total is negative: " + value);
            } else {
                invariants.add("cost-total-valid");
            }
        } catch (NumberFormatException e) {
            violations.add("cost total is not a whole number: " + total);
        }
    }

    private static String lineStartsWith(String text, String prefix) {
        for (String line : text.split("\n", -1)) {
            if (line.startsWith(prefix)) {
                return line;
            }
        }
        return null;
    }
}
