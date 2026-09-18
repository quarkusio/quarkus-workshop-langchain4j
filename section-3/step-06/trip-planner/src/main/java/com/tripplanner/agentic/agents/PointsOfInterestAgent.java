package com.tripplanner.agentic.agents;

import dev.langchain4j.agentic.declarative.McpClientAgent;
import dev.langchain4j.agentic.declarative.McpClientSupplier;
import dev.langchain4j.mcp.client.McpClient;
import io.quarkiverse.langchain4j.agentic.runtime.CdiBean;
import io.quarkiverse.langchain4j.mcp.runtime.McpClientName;

public interface PointsOfInterestAgent {

    @McpClientAgent(toolName = "getPointsOfInterest", outputKey = "pointsOfInterest",
            description = "Fetches points of interest from the Trip Intelligence MCP server")
    String fetchPointsOfInterest(String destination, String tripType);

    @McpClientSupplier
    static McpClient mcpClient(@CdiBean @McpClientName("tripIntelligence") McpClient client) {
        return client;
    }
}
