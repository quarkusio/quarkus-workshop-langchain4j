package com.tripplanner.evaluation;

import com.tripplanner.model.TripPlan;
import com.tripplanner.model.TripPlan.DayItinerary;
import com.tripplanner.model.TripPlan.VehicleRecommendation;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders a {@link TripPlan} to a deterministic, line-oriented text form.
 *
 * <p>Every evaluation in this step operates on this text (the "saved output"),
 * so the invariant checks, the judge comparison, and the fixtures all agree on
 * one representation. The form is stable: one line per field, one line per day.
 */
public final class TripPlanText {

    public static final String VEHICLE_PREFIX = "Vehicle: ";
    public static final String DAY_PREFIX = "Day ";
    public static final String ROUTE_PREFIX = "Route: ";
    public static final String COST_PREFIX = "Cost total: ";

    private TripPlanText() {
    }

    public static String render(TripPlan plan) {
        return render(plan, List.of());
    }

    public static String render(TripPlan plan, List<String> evidence) {
        StringBuilder sb = new StringBuilder();
        appendVehicle(sb, plan.vehicle());
        sb.append(ROUTE_PREFIX).append(plan.routeOverview()).append('\n');
        for (DayItinerary day : plan.itinerary()) {
            sb.append(DAY_PREFIX).append(day.day()).append(": ").append(day.title()).append('\n');
        }
        sb.append(COST_PREFIX).append(plan.costs().total()).append('\n');
        if (!evidence.isEmpty()) {
            sb.append("Evidence:\n");
            for (String line : evidence) {
                sb.append("  - ").append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private static void appendVehicle(StringBuilder sb, VehicleRecommendation vehicle) {
        if (vehicle == null) {
            sb.append(VEHICLE_PREFIX).append('\n');
            return;
        }
        sb.append(VEHICLE_PREFIX).append(vehicle.model());
        if (vehicle.reasoning() != null && !vehicle.reasoning().isBlank()) {
            sb.append(" (").append(vehicle.reasoning()).append(')');
        }
        sb.append('\n');
    }
}
