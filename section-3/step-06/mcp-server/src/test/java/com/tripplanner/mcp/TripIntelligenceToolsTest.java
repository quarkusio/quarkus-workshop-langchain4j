package com.tripplanner.mcp;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TripIntelligenceToolsTest {

    @Inject
    TripIntelligenceTools tools;

    @Test
    void weatherForecastReturnsValidJson() {
        String result = tools.getWeatherForecast("Barcelona", "2026-07-15", 5);

        assertNotNull(result);
        assertTrue(result.contains("Barcelona"));
        assertTrue(result.contains("destination"));
        assertTrue(result.contains("summary"));
        assertTrue(result.contains("avgTemperatureCelsius"));
        assertTrue(result.contains("conditions"));
    }

    @Test
    void weatherForecastIsDeterministic() {
        String first = tools.getWeatherForecast("Rome", "2026-08-01", 3);
        String second = tools.getWeatherForecast("Rome", "2026-08-01", 3);

        assertEquals(first, second);
    }

    @Test
    void pointsOfInterestReturnsMatchingTripType() {
        String family = tools.getPointsOfInterest("Paris", "family", 3);
        String adventure = tools.getPointsOfInterest("Paris", "adventure", 3);

        assertTrue(family.contains("Zoo") || family.contains("Museum") || family.contains("Park"));
        assertTrue(adventure.contains("Trail") || adventure.contains("Rafting") || adventure.contains("Climbing"));
    }

    @Test
    void pointsOfInterestRespectsLimit() {
        String one = tools.getPointsOfInterest("Berlin", "family", 1);
        String four = tools.getPointsOfInterest("Berlin", "family", 4);

        long oneCount = one.chars().filter(c -> c == '{').count();
        long fourCount = four.chars().filter(c -> c == '{').count();
        assertEquals(1, oneCount);
        assertEquals(4, fourCount);
    }
}
