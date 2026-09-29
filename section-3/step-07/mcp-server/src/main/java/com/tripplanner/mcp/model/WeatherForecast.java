package com.tripplanner.mcp.model;

import java.util.List;

public record WeatherForecast(
        String destination,
        String summary,
        double avgTemperatureCelsius,
        String conditions,
        List<String> warnings
) {}
