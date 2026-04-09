package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CarConditionFeedbackAgent;
import com.carmanagement.agentic.agents.CarImageAnalysisAgent;
import com.carmanagement.agentic.agents.FleetSupervisorAgent;
import com.carmanagement.model.CarConditions;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.FeedbackContext;
import com.carmanagement.model.FeedbackTask;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.MonitoredAgent;
import dev.langchain4j.data.message.ImageContent;
import io.quarkus.logging.Log;

import java.util.List;

/**
 * Workflow for processing car returns using a supervisor agent for complete orchestration.
 * The supervisor coordinates both feedback analysis and action agents.
 */
public interface CarProcessingWorkflow extends MonitoredAgent {

    /**
     * Processes a car return by first analyzing feedback, then using supervisor to coordinate actions.
     * CarImageAnalysisAgent analyzes the car image first.
     * FeedbackAnalysisWorkflow analyzes feedback in parallel and returns FeedbackAnalysisResults via its @Output method.
     * FleetSupervisorAgent uses these results to coordinate action agents.
     * CarConditionFeedbackAgent determines the final car assignment and condition.
     */
    // --8<-- [start:sequence-agent]
    @SequenceAgent(outputKey = "carProcessingAgentResult",
            subAgents = { CarImageAnalysisAgent.class, FeedbackAnalysisWorkflow.class, FleetSupervisorAgent.class, CarConditionFeedbackAgent.class })
    // --8<-- [end:sequence-agent]
    CarConditions processCarReturn(
            List<FeedbackTask> tasks,
            CarInfo carInfo,
            Integer carNumber,
            FeedbackContext feedbackContext,
            ImageContent carImage);

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


