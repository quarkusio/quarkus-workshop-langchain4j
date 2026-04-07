package com.carmanagement.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import dev.langchain4j.data.message.ImageContent;
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
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Blocking
    public Uni<Response> processRentalReturn(Integer carNumber, @RestForm String rentalFeedback, @RestForm FileUpload carImage) {
        ImageContent imageContent = toImageContent(carImage);

        return carManagementService.processCarReturn(carNumber, rentalFeedback, "", "", imageContent)
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
        
        return carManagementService.processCarReturn(carNumber, "", cleaningFeedback, "", EMPTY_IMAGE)
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
        
        return carManagementService.processCarReturn(carNumber, "", "", maintenanceFeedback, EMPTY_IMAGE)
            .onItem().transform(result -> Response.ok(result).build())
            .onFailure().recoverWithItem(e -> {
                Log.error(e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error processing maintenance return: " + e.getMessage())
                        .build();
            });
    }

    @GET
    @Path("/report")
    @Produces(MediaType.TEXT_HTML)
    public Response report() {
        return Response.ok(carManagementService.report()).build();
    }

    private static final ImageContent EMPTY_IMAGE = ImageContent.from(java.nio.file.Path.of("src", "main", "resources", "white-pixel-icon.png"), "image/png");

    private ImageContent toImageContent(FileUpload fileUpload) {
        if (fileUpload == null || fileUpload.filePath() == null) {
            return EMPTY_IMAGE;
        }
        try {
            byte[] bytes = Files.readAllBytes(fileUpload.filePath());
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String mimeType = fileUpload.contentType();
            return new ImageContent(base64, mimeType);
        } catch (IOException e) {
            Log.error("Failed to read uploaded car image", e);
            return EMPTY_IMAGE;
        }
    }
}


