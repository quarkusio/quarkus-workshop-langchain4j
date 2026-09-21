package com.tripplanner.mcp;

import com.tripplanner.mcp.model.PointOfInterest;
import com.tripplanner.mcp.model.WeatherForecast;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TripIntelligenceToolsTest {

    @Inject
    TripIntelligenceTools tools;

    @Test
    void weatherForecastReturnsValidData() {
        WeatherForecast result = tools.getWeatherForecast("Barcelona", "2026-07-15", 5);

        assertNotNull(result);
        assertEquals("Barcelona", result.destination());
        assertNotNull(result.summary());
        assertTrue(result.summary().contains("Barcelona"));
        assertNotNull(result.conditions());
        assertTrue(result.avgTemperatureCelsius() > 0);
    }

    @Test
    void weatherForecastIsDeterministic() {
        WeatherForecast first = tools.getWeatherForecast("Rome", "2026-08-01", 3);
        WeatherForecast second = tools.getWeatherForecast("Rome", "2026-08-01", 3);

        assertEquals(first, second);
    }

    @Test
    void pointsOfInterestReturnsMatchingTripType() {
        List<PointOfInterest> family = tools.getPointsOfInterest("Paris", "family");
        List<PointOfInterest> adventure = tools.getPointsOfInterest("Paris", "adventure");

        assertFalse(family.isEmpty());
        assertFalse(adventure.isEmpty());
        assertTrue(family.stream().anyMatch(p ->
                p.name().contains("Zoo") || p.name().contains("Museum") || p.name().contains("Park")));
        assertTrue(adventure.stream().anyMatch(p ->
                p.name().contains("Trail") || p.name().contains("Rafting") || p.name().contains("Climbing")));
    }

    @Test
    void pointsOfInterestReturnsAllEntries() {
        List<PointOfInterest> result = tools.getPointsOfInterest("Berlin", "family");

        assertEquals(4, result.size());
    }
}
