package com.carmanagement.agentic.workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * Workflow for processing car actions conditionally.
 */
public interface ActionWorkflow {

    /**
     * Runs the appropriate action agent based on the feedback analysis.
     */
    // --8<-- [start:actionWorkflow]
    @Agent(outputName="actionResult")
    String processAction(
            @V("carMake") String carMake,
            @V("carModel") String carModel,
            @V("carYear") Integer carYear,
            @V("carNumber") Integer carNumber,
            @V("carCondition") String carCondition,
            @V("carWashRequest") String carWashRequest,
            @V("maintenanceRequest") String maintenanceRequest,
            @V("dispositionRequest") String dispositionRequest);
    // --8<-- [end:actionWorkflow]

}


