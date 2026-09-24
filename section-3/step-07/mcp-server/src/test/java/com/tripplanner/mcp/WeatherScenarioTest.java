package com.tripplanner.mcp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeatherScenarioTest {
    @Test
    void severeWeatherIncludesColdAndWarnings() {
        var tools = new TripIntelligenceTools();
        tools.scenario = "severe-weather";
        var result = tools.getWeatherForecast("Rome", "2027-07-10", "3");
        assertTrue(result.avgTemperatureCelsius() < 0);
        assertFalse(result.warnings().isEmpty());
    }

    @Test
    void malformedFixtureAndInvalidInputsAreRepeatable() {
        var tools = new TripIntelligenceTools();
        tools.scenario = "malformed-response";
        assertTrue(tools.getWeatherForecast("Rome", "2027-07-10", "3").summary().isBlank());
        assertThrows(RuntimeException.class, () -> tools.getWeatherForecast("Rome", "not-a-date", "3"));
        assertThrows(RuntimeException.class, () -> tools.getWeatherForecast("Rome", "2027-07-10", "31"));
        assertThrows(RuntimeException.class, () -> tools.getWeatherForecast("Rome", "2027-07-10", "abc"));
        tools.scenario = "empty-poi";
        assertTrue(tools.getPointsOfInterest("Rome", "family").entries().isEmpty());
    }
}
