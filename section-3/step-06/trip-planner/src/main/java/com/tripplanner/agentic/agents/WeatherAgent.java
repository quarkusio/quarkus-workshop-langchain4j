package com.tripplanner.agentic.agents;

import dev.langchain4j.agentic.declarative.McpClientAgent;
import dev.langchain4j.agentic.declarative.McpClientSupplier;
import dev.langchain4j.mcp.client.McpClient;
import io.quarkiverse.langchain4j.agentic.runtime.CdiBean;
import io.quarkiverse.langchain4j.mcp.runtime.McpClientName;

public interface WeatherAgent {

    @McpClientAgent(toolName = "getWeatherForecast", outputKey = "weather",
            description = "Fetches weather forecast from the Trip Intelligence MCP server")
    String fetchWeather(String destination, String startDate, String days);

    @McpClientSupplier
    static McpClient mcpClient(@CdiBean @McpClientName("tripIntelligence") McpClient client) {
        return client;
    }
}
