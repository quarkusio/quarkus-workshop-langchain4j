package com.carmanagement.agentic.tools;

import com.carmanagement.model.CarInfo;
import com.carmanagement.model.CarStatus;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.Dependent;

/**
 * Tool for requesting car wash operations.
 */
@Dependent
public class CarWashTool {

    /**
     * Requests a car wash based on the provided parameters.
     *
     * @param carNumber The car number
     * @param carMake The car make
     * @param carModel The car model
     * @param carYear The car year
     * @param exteriorWash Whether to request exterior wash
     * @param interiorCleaning Whether to request interior cleaning
     * @param detailing Whether to request detailing
     * @param waxing Whether to request waxing
     * @param requestText The car wash request text
     * @return A summary of the car wash request
     */
    @Tool("Requests a car wash with the specified options")
    public String requestCarWash(
            Long carNumber,
            String carMake,
            String carModel,
            Integer carYear,
            boolean exteriorWash,
            boolean interiorCleaning,
            boolean detailing,
            boolean waxing,
            String requestText) {
        
        // In a real implementation, this would make an API call to a car wash service
        // or update a database with the car wash request
        
        // Update car status to AT_CAR_WASH
        CarInfo carInfo = CarInfo.findById(carNumber);
        if (carInfo != null) {
            carInfo.status = CarStatus.AT_CAR_WASH;
            carInfo.persist();
        }
        
        String result = generateCarWashSummary(carNumber, carMake, carModel, carYear,
                                              exteriorWash, interiorCleaning, detailing,
                                              waxing, requestText);
        System.out.println("\uD83D\uDE97 CarWashTool result: " + result);
        return result;
    }

    private String generateCarWashSummary(
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
        summary.append("Car wash requested for ").append(carMake).append(" ")
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


