package com.tripplanner.mcp;

import com.tripplanner.mcp.model.PointOfInterest;
import com.tripplanner.mcp.model.WeatherForecast;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;

import java.util.ArrayList;
import java.util.List;

public class TripIntelligenceTools {

    @Tool(description = "Get weather forecast for a trip destination and date range")
    WeatherForecast getWeatherForecast(
            @ToolArg(description = "Trip destination city or region") String destination,
            @ToolArg(description = "Trip start date in yyyy-MM-dd format") String startDate,
            @ToolArg(description = "Trip duration in days") int days) {

        int hash = Math.abs(destination.toLowerCase().hashCode());
        double baseTemp = 10 + (hash % 25);
        boolean rainy = hash % 3 == 0;
        boolean windy = hash % 5 == 0;

        String conditions;
        if (rainy && windy) {
            conditions = "Rainy and windy";
        } else if (rainy) {
            conditions = "Occasional rain showers";
        } else if (windy) {
            conditions = "Clear skies with strong winds";
        } else {
            conditions = "Mostly sunny";
        }

        List<String> warnings = new ArrayList<>();
        if (baseTemp > 30) {
            warnings.add("Heat advisory: temperatures above 30°C expected");
        }
        if (rainy && windy) {
            warnings.add("Storm warning: consider indoor alternatives for outdoor activities");
        }
        if (baseTemp < 5) {
            warnings.add("Cold weather alert: pack warm clothing and check road conditions");
        }

        String summary = "%d-day forecast for %s starting %s: %.0f°C average, %s".formatted(
                days, destination, startDate, baseTemp, conditions.toLowerCase());

        return new WeatherForecast(destination, summary, baseTemp, conditions, warnings);
    }

    @Tool(description = "Get points of interest for a destination matching a trip type")
    List<PointOfInterest> getPointsOfInterest(
            @ToolArg(description = "Trip destination city or region") String destination,
            @ToolArg(description = "Trip type: family, adventure, or business") String tripType) {

        return PointOfInterest.list("destination = ?1 and tripType = ?2", destination, tripType.toLowerCase());
    }
}
