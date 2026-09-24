package com.tripplanner.agentic.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tripplanner.model.TripIntelligenceException;

/** Validates supplied MCP data before the research agents consume it. */
public final class DestinationEvidence {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private DestinationEvidence() {}

    public static void validate(String destination, String weather, String pointsOfInterest) {
        try {
            JsonNode forecast = parse(weather);
            if (!forecast.isObject() || !text(forecast, "destination")
                    || !destination.equals(forecast.get("destination").asText())
                    || !text(forecast, "summary") || !text(forecast, "conditions")
                    || !forecast.path("avgTemperatureCelsius").isNumber()
                    || !Double.isFinite(forecast.path("avgTemperatureCelsius").asDouble())
                    || !forecast.path("warnings").isArray()) throw new TripIntelligenceException();
            for (JsonNode warning : forecast.get("warnings")) {
                if (!warning.isTextual() || warning.asText().isBlank()) throw new TripIntelligenceException();
            }
            JsonNode pois = parse(pointsOfInterest).path("entries");
            if (!pois.isArray()) throw new TripIntelligenceException();
            for (JsonNode poi : pois) {
                if (!text(poi, "destination") || !destination.equals(poi.get("destination").asText())
                        || !text(poi, "name") || !text(poi, "category") || !text(poi, "description")
                        || !poi.path("rating").isNumber() || !Double.isFinite(poi.get("rating").asDouble())
                        || poi.get("rating").asDouble() < 0 || poi.get("rating").asDouble() > 5) {
                    throw new TripIntelligenceException();
                }
            }
        } catch (TripIntelligenceException e) {
            throw e;
        } catch (Exception e) {
            throw new TripIntelligenceException();
        }
    }

    private static JsonNode parse(String value) throws Exception {
        if (value == null || value.isBlank() || value.length() > 100_000) throw new TripIntelligenceException();
        return JSON.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .readTree(value);
    }

    private static boolean text(JsonNode node, String field) {
        return node.path(field).isTextual() && !node.get(field).asText().isBlank();
    }
}
