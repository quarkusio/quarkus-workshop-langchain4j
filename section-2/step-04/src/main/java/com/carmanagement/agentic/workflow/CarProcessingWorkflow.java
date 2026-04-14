package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CarConditionFeedbackAgent;
import com.carmanagement.agentic.agents.FleetSupervisorAgent;
import com.carmanagement.model.CarConditions;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.FeedbackContext;
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
     * Processes a car return by first analyzing feedback, then using supervisor to coordinate actions.
     * FeedbackAnalysisWorkflow analyzes feedback in parallel and returns FeedbackAnalysisResults via its @Output method.
     * FleetSupervisorAgent uses these results to coordinate action agents.
     * CarConditionFeedbackAgent determines the final car assignment and condition.
     */
    // --8<-- [start:sequence-agent]
    @SequenceAgent(outputKey = "carProcessingAgentResult",
            subAgents = { FeedbackAnalysisWorkflow.class, FleetSupervisorAgent.class, CarConditionFeedbackAgent.class })
    // --8<-- [end:sequence-agent]
    CarConditions processCarReturn(
            List<FeedbackTask> tasks,
            CarInfo carInfo,
            Integer carNumber,
            FeedbackContext feedback);

    @Output
    static CarConditions output(CarConditions carConditions) {
        // CarConditionFeedbackAgent handles all logic for determining
        // the final car assignment and condition description.
        Log.debug("CarConditions: " + carConditions.generalCondition() + " → " + carConditions.carAssignment());
        return carConditions;
    }
}


