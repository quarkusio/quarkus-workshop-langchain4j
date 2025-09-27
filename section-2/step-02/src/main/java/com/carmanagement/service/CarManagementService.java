package com.carmanagement.service;

import com.carmanagement.agentic.agents.CarConditionFeedbackAgent;
import com.carmanagement.agentic.agents.CarWashAgent;
import com.carmanagement.agentic.config.Models;
import com.carmanagement.agentic.tools.CarWashTool;
import com.carmanagement.agentic.workflow.CarProcessingWorkflow;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.CarStatus;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;

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
    Models models;

    @Inject
    CarWashTool carWashTool;

    private CarProcessingWorkflow carProcessingWorkflow;

    @PostConstruct
    void initialize() {
        carProcessingWorkflow = createCarProcessingWorkflow();
    }

    private CarProcessingWorkflow createCarProcessingWorkflow() {
        // CarWashAgent
        CarWashAgent carWashAgent = AgenticServices
                .agentBuilder(CarWashAgent.class)
                .chatModel(models.baseModel())
                .tools(carWashTool)
                .build();

        // CarConditionFeedbackAgent
        CarConditionFeedbackAgent carConditionFeedbackAgent = AgenticServices
                .agentBuilder(CarConditionFeedbackAgent.class)
                .chatModel(models.baseModel())
                .build();

        // CarProcessingWorkflow - simple sequence of CarWashAgent and CarConditionFeedbackAgent
        CarProcessingWorkflow carProcessingWorkflow = AgenticServices
                .sequenceBuilder(CarProcessingWorkflow.class)
                .subAgents(carWashAgent, carConditionFeedbackAgent)
                .build();

        return carProcessingWorkflow;
    }

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
        ResultWithAgenticScope<String> resultWithScope = carProcessingWorkflow.processCarReturn(
                carInfo.getMake(),
                carInfo.getModel(),
                carInfo.getYear(),
                carNumber,
                carInfo.getCondition(),
                rentalFeedback != null ? rentalFeedback : "",
                carWashFeedback != null ? carWashFeedback : "");

        String result = resultWithScope.result();
        AgenticScope scope = resultWithScope.agenticScope();

        // Update the car's condition with the result from CarConditionFeedbackAgent
        String newCondition = (String) scope.readState("carCondition");
        if (newCondition != null && !newCondition.isEmpty()) {
            carInfo.setCondition(newCondition);
        }

        // If carwash was not required, make the car available to rent
        if (!isRequired(scope, "carWashAgentResult")) {
            carInfo.setStatus(CarStatus.AVAILABLE);
        }

        return result;
    }

    private static boolean isRequired(AgenticScope agenticScope, String key) {
        String s = (String)agenticScope.readState(key);
        boolean required = s != null && !s.isEmpty() && !s.toUpperCase().contains("NOT_REQUIRED");
        return required;
    }

}
