package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.A2AClientAgent;

/**
 * Agent that determines how to dispose of a car.
 */
public interface DispositionAgent {

    @Agent(description = "Car disposition specialist. Determines how to dispose of a car based on value and condition.",
           outputKey = "dispositionAction")
    @A2AClientAgent(a2aServerUrl = "http://localhost:8888", outputKey = "dispositionAction ")
    String processDisposition(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String carCondition,
            String carValue,
            String rentalFeedback);
}

