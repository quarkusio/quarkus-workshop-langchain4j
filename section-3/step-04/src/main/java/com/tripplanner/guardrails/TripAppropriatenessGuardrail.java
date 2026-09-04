package com.tripplanner.guardrails;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tripplanner.model.TripRequest;
import com.tripplanner.model.TripRequestContext;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Set;

@ApplicationScoped
public class TripAppropriatenessGuardrail implements OutputGuardrail {

    private static final Set<String> SMALL_VEHICLE_KEYWORDS = Set.of(
            "sports car", "sport car", "coupé", "coupe", "convertible", "2-seater", "two-seater", "roadster");

    private static final Set<String> LUXURY_BRANDS = Set.of(
            "ferrari", "porsche", "lamborghini", "maserati", "bentley", "rolls-royce", "aston martin", "mclaren");

    @Inject
    GuardrailAuditLog auditLog;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    TripRequestContext tripRequestContext;

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String text = responseFromLLM.text();
        if (text == null || text.isBlank()) {
            return success();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(TripSafetyGuardrail.extractJson(text));
        } catch (Exception e) {
            return success();
        }

        TripRequest request = tripRequestContext.get();
        if (request == null) {
            auditLog.log("TripAppropriatenessGuardrail", "PASS", "No request context available, skipping checks");
            return success();
        }

        String vehicleType = root.path("type").asText("").toLowerCase();
        String vehicleModel = root.path("model").asText("").toLowerCase();

        if (request.travelers() >= 4 && isSmallVehicle(vehicleType)) {
            rewriteVehicle((ObjectNode) root, request);
            auditLog.log("TripAppropriatenessGuardrail", "REWRITE",
                    "Vehicle type '" + vehicleType + "' is too small for " + request.travelers() + " travelers");
            return successWith(AiMessage.from(root.toString()));
        }

        if (request.budget().toLowerCase().contains("economy") && isLuxuryBrand(vehicleModel)) {
            auditLog.log("TripAppropriatenessGuardrail", "REPROMPT",
                    "Luxury vehicle '" + vehicleModel + "' does not match economy budget");
            return reprompt("The vehicle recommendation is a luxury vehicle but the budget is economy. "
                    + "Please recommend an affordable, budget-friendly vehicle instead.",
                    "You are a vehicle advisor for road trips. You MUST recommend only budget-friendly, "
                    + "affordable vehicles. Never suggest luxury, premium, or sports brands.");
        }

        auditLog.log("TripAppropriatenessGuardrail", "PASS", "All appropriateness checks passed");
        return success();
    }

    private boolean isSmallVehicle(String vehicleType) {
        return SMALL_VEHICLE_KEYWORDS.stream().anyMatch(vehicleType::contains);
    }

    private boolean isLuxuryBrand(String vehicleModel) {
        return LUXURY_BRANDS.stream().anyMatch(vehicleModel::contains);
    }

    private void rewriteVehicle(ObjectNode vehicle, TripRequest request) {
        String replacement = switch (request.tripType().toLowerCase()) {
            case "adventure" -> "SUV";
            case "business" -> "Estate";
            default -> "MPV";
        };
        vehicle.put("type", replacement);
        vehicle.put("reasoning", "Vehicle upgraded by guardrail: original recommendation was too small for "
                + request.travelers() + " travelers. Replaced with a " + replacement + ".");
    }
}
