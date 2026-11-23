package com.carmanagement.agentic.agents;

import com.carmanagement.model.CarAssignment;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.decision.Decide;

/**
 * Agent that decides which team the car must be assigned to.
 * It asks a decision model a single question and gets a typed answer instead of generated text.
 */
public interface CarAssignmentDecisionAgent {

    @Decide("""
        Which team should handle this car next: MAINTENANCE if any maintenance is required, \
        otherwise CLEANING if any cleaning is required, otherwise NONE?""")
    @Agent(description = "Car assignment decider. Decides if the car goes to maintenance, to cleaning, or needs no action.",
            outputKey = "carAssignment")
    CarAssignment decideAssignment(
            String maintenanceRequest,
            String cleaningRequest);
}
