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
     * Process a car return from any status (rental, cleaning, or maintenance).
     * This is a blocking operation due to AI agent processing.
     *
     * @param carNumber The car number
     * @param feedback Optional feedback about the return
     * @param carImage Optional image of the car being returned (multipart form data)
     * @return Uni that completes with the result
     */
    @POST
    @Path("/return/{carNumber}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Blocking
    public Uni<Response> processReturn(Integer carNumber, @RestForm String feedback, @RestForm FileUpload carImage) {
        ImageContent imageContent = toImageContent(carImage);

        return carManagementService.processCarReturn(carNumber, feedback != null ? feedback : "", imageContent)
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

    private ImageContent toImageContent(FileUpload fileUpload) {
        if (fileUpload == null || fileUpload.filePath() == null) {
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(fileUpload.filePath());
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String mimeType = fileUpload.contentType();
            return new ImageContent(base64, mimeType);
        } catch (IOException e) {
            Log.error("Failed to read uploaded car image", e);
            return null;
        }
    }
}


