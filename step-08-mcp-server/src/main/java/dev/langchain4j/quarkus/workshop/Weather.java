package dev.langchain4j.quarkus.workshop;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;

public class Weather {

    @RestClient
    WeatherClient weatherClient;

    @Tool(description = "Get weather forecast for a location.")
    String getForecast(@ToolArg(description = "Latitude of the location") double latitude,
                       @ToolArg(description = "Longitude of the location") double longitude) {
        return weatherClient.getForecast(
                latitude,
                longitude,
                16,
                "temperature_2m,snowfall,rain,precipitation,precipitation_probability");
    }
}