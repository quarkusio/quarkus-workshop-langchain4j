package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.declarative.A2AClientAgent;

/**
 * Agent that determines how to dispose of a car.
 *
 * Note: Uses Integer for carNumber (instead of Long used in step-04) because this is an
 * A2A client agent that calls a remote A2A server. The remote server requires Integer due
 * to explicit string-to-type parsing in the A2A protocol (see DispositionAgentExecutor).
 */
public interface DispositionAgent {
    @A2AClientAgent(a2aServerUrl = "http://localhost:8888", 
        outputKey = "dispositionAction", 
        description = "Car disposition specialist. Determines how to dispose of a car based on value and condition.")
    String processDisposition(
            String carMake,
            String carModel,
            Integer carYear,
            Integer carNumber,
            String carCondition,
            String carValue,
            String rentalFeedback);
}

