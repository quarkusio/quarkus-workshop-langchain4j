package com.carmanagement.agentic.agents;

import dev.langchain4j.service.V;
import dev.langchain4j.agentic.Agent;

/**
 * Agent that determines how to dispose of a car.
 */
public interface DispositionAgent {

    @Agent("Car disposition specialist. Determines how to dispose of a car.")
    String processDisposition(
            @V("carMake") String carMake,
            @V("carModel") String carModel,
            @V("carYear") Integer carYear,
            @V("carNumber") Integer carNumber,
            @V("carCondition") String carCondition,
            @V("dispositionRequest") String dispositionRequest);
}

