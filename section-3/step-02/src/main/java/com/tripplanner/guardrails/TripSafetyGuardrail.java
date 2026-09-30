package com.tripplanner.guardrails;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.model.ItineraryResult;
import com.tripplanner.model.TripPlan.DayItinerary;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class TripSafetyGuardrail implements OutputGuardrail {

    private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
            "war zone", "conflict area", "active military", "travel ban",
            "do not travel", "armed conflict", "combat zone", "no-go zone");

    @Inject
    ObjectMapper objectMapper;

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String text = responseFromLLM.text();
        if (text == null || text.isBlank()) {
            Log.info("🛡️ [TripSafetyGuardrail] SKIP — No text content; tool-call content was not validated");
            return success();
        }

        ItineraryResult result;
        try {
            result = objectMapper.readValue(extractJson(text), ItineraryResult.class);
        } catch (Exception e) {
            Log.info("🛡️ [TripSafetyGuardrail] RETRY — Response is not valid JSON");
            return retry("The response is not valid JSON. Please return a valid JSON object matching the ItineraryResult format.");
        }

        if (result.itinerary() == null || result.itinerary().isEmpty()) {
            Log.info("🛡️ [TripSafetyGuardrail] RETRY — Itinerary is missing or empty");
            return retry("The trip plan must include a day-by-day itinerary. Please provide at least one day.");
        }

        String planText = Stream.concat(
                        Stream.of(result.routeOverview()),
                        result.itinerary().stream().map(DayItinerary::description))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "))
                .toLowerCase();

        List<String> dangerousMatches = DANGEROUS_KEYWORDS.stream().filter(planText::contains).toList();
        if (!dangerousMatches.isEmpty()) {
            String matched = String.join(", ", dangerousMatches);
            Log.infof("🛡️ [TripSafetyGuardrail] RETRY — Dangerous content detected: %s", matched);
            return retry("The trip plan references potentially dangerous areas (" + matched
                    + "). Please regenerate the plan avoiding these areas and suggesting safe alternatives.");
        }

        Log.info("🛡️ [TripSafetyGuardrail] PASS — Nonempty itinerary; no configured phrases in route overview or day descriptions");
        return success();
    }

    static String extractJson(String text) {
        String trimmed = text.strip();
        if (trimmed.startsWith("{")) {
            return trimmed;
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
