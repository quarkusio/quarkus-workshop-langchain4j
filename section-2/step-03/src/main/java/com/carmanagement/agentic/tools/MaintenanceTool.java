package com.carmanagement.agentic.tools;

import dev.langchain4j.agent.tool.Tool;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Tool for requesting car maintenance operations.
 */
@ApplicationScoped
public class MaintenanceTool {

    /**
     * Requests maintenance for a car based on the provided parameters.
     *
     * @param carNumber The car number
     * @param carMake The car make
     * @param carModel The car model
     * @param carYear The car year
     * @param oilChange Whether to request an oil change
     * @param tireRotation Whether to request tire rotation
     * @param brakeService Whether to request brake service
     * @param engineService Whether to request engine service
     * @param transmissionService Whether to request transmission service
     * @param requestText The maintenance request text
     * @return A summary of the maintenance request
     */
    @Tool("Requests maintenance with the specified options")
    public String requestMaintenance(
            Long carNumber,
            String carMake,
            String carModel,
            Integer carYear,
            boolean oilChange,
            boolean tireRotation,
            boolean brakeService,
            boolean engineService,
            boolean transmissionService,
            String requestText) {
        
        // In a more elaborate implementation, we might make an API call to a maintenance service here
        
        StringBuilder summary = new StringBuilder();
        summary.append("Maintenance requested for ").append(carMake).append(" ")
               .append(carModel).append(" (").append(carYear).append("), Car #")
               .append(carNumber).append(":\n");
        
        if (oilChange) {
            summary.append("- Oil change\n");
        }
        
        if (tireRotation) {
            summary.append("- Tire rotation\n");
        }
        
        if (brakeService) {
            summary.append("- Brake service\n");
        }
        
        if (engineService) {
            summary.append("- Engine service\n");
        }
        
        if (transmissionService) {
            summary.append("- Transmission service\n");
        }
        
        if (requestText != null && !requestText.isEmpty()) {
            summary.append("Additional notes: ").append(requestText);
        }
        
        Log.info("  └─ MaintenanceAgent activated");
        String result = summary.toString();
        Log.debug("🚗 MaintenanceTool result: " + result);
        return result;
    }
}


