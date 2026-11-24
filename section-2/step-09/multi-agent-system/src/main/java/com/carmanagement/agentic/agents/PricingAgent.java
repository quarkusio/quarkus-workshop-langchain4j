package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.declarative.A2AClientAgent;
import dev.langchain4j.agentic.declarative.A2AServerUrlSupplier;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Agent that estimates the market value of a vehicle.
 * Delegates to the remote A2A pricing service.
 */
public interface PricingAgent {

    @A2AClientAgent(outputKey = "carValue",
        description = "Pricing specialist that estimates vehicle market value based on make, model, year, and condition")
    String estimateValue(String carMake, String carModel, Integer carYear, String carCondition);

    @A2AServerUrlSupplier
    static String a2aServerUrl() {
        return ConfigProvider.getConfig().getValue("car-management.pricing-agent.url", String.class);
    }
}
