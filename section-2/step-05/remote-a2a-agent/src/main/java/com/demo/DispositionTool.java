package com.demo;

import dev.langchain4j.agent.tool.Tool;
import jakarta.inject.Singleton;

/**
 * Tool for requesting car disposition operations.
 * This tool is used by the LLM to determine the appropriate disposition for a car.
 */
@Singleton
public class DispositionTool {


    /**
     * Enum representing the possible disposition options for a car.
     */
    public enum DispositionOption {
        SCRAP("Scrap the car"),
        SELL("Sell the car"),
        DONATE("Donate the car");
        
        private final String description;
        
        DispositionOption(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }

    /**
     * Requests disposition for a car based on the provided parameters.
     *
     * @param carNumber The car number
     * @param carMake The car make
     * @param carModel The car model
     * @param carYear The car year
     * @param dispositionOption The disposition option (SCRAP, SELL, or DONATE)
     * @param carCondition The condition of the car
     * @return A summary of the disposition request
     */
    @Tool(name = "DispositionTool")
    public String requestDisposition(
            Long carNumber,
            String carMake,
            String carModel,
            Integer carYear,
            DispositionOption dispositionOption,
            String carCondition) {

        // In a real implementation, this would make an API call to a disposition service
        // or update a database with the disposition request

        String result = "Car disposition requested for " + carMake + " " +
                carModel + " (" + carYear + "), Car #" +
                carNumber + ": " +
                dispositionOption.getDescription() +
                "\n";
        System.out.println("⛍ DispositionTool result: " + result);
        return result;
    }
}