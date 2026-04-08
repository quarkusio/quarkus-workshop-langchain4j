package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CarConditionFeedbackAgent;
import com.carmanagement.agentic.agents.FeedbackExtractorAgent;
import com.carmanagement.agentic.agents.FleetSupervisorAgent;
import com.carmanagement.model.CarConditions;
import com.carmanagement.model.FeedbackTask;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import io.quarkus.logging.Log;

import java.util.List;

/**
 * Workflow for processing car returns using a supervisor agent for complete orchestration.
 * The supervisor coordinates both feedback analysis and action agents.
 */
public interface CarProcessingWorkflow {

    /**
     * Processes a car return by first analyzing feedback, extracting individual results,
     * then using supervisor to coordinate actions.
     * FeedbackWorkflow produces a list of feedback results.
     * FeedbackExtractorAgent extracts individual cleaningRequest, maintenanceRequest, and dispositionRequest.
     * FleetSupervisorAgent then uses these to coordinate action agents.
     * CarConditionFeedbackAgent determines the final car assignment and condition.
     */
    // --8<-- [start:sequence-agent]
    @SequenceAgent(outputKey = "carProcessingAgentResult",
            subAgents = { FeedbackWorkflow.class, FeedbackExtractorAgent.class, FleetSupervisorAgent.class, CarConditionFeedbackAgent.class })
    // --8<-- [end:sequence-agent]
    CarConditions processCarReturn(
            List<FeedbackTask> tasks,
            String carMake,
            String carModel,
            Integer carYear,
            Integer carNumber,
            String carCondition,
            String rentalFeedback,
            String cleaningFeedback,
            String maintenanceFeedback);

    @Output
    static CarConditions output(CarConditions carConditions) {
        // CarConditionFeedbackAgent handles all logic for determining
        // the final car assignment and condition description.
        Log.debug("CarConditions: " + carConditions.generalCondition() + " → " + carConditions.carAssignment());
        return carConditions;
    }
}


