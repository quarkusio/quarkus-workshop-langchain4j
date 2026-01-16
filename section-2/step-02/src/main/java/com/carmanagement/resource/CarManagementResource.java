package com.carmanagement.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.logging.Log;

import com.carmanagement.service.CarManagementService;

/**
 * REST resource for car management operations.
 */
@Path("/car-management")
public class CarManagementResource {
    
    @Inject
    CarManagementService carManagementService;
    
    /**
     * Process a car return from rental.
     * 
     * @param carNumber The car number
     * @param rentalFeedback Optional rental feedback
     * @return Result of the processing
     */
    @POST
    @Path("/rental-return/{carNumber}")
    public Response processRentalReturn(Long carNumber, @RestQuery String rentalFeedback) {
        
        try {
            String result = carManagementService.processCarReturn(carNumber, rentalFeedback, "");
            return Response.ok(result).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error processing rental return: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Process a car return from cleaning.
     * 
     * @param carNumber The car number
     * @param cleaningFeedback Optional cleaning feedback
     * @return Result of the processing
     */
    @POST
    @Path("/cleaningReturn/{carNumber}")
    public Response processCleaningReturn(Long carNumber, @RestQuery String cleaningFeedback) {
        
        try {
            String result = carManagementService.processCarReturn(carNumber, "", cleaningFeedback);
            return Response.ok(result).build();
        } catch (Exception e) {
            Log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error processing cleaning return: " + e.getMessage())
                    .build();
        }
    }
}


