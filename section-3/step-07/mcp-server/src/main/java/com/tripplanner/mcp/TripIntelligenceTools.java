package com.tripplanner.mcp;

import com.tripplanner.mcp.model.PointOfInterest;
import com.tripplanner.mcp.model.WeatherForecast;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDate;
import java.util.List;

public class TripIntelligenceTools {

    @ConfigProperty(name = "trip.intelligence.scenario", defaultValue = "sunny")
    String scenario;

    @Tool(description = "Get controlled workshop weather data for a trip; this is not a live forecast")
    WeatherForecast getWeatherForecast(
            @ToolArg(description = "Trip destination city or region") String destination,
            @ToolArg(description = "Trip start date in yyyy-MM-dd format") String startDate,
            @ToolArg(description = "Trip duration as a decimal string from 1 to 30") String days) {
        requireText(destination);
        LocalDate.parse(startDate);
        int duration = Integer.parseInt(days);
        if (duration < 1 || duration > 30) throw new IllegalArgumentException("Days must be between 1 and 30");
        if ("timeout".equals(scenario)) {
            try {
                Thread.sleep(15_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Fixture interrupted");
            }
        }
        if ("malformed-response".equals(scenario)) {
            return new WeatherForecast(destination, "", 20, "", null);
        }
        boolean severe = "severe-weather".equals(scenario);
        double temperature = severe ? -2 : 22;
        String conditions = severe ? "Snow and strong winds" : "Mostly sunny";
        List<String> warnings = severe
                ? List.of("Fictional severe-weather scenario: avoid exposed outdoor activities and use indoor alternatives")
                : List.of();
        String summary = "Workshop fixture: %d days in %s starting %s; %s. Not a live forecast."
                .formatted(duration, destination, startDate, conditions);
        return new WeatherForecast(destination, summary, temperature, conditions, warnings);
    }

    public record PoiCatalog(List<PointOfInterest> entries) {}

    @Tool(description = "Get seeded workshop points of interest; entries may be fictional")
    PoiCatalog getPointsOfInterest(
            @ToolArg(description = "Trip destination city or region") String destination,
            @ToolArg(description = "Trip type: family, adventure, or business") String tripType) {
        requireText(destination);
        if (tripType == null || !List.of("family", "adventure", "business").contains(tripType.toLowerCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("Unknown trip type");
        }
        if ("empty-poi".equals(scenario)) return new PoiCatalog(List.of());
        return new PoiCatalog(PointOfInterest.list("destination = ?1 and tripType = ?2", destination, tripType.toLowerCase(java.util.Locale.ROOT)));
    }

    private static void requireText(String value) {
        if (value == null || value.isBlank() || value.length() > 200) {
            throw new IllegalArgumentException("Destination must contain 1 to 200 characters");
        }
    }
}
