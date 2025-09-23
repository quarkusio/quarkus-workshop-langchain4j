package com.carmanagement.service;

import com.carmanagement.agentic.agents.CarWashAgent;
import com.carmanagement.agentic.config.Models;
import com.carmanagement.agentic.tools.CarWashTool;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.CarStatus;
import dev.langchain4j.agentic.AgenticServices;
import jakarta.annotation.PostConstruct;
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
    Models models = null;

    @Inject
    CarWashTool carWashTool;

    private CarWashAgent carWashAgent;

    // --8<-- [start:createCarWashAgent]
    @PostConstruct
    void initialize() {
        carWashAgent = createCarWashAgent();
    }

    private CarWashAgent createCarWashAgent() {
        // CarWashAgent
        CarWashAgent carWashAgent = AgenticServices
                .agentBuilder(CarWashAgent.class)
                .chatModel(models.baseModel())
                .tools(carWashTool)
                .outputName("carWashAgentResult")
                .build();

        return carWashAgent;
    }
// --8<-- [end:createCarWashAgent]

    // --8<-- [start:processCarReturn]
    /**
     * Process a car return from any operation.
     *
     * @param carNumber The car number
     * @param rentalFeedback Optional rental feedback
     * @param rentalFeedback Optional car wash feedback
     * @return Result of the processing
     */
    public String processCarReturn(Integer carNumber, String rentalFeedback, String carWashFeedback) {
        CarInfo carInfo = carService.getCarById(carNumber);
        if (carInfo == null) {
            return "Car not found with number: " + carNumber;
        }

        // Process the car result
        String result = carWashAgent.processCarWash(
                carInfo.getMake(),
                carInfo.getModel(),
                carInfo.getYear(),
                carNumber,
                rentalFeedback != null ? rentalFeedback : "",
                carWashFeedback != null ? carWashFeedback : "");

        if (result.toUpperCase().contains("CARWASH_NOT_REQUIRED")) {
            carInfo.setStatus(CarStatus.AVAILABLE);
        }

        return result;
    }
    // --8<-- [end:processCarReturn]
}

