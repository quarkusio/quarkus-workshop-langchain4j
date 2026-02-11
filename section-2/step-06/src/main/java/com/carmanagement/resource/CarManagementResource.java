package com.carmanagement.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

import com.carmanagement.service.CarManagementService;

/**
 * REST resource for car management operations.
 * Uses blocking processing for AI agent workflows.
 */
@Path("/car-management")
public class CarManagementResource {
    
    @Inject
    CarManagementService carManagementService;
    
    /**
     * Process a car return from rental.
     * This is a blocking operation due to AI agent processing.
     *
     * @param carNumber The car number
     * @param rentalFeedback Optional rental feedback
     * @return Uni that completes with the result
     */
    @POST
    @Path("/rental-return/{carNumber}")
    @Blocking
    public Uni<Response> processRentalReturn(Integer carNumber, @RestQuery String rentalFeedback) {
        
        return carManagementService.processCarReturn(carNumber, rentalFeedback, "", "")
            .onItem().transform(result -> Response.ok(result).build())
            .onFailure().recoverWithItem(e -> {
                Log.error(e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error processing rental return: " + e.getMessage())
                        .build();
            });
    }
    
    /**
     * Process a car return from cleaning.
     *
     * @param carNumber The car number
     * @param cleaningFeedback Optional cleaning feedback
     * @return Uni that completes with the result
     */
    @POST
    @Path("/cleaningReturn/{carNumber}")
    public Uni<Response> processCleaningReturn(Integer carNumber, @RestQuery String cleaningFeedback) {
        
        return carManagementService.processCarReturn(carNumber, "", cleaningFeedback, "")
            .onItem().transform(result -> Response.ok(result).build())
            .onFailure().recoverWithItem(e -> {
                Log.error(e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error processing cleaning return: " + e.getMessage())
                        .build();
            });
    }
    
    /**
     * Process a car return from maintenance.
     *
     * @param carNumber The car number
     * @param maintenanceFeedback Optional maintenance feedback
     * @return Uni that completes with the result
     */
    @POST
    @Path("/maintenance-return/{carNumber}")
    public Uni<Response> processMaintenanceReturn(Integer carNumber, @RestQuery String maintenanceFeedback) {
        
        return carManagementService.processCarReturn(carNumber, "", "", maintenanceFeedback)
            .onItem().transform(result -> Response.ok(result).build())
            .onFailure().recoverWithItem(e -> {
                Log.error(e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error processing maintenance return: " + e.getMessage())
                        .build();
            });
    }
}


