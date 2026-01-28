package com.carmanagement.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.carmanagement.agentic.workflow.CarProcessingWorkflow;
import com.carmanagement.model.CarConditions;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.CarStatus;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;

import java.time.Duration;

/**
 * Service for managing car returns from various operations.
 * Uses async processing to handle Human-in-the-Loop workflow pauses.
 */
@ApplicationScoped
public class CarManagementService {

    @Inject
    CarProcessingWorkflow carProcessingWorkflow;

    /**
     * Process a car return from any operation.
     * This method runs asynchronously to handle workflow pauses for human approval.
     *
     * NOTE: No @Transactional here because the workflow may block for minutes waiting
     * for human input. Transactions are handled at the appropriate level within the workflow.
     *
     * @param carNumber The car number
     * @param rentalFeedback Optional rental feedback
     * @param cleaningFeedback Optional cleaning feedback
     * @param maintenanceFeedback Optional maintenance feedback
     * @return Uni that completes with the result of the processing
     */
    public Uni<String> processCarReturn(Long carNumber, String rentalFeedback, String cleaningFeedback,
                                   String maintenanceFeedback) {
        
        return Uni.createFrom().item(() -> {
            CarInfo carInfo = findCarInfo(carNumber);
            if (carInfo == null) {
                return "Car not found with number: " + carNumber;
            }
                    
            Log.info("FeedbackWorkflow executing...");
            Log.info("  ├─ CleaningFeedbackAgent analyzing...");
            Log.info("  └─ MaintenanceFeedbackAgent analyzing...");
            Log.info("FleetSupervisorAgent orchestrating car processing...");
            
            // Process the car return using the workflow with supervisor
            // This may PAUSE if human approval is needed
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
            
            // Persist the changes to the database in a separate transaction
            updateCarInfo(carInfo);

            return carConditions.generalCondition();
        }).runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
    }
    
    /**
     * Find car info in a read-only transaction
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    CarInfo findCarInfo(Long carNumber) {
        return CarInfo.findById(carNumber);
    }
    
    /**
     * Update car info in a separate transaction after workflow completes.
     * Uses merge to handle detached entity from the workflow.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void updateCarInfo(CarInfo carInfo) {
        // Merge the detached entity back into the persistence context
        CarInfo.getEntityManager().merge(carInfo);
    }
}


