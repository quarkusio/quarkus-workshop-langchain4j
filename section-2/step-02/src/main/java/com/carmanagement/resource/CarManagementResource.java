package com.carmanagement.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.logging.Log;

import com.carmanagement.model.CarInfo;
import com.carmanagement.service.CarManagementService;

/**
 * REST resource for car management operations.
 */
@Path("/car-management")
public class CarManagementResource {

    @Inject
    CarManagementService carManagementService;

    /**
     * Process a car return.
     *
     * @param carNumber The car number
     * @param feedback Optional feedback
     * @return Result of the processing
     */
    @POST
    @Path("/return/{carNumber}")
    public Response processReturn(Integer carNumber, @RestQuery String feedback) {

        try {
            CarInfo car = CarInfo.findById(carNumber);
            String rentalFeedback = "";
            String cleaningFeedback = "";

            if (car != null) {
                switch (car.status) {
                    case RENTED:
                        rentalFeedback = feedback != null ? feedback : "";
                        break;
                    case AT_CLEANING:
                        cleaningFeedback = feedback != null ? feedback : "";
                        break;
                }
            }

            String result = carManagementService.processCarReturn(carNumber, rentalFeedback, cleaningFeedback);
            return Response.ok(result).build();
        } catch (Exception e) {
            Log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error processing return: " + e.getMessage())
                    .build();
        }
    }
}


