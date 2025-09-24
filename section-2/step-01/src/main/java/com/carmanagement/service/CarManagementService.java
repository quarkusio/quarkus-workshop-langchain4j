package com.carmanagement.service;

import com.carmanagement.agentic.agents.CarWashAgent;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.CarStatus;
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
    CarWashAgent carWashAgent;

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
}

