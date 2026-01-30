package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CarConditionFeedbackAgent;
import com.carmanagement.agentic.agents.FleetSupervisorAgent;
import com.carmanagement.model.CarConditions;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import io.quarkus.logging.Log;

/**
 * Workflow for processing car returns using a supervisor agent for complete orchestration.
 * The supervisor coordinates both feedback analysis and action agents.
 */
public interface CarProcessingWorkflow {

    /**
     * Processes a car return by first analyzing feedback, then using supervisor to coordinate actions.
     * FeedbackWorkflow produces cleaningRequest, maintenanceRequest, and dispositionRequest.
     * FleetSupervisorAgent then uses these to coordinate action agents.
     */
    // --8<-- [start:sequence-agent]
    @SequenceAgent(outputKey = "carProcessingAgentResult",
            subAgents = { FeedbackWorkflow.class, FleetSupervisorAgent.class, CarConditionFeedbackAgent.class })
    // --8<-- [end:sequence-agent]
    CarConditions processCarReturn(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String carCondition,
            String rentalFeedback,
            String cleaningFeedback,
            String maintenanceFeedback);

    @Output
    static CarConditions output(CarConditions carConditions) {
        // CarConditionFeedbackAgent now handles all the logic for determining
        // the final car assignment, disposition status, and condition description.
        // We simply pass through its result.
  
        Log.debug("DEBUG CarConditions output method:");
        Log.debug("  generalCondition: " + carConditions.generalCondition());
        Log.debug("  carAssignment: " + carConditions.carAssignment());
        Log.debug("  dispositionStatus: " + carConditions.dispositionStatus());
        Log.debug("  dispositionReason: " + carConditions.dispositionReason());
        
        return carConditions;
    }
}


