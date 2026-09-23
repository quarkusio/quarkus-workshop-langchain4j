package com.tripplanner.agentic.workflow;

import com.tripplanner.agentic.agents.PointsOfInterestAgent;
import com.tripplanner.agentic.agents.WeatherAgent;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.ParallelAgent;

public interface DestinationIntelligence {

    @ParallelAgent(
            description = "Fetches destination intelligence (weather, POI) from MCP server in parallel",
            outputKey = "intelligenceComplete",
            subAgents = { WeatherAgent.class, PointsOfInterestAgent.class })
    String fetchIntelligence(String destination, String startDate, String days, String tripType);

    @Output
    static String output(String destination, String weather, String pointsOfInterest) {
        DestinationEvidence.validate(destination, weather, pointsOfInterest);
        return "Weather: " + weather + "\nPoints of interest: " + pointsOfInterest;
    }
}
