package com.carmanagement.resource;

import com.carmanagement.service.CarManagementService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource for car management operations.
 */
@Path("/car-management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
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
    public Response processRentalReturn(
            @PathParam("carNumber") Integer carNumber,
            @QueryParam("rentalFeedback") String rentalFeedback) {
        
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
     * Process a car return from car wash.
     * 
     * @param carNumber The car number
     * @param carWashFeedback Optional car wash feedback
     * @return Result of the processing
     */
    @POST
    @Path("/car-wash-return/{carNumber}")
    public Response processCarWashReturn(
            @PathParam("carNumber") Integer carNumber,
            @QueryParam("carWashFeedback") String carWashFeedback) {
        
        try {
            String result = carManagementService.processCarReturn(carNumber, "", carWashFeedback);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error processing car wash return: " + e.getMessage())
                    .build();
        }
    }
}


