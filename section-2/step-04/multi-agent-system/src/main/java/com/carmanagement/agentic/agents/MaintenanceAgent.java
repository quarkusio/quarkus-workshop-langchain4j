package com.carmanagement.agentic.agents;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.agentic.Agent;

/**
 * Agent that determines what maintenance services to request.
 */
public interface MaintenanceAgent {

    @SystemMessage("""
        /nothink, Reasoning: low.
        You handle intake for the car maintenance department of a car rental company.
        It is your job to submit a request to the provided requestMaintenance function to take action on the maintenance request.
        Be specific about what services are needed based on the maintenance request.
        """)
    @UserMessage("""
        Car Information:
        Make: {{carMake}}
        Model: {{carModel}}
        Year: {{carYear}}
        Car Number: {{carNumber}}
        
        Maintenance Request:
        {{maintenanceRequest}}
        """)
    @Agent("Car maintenance specialist. Using car information and request, determines what maintenance services are needed.")
    String processMaintenance(
            @V("carMake") String carMake,
            @V("carModel") String carModel,
            @V("carYear") Integer carYear,
            @V("carNumber") Integer carNumber,
            @V("maintenanceRequest") String maintenanceRequest);
}


