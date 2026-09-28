package com.tripplanner.guardrails;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Set;
import java.util.Locale;

@ApplicationScoped
public class TripAppropriatenessGuardrail implements OutputGuardrail {

    private static final Set<String> SMALL_VEHICLE_KEYWORDS = Set.of(
            "sports car", "sport car", "coupé", "coupe", "convertible", "2-seater", "two-seater", "roadster");

    // Fast pre-check for obvious supercars — avoids a judge call for clear-cut cases.
    private static final Set<String> OBVIOUS_LUXURY_BRANDS = Set.of(
            "ferrari", "porsche", "lamborghini", "maserati", "bentley", "rolls-royce", "aston martin", "mclaren");

    // Brands detected by the user preferences ANNOTATE path (unchanged from earlier steps).
    private static final Set<String> LUXURY_BRANDS = Set.of(
            "ferrari", "porsche", "lamborghini", "maserati", "bentley", "rolls-royce", "aston martin", "mclaren",
            "land rover", "range rover", "jaguar", "bmw", "mercedes", "audi", "lexus");

    @Inject
    GuardrailAuditLog auditLog;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    VehicleBudgetJudge budgetJudge;

    @Override
    public OutputGuardrailResult validate(OutputGuardrailRequest guardrailRequest) {
        String text = guardrailRequest.responseFromLLM().aiMessage().text();
        if (text == null || text.isBlank()) {
            auditLog.log("TripAppropriatenessGuardrail", "SKIP", "No text content; tool-call content was not validated");
            return success();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(TripSafetyGuardrail.extractJson(text));
        } catch (Exception e) {
            auditLog.log("TripAppropriatenessGuardrail", "SKIP", "Response is not valid JSON; suitability was not validated");
            return success();
        }

        if (root == null || !root.isObject() || !root.path("type").isTextual()
                || root.path("type").asText().isBlank() || !root.path("model").isTextual()
                || root.path("model").asText().isBlank()) {
            auditLog.log("TripAppropriatenessGuardrail", "SKIP", "Vehicle type or model is missing; suitability was not validated");
            return success();
        }

        var variables = guardrailRequest.requestParams().variables();
        String budget = (String) variables.get("budget");
        String tripType = (String) variables.get("tripType");
        String preferences = variables.get("preferences") instanceof String s ? s : "";
        int travelers;
        try {
            // Prompt variables may contain text or numeric method arguments.
            travelers = Integer.parseInt(String.valueOf(variables.get("travelers")));
        } catch (NumberFormatException e) {
            auditLog.log("TripAppropriatenessGuardrail", "SKIP", "Traveler count is missing or invalid; suitability was not validated");
            return success();
        }
        if (budget == null || budget.isBlank() || tripType == null || tripType.isBlank()) {
            auditLog.log("TripAppropriatenessGuardrail", "SKIP", "Trip variables are missing or incomplete; suitability was not validated");
            return success();
        }

        String vehicleType = root.path("type").asText("").toLowerCase(Locale.ROOT);
        String vehicleModel = root.path("model").asText("").toLowerCase(Locale.ROOT);
        String reasoning = root.path("reasoning").asText("");

        // For economy budgets: fast-path for obvious supercars, then LLM judge for everything else.
        if (budget.toLowerCase(Locale.ROOT).contains("economy")) {
            boolean inappropriate;
            if (isObviousLuxuryBrand(vehicleModel)) {
                inappropriate = true;
            } else {
                try {
                    BudgetVerdict verdict = budgetJudge.evaluate(budget, vehicleType, vehicleModel, reasoning);
                    inappropriate = !verdict.appropriate();
                    auditLog.log("TripAppropriatenessGuardrail",
                            inappropriate ? "JUDGE-FAIL" : "JUDGE-PASS",
                            "Budget judge verdict for '" + vehicleModel + "': " + verdict.reason());
                } catch (Exception e) {
                    auditLog.log("TripAppropriatenessGuardrail", "JUDGE-ERROR",
                            "Budget judge threw an exception for '" + vehicleModel + "' — treating as appropriate: " + e.getMessage());
                    inappropriate = false;
                }
            }

            if (inappropriate) {
                auditLog.log("TripAppropriatenessGuardrail", "REPROMPT",
                        "Vehicle '" + vehicleModel + "' does not match economy budget — asking model to retry with an affordable option");
                return reprompt("The vehicle recommendation does not fit the economy budget. "
                        + "Please recommend an affordable, budget-friendly vehicle instead.",
                        "The previous vehicle recommendation does not fit the economy budget. "
                        + "Respond ONLY with a valid JSON vehicle recommendation object (with fields: type, model, reasoning) "
                        + "for an affordable, budget-friendly, non-luxury vehicle suitable for this trip. Do not explain or acknowledge — just output the JSON.");
            }
        }

        if (travelers >= 4 && isSmallVehicle(vehicleType)) {
            String reason = "Original recommendation '" + vehicleType + "' is too small for " + travelers + " travelers";
            rewriteVehicle((ObjectNode) root, travelers, tripType, reason);
            auditLog.log("TripAppropriatenessGuardrail", "REWRITE", reason + "; returned a generic category recommendation");
            return successWith(AiMessage.from(root.toString()));
        }

        // If the user asked for a luxury brand but the output doesn't contain it, the model
        // already overrode the request on its own. Annotate the result so the UI can show why.
        String requestedBrand = requestedLuxuryBrand(preferences);
        if (requestedBrand != null && !vehicleModel.contains(requestedBrand)) {
            String reason = "Requested brand '" + requestedBrand + "' is not suitable for this trip; a more appropriate vehicle was selected";
            ((ObjectNode) root).put("guardrailOverride", reason);
            auditLog.log("TripAppropriatenessGuardrail", "ANNOTATE",
                    "Preferences mentioned '" + requestedBrand + "' but output is '" + vehicleModel + "' — " + reason);
            return successWith(AiMessage.from(root.toString()));
        }

        auditLog.log("TripAppropriatenessGuardrail", "PASS", "Budget judge approved the vehicle recommendation");
        return success();
    }

    private boolean isSmallVehicle(String vehicleType) {
        return SMALL_VEHICLE_KEYWORDS.stream().anyMatch(vehicleType::contains);
    }

    private boolean isObviousLuxuryBrand(String vehicleModel) {
        return OBVIOUS_LUXURY_BRANDS.stream().anyMatch(vehicleModel::contains);
    }

    private String requestedLuxuryBrand(String preferences) {
        if (preferences == null || preferences.isBlank()) return null;
        String lower = preferences.toLowerCase(Locale.ROOT);
        return LUXURY_BRANDS.stream().filter(lower::contains).findFirst().orElse(null);
    }

    private void rewriteVehicle(ObjectNode vehicle, int travelers, String tripType, String reason) {
        String replacement = switch (tripType.toLowerCase(Locale.ROOT)) {
            case "adventure" -> "SUV";
            case "business" -> "Estate";
            default -> "MPV";
        };
        vehicle.put("type", replacement);
        vehicle.put("model", (replacement.equals("MPV") ? "Family MPV" : replacement)
                + "; specific model subject to availability.");
        vehicle.put("reasoning", "Vehicle corrected by guardrail: original recommendation was too small for "
                + travelers + " travelers. Suggested category: " + replacement
                + ". Confirm seating, luggage capacity, price, and availability with the rental provider.");
        vehicle.put("guardrailOverride", reason);
    }
}
