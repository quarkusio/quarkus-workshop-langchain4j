package com.carmanagement.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.carmanagement.agentic.workflow.CarProcessingWorkflow;
import com.carmanagement.model.CarConditions;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.CarStatus;
import io.quarkus.logging.Log;

/**
 * Service for managing car returns from various operations.
 */
@ApplicationScoped
public class CarManagementService {

    @Inject
    CarProcessingWorkflow carProcessingWorkflow;

    /**
     * Process a car return from any operation.
     *
     * @param carNumber The car number
     * @param rentalFeedback Optional rental feedback
     * @param cleaningFeedback Optional cleaning feedback
     * @param maintenanceFeedback Optional maintenance feedback
     * @return Result of the processing
     */
    @Transactional
    public String processCarReturn(Long carNumber, String rentalFeedback, String cleaningFeedback,
                                   String maintenanceFeedback) {
        CarInfo carInfo = CarInfo.findById(carNumber);
        if (carInfo == null) {
            return "Car not found with number: " + carNumber;
        }
        
        Log.info("FeedbackWorkflow executing...");
        Log.info("  ├─ CleaningFeedbackAgent analyzing...");
        Log.info("  └─ MaintenanceFeedbackAgent analyzing...");
        Log.info("FleetSupervisorAgent orchestrating car processing...");
        
        // Process the car return using the workflow with supervisor
        CarConditions carConditions = carProcessingWorkflow.processCarReturn(
                carInfo.make,
                carInfo.model,
                carInfo.year,
                carNumber,
                carInfo.condition,
                rentalFeedback != null ? rentalFeedback : "",
                cleaningFeedback != null ? cleaningFeedback : "",
                maintenanceFeedback != null ? maintenanceFeedback : "");

        Log.info("CarConditionFeedbackAgent updating...");
        
        // Update the car's condition with the result from CarConditionFeedbackAgent
        carInfo.condition = carConditions.generalCondition();

        // Update the car status based on the required action
        switch (carConditions.carAssignment()) {
            case DISPOSITION:
                carInfo.status = CarStatus.PENDING_DISPOSITION;
                Log.info("Car marked for disposition - awaiting final decision");
                break;
            case MAINTENANCE:
                carInfo.status = CarStatus.IN_MAINTENANCE;
                break;
            case CLEANING:
                carInfo.status = CarStatus.AT_CLEANING;
                break;
            case NONE:
                carInfo.status = CarStatus.AVAILABLE;
                break;
        }
        
        // Persist the changes to the database
        carInfo.persist();

        return carConditions.generalCondition();
    }
}


