package com.carmanagement.service;

import com.carmanagement.agentic.agents.CarConditionFeedbackAgent;
import com.carmanagement.agentic.agents.CarWashAgent;
import com.carmanagement.agentic.agents.CarWashFeedbackAgent;
import com.carmanagement.agentic.agents.DispositionAgent;
import com.carmanagement.agentic.agents.DispositionFeedbackAgent;
import com.carmanagement.agentic.agents.MaintenanceAgent;
import com.carmanagement.agentic.agents.MaintenanceFeedbackAgent;
import com.carmanagement.agentic.config.Models;
import com.carmanagement.agentic.tools.CarWashTool;
import com.carmanagement.agentic.tools.MaintenanceTool;
import com.carmanagement.agentic.workflow.ActionWorkflow;
import com.carmanagement.agentic.workflow.CarProcessingWorkflow;
import com.carmanagement.agentic.workflow.FeedbackWorkflow;
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
        DISPOSITION,
        MAINTENANCE,
        CAR_WASH,
        NONE
    }

    @Inject
    CarService carService;

    @Inject
    Models models;

    @Inject
    CarWashTool carWashTool;

    @Inject
    MaintenanceTool maintenanceTool;

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

        // MaintenanceAgent
        MaintenanceAgent maintenanceAgent = AgenticServices
                .agentBuilder(MaintenanceAgent.class)
                .chatModel(models.baseModel())
                .tools(maintenanceTool)
                .build();

        // DispositionAgent
        DispositionAgent dispositionAgent = AgenticServices
                .a2aBuilder("http://localhost:8888", DispositionAgent.class)
                .build();

        // CarWashFeedbackAgent
        CarWashFeedbackAgent carWashFeedbackAgent = AgenticServices
                .agentBuilder(CarWashFeedbackAgent.class)
                .chatModel(models.baseModel())
                .build();

        // DispositionFeedbackAgent
        DispositionFeedbackAgent dispositionFeedbackAgent = AgenticServices
                .agentBuilder(DispositionFeedbackAgent.class)
                .chatModel(models.baseModel())
                .build();

        // MaintenanceFeedbackAgent
        MaintenanceFeedbackAgent maintenanceFeedbackAgent = AgenticServices
                .agentBuilder(MaintenanceFeedbackAgent.class)
                .chatModel(models.baseModel())
                .build();

        // CarConditionFeedbackAgent
        CarConditionFeedbackAgent carConditionFeedbackAgent = AgenticServices
                .agentBuilder(CarConditionFeedbackAgent.class)
                .chatModel(models.baseModel())
                .build();


        // FeedbackWorkflow
        FeedbackWorkflow feedbackWorkflow = AgenticServices
                .parallelBuilder(FeedbackWorkflow.class)
                .subAgents(carWashFeedbackAgent, maintenanceFeedbackAgent, dispositionFeedbackAgent)
                .build();

        // ActionWorkflow
        ActionWorkflow actionWorkflow = AgenticServices
                .conditionalBuilder(ActionWorkflow.class)
                .subAgents(
                    // Check if disposition is required
                    agenticScope -> selectAgent(agenticScope) == AgentType.DISPOSITION,
                    dispositionAgent
                )
                .subAgents(
                    // Check if maintenance is required
                    agenticScope -> selectAgent(agenticScope) == AgentType.MAINTENANCE,
                    maintenanceAgent
                )
                .subAgents(
                    // Check if car wash is required
                    agenticScope -> selectAgent(agenticScope) == AgentType.CAR_WASH,
                    carWashAgent
                )
                .build();


        // CarProcessingWorkflow
        CarProcessingWorkflow carProcessingWorkflow = AgenticServices
                .sequenceBuilder(CarProcessingWorkflow.class)
                .subAgents(feedbackWorkflow, actionWorkflow, carConditionFeedbackAgent)
                .build();

        return carProcessingWorkflow;
    }

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
        ResultWithAgenticScope<String> resultWithScope = carProcessingWorkflow.processCarReturn(
                carInfo.getMake(),
                carInfo.getModel(),
                carInfo.getYear(),
                carNumber,
                carInfo.getCondition(),
                rentalFeedback != null ? rentalFeedback : "",
                carWashFeedback != null ? carWashFeedback : "",
                maintenanceFeedback != null ? maintenanceFeedback : "");

        String result = resultWithScope.result();
        AgenticScope scope = resultWithScope.agenticScope();

        // Update the car's condition with the result from CarConditionFeedbackAgent
        String newCondition = (String) scope.readState("carCondition");
        if (newCondition != null && !newCondition.isEmpty()) {
            carInfo.setCondition(newCondition);
        }

        // Set car status to available if no actions are required
        AgentType selectedAgent = selectAgent(scope);

        if (selectedAgent == AgentType.NONE) {
            carInfo.setStatus(CarStatus.AVAILABLE);
        } else if (selectedAgent == AgentType.DISPOSITION) {
            carInfo.setStatus(CarStatus.PENDING_DISPOSITION);
        }

        return result;
    }

    /**
     * Determines which agent should be selected based on the requirements in the AgenticScope
     *
     * @param agenticScope The current AgenticScope containing request states
     * @return The appropriate AgentType to handle the car
     */
    private static AgentType selectAgent(AgenticScope agenticScope) {
        AgentType result;
        
        // Check disposition first (highest priority)
        if (isRequired(agenticScope, "dispositionRequest")) {
            result = AgentType.DISPOSITION;
        }
        // Check maintenance second (medium priority)
        else if (isRequired(agenticScope, "maintenanceRequest")) {
            result = AgentType.MAINTENANCE;
        }
        // Check car wash last (lowest priority)
        else if (isRequired(agenticScope, "carWashRequest")) {
            result = AgentType.CAR_WASH;
        }
        // No agent required
        else {
            result = AgentType.NONE;
        }
        
        System.out.println("selectAgent: " + result);
        return result;
    }

    private static boolean isRequired(AgenticScope agenticScope, String key) {
        String s = (String)agenticScope.readState(key);
        boolean required = s != null && !s.isEmpty() && !s.toUpperCase().contains("NOT_REQUIRED");
        return required;
    }
}


