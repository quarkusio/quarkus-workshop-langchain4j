package com.carmanagement.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

import com.carmanagement.model.CarInfo;
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
     * Process a car return.
     *
     * @param carNumber The car number
     * @param feedback Optional feedback
     * @return Uni that completes with the result
     */
    @POST
    @Path("/return/{carNumber}")
    @Blocking
    public Uni<Response> processReturn(Integer carNumber, @RestQuery String feedback) {

        CarInfo car = CarInfo.findById(carNumber);
        String rentalFeedback = "";
        String cleaningFeedback = "";
        String maintenanceFeedback = "";

        if (car != null) {
            switch (car.status) {
                case RENTED:
                    rentalFeedback = feedback != null ? feedback : "";
                    break;
                case AT_CLEANING:
                    cleaningFeedback = feedback != null ? feedback : "";
                    break;
                case IN_MAINTENANCE:
                    maintenanceFeedback = feedback != null ? feedback : "";
                    break;
            }
        }

        return carManagementService.processCarReturn(carNumber, rentalFeedback, cleaningFeedback, maintenanceFeedback)
            .onItem().transform(result -> Response.ok(result).build())
            .onFailure().recoverWithItem(e -> {
                Log.error(e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error processing car return: " + e.getMessage())
                        .build();
            });
    }

    @GET
    @Path("/report")
    @Produces(MediaType.TEXT_HTML)
    public Response report() {
        return Response.ok(carManagementService.report()).build();
    }
}


