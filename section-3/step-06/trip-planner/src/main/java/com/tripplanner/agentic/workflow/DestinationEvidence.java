package com.tripplanner.agentic.workflow;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tripplanner.model.TripIntelligenceException;

import java.util.List;

/** Validates supplied MCP data before the research agents consume it. */
public final class DestinationEvidence {
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    record Forecast(String destination, String summary, String conditions,
                    Double avgTemperatureCelsius, List<String> warnings) {}

    record PoiCatalog(List<Poi> entries) {}

    record Poi(String destination, String name, String category, String description, Double rating) {}

    private DestinationEvidence() {}

    public static void validate(String destination, String weather, String pointsOfInterest) {
        Forecast forecast = parse(weather, Forecast.class);
        if (forecast == null || !destination.equals(forecast.destination())
                || isBlank(forecast.summary()) || isBlank(forecast.conditions())
                || !isFinite(forecast.avgTemperatureCelsius())
                || forecast.warnings() == null || forecast.warnings().stream().anyMatch(DestinationEvidence::isBlank)) {
            throw new TripIntelligenceException();
        }

        PoiCatalog catalog = parse(pointsOfInterest, PoiCatalog.class);
        if (catalog == null || catalog.entries() == null) throw new TripIntelligenceException();
        for (Poi poi : catalog.entries()) {
            if (poi == null || !destination.equals(poi.destination()) || isBlank(poi.name())
                    || isBlank(poi.category()) || isBlank(poi.description())
                    || !isFinite(poi.rating()) || poi.rating() < 0 || poi.rating() > 5) {
                throw new TripIntelligenceException();
            }
        }
    }

    private static <T> T parse(String value, Class<T> type) {
        if (value == null || value.isBlank() || value.length() > 100_000) throw new TripIntelligenceException();
        try {
            return JSON.readValue(value, type);
        } catch (Exception e) {
            throw new TripIntelligenceException();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isFinite(Double value) {
        return value != null && Double.isFinite(value);
    }
}
