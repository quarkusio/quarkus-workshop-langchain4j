// --8<-- [start:part1]
package com.carmanagement.service;

import com.carmanagement.agentic.tools.CarWashTool;
import com.carmanagement.agentic.workflow.CarProcessingWorkflow;
import com.carmanagement.model.CarConditions;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.CarStatus;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for managing car returns from various operations.
 */
@ApplicationScoped
public class CarManagementService {

    /**
     * Enum representing the type of agent to be selected for car processing
     */
    public enum AgentType {
        CAR_WASH,
        NONE
    }

    @Inject
    CarService carService;

    @Inject
    CarProcessingWorkflow carProcessingWorkflow;

    /**
     * Process a car return from any operation.
     *
     * @param carNumber The car number
     * @param rentalFeedback Optional rental feedback
     * @return Result of the processing
     */
    public String processCarReturn(Integer carNumber, String rentalFeedback, String carWashFeedback) {
        CarInfo carInfo = carService.getCarById(carNumber);
        if (carInfo == null) {
            return "Car not found with number: " + carNumber;
        }

        // Process the car return using the workflow and get the AgenticScope
        CarConditions carConditions = carProcessingWorkflow.processCarReturn(
                carInfo.getMake(),
                carInfo.getModel(),
                carInfo.getYear(),
                carNumber,
                carInfo.getCondition(),
                rentalFeedback != null ? rentalFeedback : "",
                carWashFeedback != null ? carWashFeedback : "");

        // Update the car's condition with the result from CarConditionFeedbackAgent
        carInfo.setCondition(carConditions.generalCondition());

        // If carwash was not required, make the car available to rent
        if (!carConditions.carWashRequired()) {
            carInfo.setStatus(CarStatus.AVAILABLE);
        }

        return carConditions.generalCondition();
    }
}
