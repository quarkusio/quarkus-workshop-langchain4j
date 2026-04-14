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
            String result = carManagementService.processCarReturn(carNumber, feedback != null ? feedback : "");
            return Response.ok(result).build();
        } catch (Exception e) {
            Log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error processing return: " + e.getMessage())
                    .build();
        }
    }
}


