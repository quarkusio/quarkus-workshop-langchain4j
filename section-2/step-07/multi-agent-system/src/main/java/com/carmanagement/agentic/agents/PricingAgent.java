package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.declarative.A2AClientAgent;

/**
 * Agent that estimates the market value of a vehicle.
 * Delegates to the remote A2A pricing service.
 */
public interface PricingAgent {

    @A2AClientAgent(a2aServerUrl = "http://localhost:8888",
        outputKey = "carValue",
        description = "Pricing specialist that estimates vehicle market value based on make, model, year, and condition")
    String estimateValue(String carMake, String carModel, Integer carYear, String carCondition);
}
