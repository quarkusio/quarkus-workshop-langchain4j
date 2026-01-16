package com.carmanagement.agentic.tools;

import jakarta.enterprise.context.Dependent;

import com.carmanagement.model.CarInfo;
import com.carmanagement.model.CarStatus;
import dev.langchain4j.agent.tool.Tool;

/**
 * Tool for requesting cleaning operations.
 */
@Dependent
public class CleaningTool {

    /**
     * Requests a cleaning based on the provided parameters.
     *
     * @param carNumber The car number
     * @param carMake The car make
     * @param carModel The car model
     * @param carYear The car year
     * @param exteriorWash Whether to request exterior wash
     * @param interiorCleaning Whether to request interior cleaning
     * @param detailing Whether to request detailing
     * @param waxing Whether to request waxing
     * @param requestText The cleaning request text
     * @return A summary of the cleaning request
     */
    @Tool("Requests a cleaning with the specified options")
    public String requestCleaning(
            Long carNumber,
            String carMake,
            String carModel,
            Integer carYear,
            boolean exteriorWash,
            boolean interiorCleaning,
            boolean detailing,
            boolean waxing,
            String requestText) {
        
        // In a real implementation, this would make an API call to a cleaning service
        // or update a database with the cleaning request
        
        // Update car status to AT_CLEANING
        CarInfo carInfo = CarInfo.findById(carNumber);
        if (carInfo != null) {
            carInfo.status = CarStatus.AT_CLEANING;
            carInfo.persist();
        }
        
        String result = generateCleaningSummary(carNumber, carMake, carModel, carYear,
                                              exteriorWash, interiorCleaning, detailing,
                                              waxing, requestText);
        System.out.println("\uD83D\uDE97 CleaningTool result: " + result);
        return result;
    }

    private String generateCleaningSummary(
            Long carNumber,
            String carMake,
            String carModel,
            Integer carYear,
            boolean exteriorWash,
            boolean interiorCleaning,
            boolean detailing,
            boolean waxing,
            String requestText) {

        StringBuilder summary = new StringBuilder();
        summary.append("Cleaning requested for ").append(carMake).append(" ")
               .append(carModel).append(" (").append(carYear).append("), Car #")
               .append(carNumber).append(":\n");
        
        if (exteriorWash) {
            summary.append("- Exterior wash\n");
        }
        
        if (interiorCleaning) {
            summary.append("- Interior cleaning\n");
        }
        
        if (detailing) {
            summary.append("- Detailing\n");
        }
        
        if (waxing) {
            summary.append("- Waxing\n");
        }
        
        if (requestText != null && !requestText.isEmpty()) {
            summary.append("Additional notes: ").append(requestText);
        }
        
        return summary.toString();
    }
}


