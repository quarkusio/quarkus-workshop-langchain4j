package com.carmanagement.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.carmanagement.agentic.agents.CleaningAgent;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.CarStatus;

/**
 * Service for managing car returns from various operations.
 */
@ApplicationScoped
public class CarManagementService {

    @Inject
    CleaningAgent cleaningAgent;

    // --8<-- [start:processCarReturn]
    /**
     * Process a car return from any operation.
     *
     * @param carNumber The car number
     * @param rentalFeedback Optional rental feedback
     * @param cleaningFeedback Optional cleaning feedback
     * @return Result of the processing
     */
    @Transactional
    public String processCarReturn(Long carNumber, String rentalFeedback, String cleaningFeedback) {
        CarInfo carInfo = CarInfo.findById(carNumber);
        if (carInfo == null) {
            return "Car not found with number: " + carNumber;
        }

        // Process the car result
        String result = cleaningAgent.processCleaning(
                carInfo.make,
                carInfo.model,
                carInfo.year,
                carNumber,
                rentalFeedback != null ? rentalFeedback : "",
                cleaningFeedback != null ? cleaningFeedback : "");

        if (result.toUpperCase().contains("CLEANING_NOT_REQUIRED")) {
            carInfo.status = CarStatus.AVAILABLE;
            carInfo.persist();
        }

        return result;
    }
    // --8<-- [end:processCarReturn]
}

