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

        return generatePois(destination, tripType);
    }

    private List<PointOfInterest> generatePois(String destination, String tripType) {
        return switch (tripType.toLowerCase()) {
            case "family" -> List.of(
                    new PointOfInterest(destination + " Zoo & Aquarium", "attraction",
                            "Family-friendly zoo with interactive exhibits and aquarium", 4.5),
                    new PointOfInterest(destination + " Science Museum", "museum",
                            "Hands-on science exhibits for all ages", 4.3),
                    new PointOfInterest(destination + " Central Park", "nature",
                            "Large urban park with playgrounds and picnic areas", 4.7),
                    new PointOfInterest(destination + " Children's Theater", "entertainment",
                            "Live performances for young audiences", 4.2));
            case "adventure" -> List.of(
                    new PointOfInterest(destination + " Mountain Trails", "outdoor",
                            "Network of hiking and mountain biking trails", 4.8),
                    new PointOfInterest(destination + " River Rafting Center", "outdoor",
                            "Guided whitewater rafting expeditions", 4.6),
                    new PointOfInterest(destination + " Rock Climbing Park", "outdoor",
                            "Indoor and outdoor climbing walls for all levels", 4.4),
                    new PointOfInterest(destination + " Zip Line Adventure", "outdoor",
                            "Canopy zip line tour through forest", 4.5));
            case "business" -> List.of(
                    new PointOfInterest(destination + " Convention Center", "venue",
                            "Modern conference and exhibition facilities", 4.1),
                    new PointOfInterest(destination + " Business Lounge", "service",
                            "Premium co-working space with meeting rooms", 4.3),
                    new PointOfInterest(destination + " Fine Dining Quarter", "dining",
                            "Upscale restaurants ideal for business dinners", 4.6),
                    new PointOfInterest(destination + " Historic District", "culture",
                            "Walking tour of historic landmarks and architecture", 4.4));
            default -> List.of(
                    new PointOfInterest(destination + " Old Town", "culture",
                            "Historic city center with local shops and cafes", 4.5),
                    new PointOfInterest(destination + " Botanical Garden", "nature",
                            "Scenic gardens with native and exotic plants", 4.4));
        };
    }
}
