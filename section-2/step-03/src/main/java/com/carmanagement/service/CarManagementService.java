package com.carmanagement.service;

import com.carmanagement.agentic.agents.CarConditionFeedbackAgent;
import com.carmanagement.agentic.agents.CarWashAgent;
import com.carmanagement.agentic.agents.CarWashFeedbackAgent;
import com.carmanagement.agentic.agents.MaintenanceAgent;
import com.carmanagement.agentic.agents.MaintenanceFeedbackAgent;
import com.carmanagement.agentic.config.Models;
import com.carmanagement.agentic.tools.CarWashTool;
import com.carmanagement.agentic.tools.MaintenanceTool;
import com.carmanagement.agentic.workflow.ActionWorkflow;
import com.carmanagement.agentic.workflow.CarProcessingWorkflow;
import com.carmanagement.agentic.workflow.FeedbackWorkflow;
import com.carmanagement.model.CarConditions;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.CarStatus;
import com.carmanagement.model.RequiredAction;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for managing car returns from various operations.
 */
@ApplicationScoped
public class CarManagementService {

    @Inject
    CarService carService;

    @Inject
    CarProcessingWorkflow carProcessingWorkflow;

    /**
     * Process a car return from any operation.
     *
     * @param carNumber The car number
     * @param rentalFeedback Optional rental feedback
     * @param carWashFeedback Optional car wash feedback
     * @param maintenanceFeedback Optional maintenance feedback
     * @return Result of the processing
     */
    public String processCarReturn(Integer carNumber, String rentalFeedback, String carWashFeedback, String maintenanceFeedback) {
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
                carWashFeedback != null ? carWashFeedback : "",
                maintenanceFeedback != null ? maintenanceFeedback : "");

        // Update the car's condition with the result from CarConditionFeedbackAgent
        carInfo.setCondition(carConditions.generalCondition());

        if (carConditions.requiredAction() == RequiredAction.NONE) {
            carInfo.setStatus(CarStatus.AVAILABLE);
        }

        return carConditions.generalCondition();
    }
}


