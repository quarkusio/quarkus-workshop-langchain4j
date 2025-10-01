package com.carmanagement.resource;

import com.carmanagement.service.CarManagementService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestQuery;

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
            String result = carManagementService.processCarReturn(carNumber, rentalFeedback, "", "");
            return Response.ok(result).build();
        } catch (Exception e) {
            Log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error processing rental return: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Process a car return from car wash.
     * 
     * @param carNumber The car number
     * @param carWashFeedback Optional car wash feedback
     * @return Result of the processing
     */
    @POST
    @Path("/car-wash-return/{carNumber}")
    public Response processCarWashReturn(Long carNumber, @RestQuery String carWashFeedback) {
        
        try {
            String result = carManagementService.processCarReturn(carNumber, "", carWashFeedback, "");
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error processing car wash return: " + e.getMessage())
                    .build();
        }
    }
    
    // --8<-- [start:maintenanceReturn]
    /**
     * Process a car return from maintenance.
     *
     * @param carNumber The car number
     * @param maintenanceFeedback Optional maintenance feedback
     * @return Result of the processing
     */
    @POST
    @Path("/maintenance-return/{carNumber}")
    public Response processMaintenanceReturn(Long carNumber, @RestQuery String maintenanceFeedback) {
        
        try {
            String result = carManagementService.processCarReturn(carNumber, "", "", maintenanceFeedback);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error processing maintenance return: " + e.getMessage())
                    .build();
        }
    }
    // --8<-- [end:maintenanceReturn]
}


