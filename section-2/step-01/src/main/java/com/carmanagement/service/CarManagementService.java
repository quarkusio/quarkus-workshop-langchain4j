package com.carmanagement.service;

import com.carmanagement.agentic.agents.CarWashAgent;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.CarStatus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Service for managing car returns from various operations.
 */
@ApplicationScoped
public class CarManagementService {

    @Inject
    CarWashAgent carWashAgent;

    // --8<-- [start:processCarReturn]
    /**
     * Process a car return from any operation.
     *
     * @param carNumber The car number
     * @param rentalFeedback Optional rental feedback
     * @param carWashFeedback Optional car wash feedback
     * @return Result of the processing
     */
    @Transactional
    public String processCarReturn(Long carNumber, String rentalFeedback, String carWashFeedback) {
        CarInfo carInfo = CarInfo.findById(carNumber);
        if (carInfo == null) {
            return "Car not found with number: " + carNumber;
        }

        // Process the car result
        String result = carWashAgent.processCarWash(
                carInfo.make,
                carInfo.model,
                carInfo.year,
                carNumber,
                rentalFeedback != null ? rentalFeedback : "",
                carWashFeedback != null ? carWashFeedback : "");

        if (result.toUpperCase().contains("CARWASH_NOT_REQUIRED")) {
            carInfo.status = CarStatus.AVAILABLE;
            carInfo.persist();
        }

        return result;
    }
    // --8<-- [end:processCarReturn]
}

