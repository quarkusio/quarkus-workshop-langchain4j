package com.tripplanner.guardrails;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class TripSafetyGuardrail implements OutputGuardrail {

    private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
            "war zone", "conflict area", "active military", "travel ban",
            "do not travel", "armed conflict", "combat zone", "no-go zone");

    @Inject
    GuardrailAuditLog auditLog;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String text = responseFromLLM.text();
        if (text == null || text.isBlank()) {
            auditLog.log("TripSafetyGuardrail", "PASS", "No text content to validate (structured output via tool calls)");
            return success();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(extractJson(text));
        } catch (Exception e) {
            auditLog.log("TripSafetyGuardrail", "RETRY", "Response is not valid JSON");
            return retry("The response is not valid JSON. Please return a valid JSON object matching the TripPlan format.");
        }

        JsonNode itinerary = root.path("itinerary");
        if (!itinerary.isArray() || itinerary.isEmpty()) {
            auditLog.log("TripSafetyGuardrail", "RETRY", "Itinerary is missing or empty");
            return retry("The trip plan must include a day-by-day itinerary. Please provide at least one day.");
        }

        List<String> dangerousMatches = findDangerousContent(root);
        if (!dangerousMatches.isEmpty()) {
            String matched = String.join(", ", dangerousMatches);
            auditLog.log("TripSafetyGuardrail", "RETRY", "Dangerous content detected: " + matched);
            return retry("The trip plan references potentially dangerous areas (" + matched
                    + "). Please regenerate the plan avoiding these areas and suggesting safe alternatives.");
        }

        auditLog.log("TripSafetyGuardrail", "PASS", "All safety checks passed");
        return success();
    }

    private List<String> findDangerousContent(JsonNode root) {
        List<String> matches = new ArrayList<>();
        String routeOverview = root.path("routeOverview").asText("").toLowerCase();
        checkForDangerousKeywords(routeOverview, matches);

        JsonNode itinerary = root.path("itinerary");
        if (itinerary.isArray()) {
            for (JsonNode day : itinerary) {
                String description = day.path("description").asText("").toLowerCase();
                checkForDangerousKeywords(description, matches);
            }
        }
        return matches;
    }

    private void checkForDangerousKeywords(String text, List<String> matches) {
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (text.contains(keyword) && !matches.contains(keyword)) {
                matches.add(keyword);
            }
        }
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
