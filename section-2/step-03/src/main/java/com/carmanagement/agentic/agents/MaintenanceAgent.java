package com.carmanagement.agentic.agents;

import com.carmanagement.agentic.tools.MaintenanceTool;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.agentic.Agent;
import io.quarkiverse.langchain4j.ToolBox;

/**
 * Agent that determines what maintenance services to request.
 */
public interface MaintenanceAgent {

    @SystemMessage("""
        You handle intake for the car maintenance department of a car rental company.
        It is your job to submit a request to the provided requestMaintenance function to take action on the maintenance request.
        Be specific about what services are needed based on the maintenance request.
        """)
    @UserMessage("""
        Car Information:
        Make: {carMake}
        Model: {carModel}
        Year: {carYear}
        Car Number: {carNumber}
        
        Maintenance Request:
        {maintenanceRequest}
        """)
    @Agent(description = "Car maintenance specialist. Using car information and request, determines what maintenance services are needed.",
            outputKey = "actionResult")
    @ToolBox(MaintenanceTool.class)
    String processMaintenance(
            String carMake,
            String carModel,
            Integer carYear,
            Long carNumber,
            String maintenanceRequest);
}


