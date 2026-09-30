package com.tripplanner.guardrails;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.model.TripPlan.VehicleRecommendation;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Set;

@ApplicationScoped
public class TripAppropriatenessGuardrail implements OutputGuardrail {

    private static final Set<String> SMALL_VEHICLE_KEYWORDS = Set.of(
            "sports car", "sport car", "coupé", "coupe", "convertible", "2-seater", "two-seater", "roadster");

    private static final Set<String> LUXURY_BRANDS = Set.of(
            "ferrari", "porsche", "lamborghini", "maserati", "bentley", "rolls-royce", "aston martin", "mclaren",
            "land rover", "range rover", "jaguar", "bmw", "mercedes", "audi", "lexus");

    @Inject
    ObjectMapper objectMapper;

    @Override
    public OutputGuardrailResult validate(OutputGuardrailRequest guardrailRequest) {
        String text = guardrailRequest.responseFromLLM().aiMessage().text();
        if (text == null || text.isBlank()) {
            Log.info("🛡️ [TripAppropriatenessGuardrail] SKIP — No text content; tool-call content was not validated");
            return success();
        }

        VehicleRecommendation vehicle;
        try {
            vehicle = objectMapper.readValue(TripSafetyGuardrail.extractJson(text), VehicleRecommendation.class);
        } catch (Exception e) {
            Log.info("🛡️ [TripAppropriatenessGuardrail] SKIP — Response is not valid JSON; suitability was not validated");
            return success();
        }

        if (vehicle == null || isBlank(vehicle.type()) || isBlank(vehicle.model())) {
            Log.info("🛡️ [TripAppropriatenessGuardrail] SKIP — Vehicle type or model is missing; suitability was not validated");
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
            Log.info("🛡️ [TripAppropriatenessGuardrail] SKIP — Traveler count is missing or invalid; suitability was not validated");
            return success();
        }
        if (budget == null || budget.isBlank() || tripType == null || tripType.isBlank()) {
            Log.info("🛡️ [TripAppropriatenessGuardrail] SKIP — Trip variables are missing or incomplete; suitability was not validated");
            return success();
        }

        String vehicleType = vehicle.type().toLowerCase(Locale.ROOT);
        String vehicleModel = vehicle.model().toLowerCase(Locale.ROOT);

        // Check the original model before a generic rewrite can hide a budget violation.
        if (budget.toLowerCase(Locale.ROOT).contains("economy") && isLuxuryBrand(vehicleModel)) {
            Log.infof("🛡️ [TripAppropriatenessGuardrail] REPROMPT — Luxury vehicle '%s' does not match economy budget — asking model to retry with an affordable option", vehicleModel);
            return reprompt("The vehicle recommendation is a luxury vehicle but the budget is economy. "
                    + "Please recommend an affordable, budget-friendly vehicle instead.",
                    "The previous vehicle recommendation is a luxury vehicle that does not fit the economy budget. "
                    + "Respond ONLY with a valid JSON vehicle recommendation object (with fields: type, model, reasoning) "
                    + "for an affordable, budget-friendly, non-luxury vehicle suitable for this trip. Do not explain or acknowledge — just output the JSON.");
        }

        if (travelers >= 4 && isSmallVehicle(vehicleType)) {
            String reason = "Original recommendation '" + vehicleType + "' is too small for " + travelers + " travelers";
            Log.infof("🛡️ [TripAppropriatenessGuardrail] REWRITE — %s; returned a generic category recommendation", reason);
            return successWith(AiMessage.from(toJson(genericVehicle(travelers, tripType, reason))));
        }

        // If the user asked for a luxury brand but the output doesn't contain it, the model
        // already overrode the request on its own. Annotate the result so the UI can show why.
        String requestedBrand = requestedLuxuryBrand(preferences);
        if (requestedBrand != null && !vehicleModel.contains(requestedBrand)) {
            String reason = "Requested brand '" + requestedBrand + "' is not suitable for this trip; a more appropriate vehicle was selected";
            var annotated = new VehicleRecommendation(vehicle.type(), vehicle.model(), vehicle.reasoning(), reason);
            Log.infof("🛡️ [TripAppropriatenessGuardrail] ANNOTATE — Preferences mentioned '%s' but output is '%s' — %s", requestedBrand, vehicleModel, reason);
            return successWith(AiMessage.from(toJson(annotated)));
        }

        Log.info("🛡️ [TripAppropriatenessGuardrail] PASS — No configured small-vehicle or economy-brand rule matched");
        return success();
    }

    private boolean isSmallVehicle(String vehicleType) {
        return SMALL_VEHICLE_KEYWORDS.stream().anyMatch(vehicleType::contains);
    }

    private boolean isLuxuryBrand(String vehicleModel) {
        return LUXURY_BRANDS.stream().anyMatch(vehicleModel::contains);
    }

    private String requestedLuxuryBrand(String preferences) {
        if (preferences == null || preferences.isBlank()) return null;
        String lower = preferences.toLowerCase(Locale.ROOT);
        return LUXURY_BRANDS.stream().filter(lower::contains).findFirst().orElse(null);
    }

    private VehicleRecommendation genericVehicle(int travelers, String tripType, String reason) {
        String replacement = switch (tripType.toLowerCase(Locale.ROOT)) {
            case "adventure" -> "SUV";
            case "business" -> "Estate";
            default -> "MPV";
        };
        return new VehicleRecommendation(
                replacement,
                (replacement.equals("MPV") ? "Family MPV" : replacement) + "; specific model subject to availability.",
                "Vehicle corrected by guardrail: original recommendation was too small for "
                        + travelers + " travelers. Suggested category: " + replacement
                        + ". Confirm seating, luggage capacity, price, and availability with the rental provider.",
                reason);
    }

    private String toJson(VehicleRecommendation vehicle) {
        try {
            return objectMapper.writeValueAsString(vehicle);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
