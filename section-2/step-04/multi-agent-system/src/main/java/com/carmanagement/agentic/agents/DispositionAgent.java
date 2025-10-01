package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.declarative.A2AClientAgent;

/**
 * Agent that determines how to dispose of a car.
 */
public interface DispositionAgent {

    @A2AClientAgent(a2aServerUrl = "http://localhost:8888",
            outputName="dispositionAgentResult",
            description="Car disposition specialist. Determines how to dispose of a car."
    )
    String processDisposition(
            String carMake,
            String carModel,
            Integer carYear,
            Integer carNumber,
            String carCondition,
            String dispositionRequest);
}

