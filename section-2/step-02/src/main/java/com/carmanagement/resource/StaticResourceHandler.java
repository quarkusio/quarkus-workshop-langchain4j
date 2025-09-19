package com.carmanagement.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;

/**
 * Handler for serving static resources like CSS and JavaScript files.
 */
@Path("/")
public class StaticResourceHandler {

    /**
     * Serve CSS files.
     * 
     * @param fileName The name of the CSS file
     * @return The CSS file content
     */
    @GET
    @Path("/css/{fileName}")
    @Produces("text/css")
    public Response getCss(@PathParam("fileName") String fileName) {
        return serveStaticResource("static/css/" + fileName);
    }

    /**
     * Serve JavaScript files.
     * 
     * @param fileName The name of the JavaScript file
     * @return The JavaScript file content
     */
    @GET
    @Path("/js/{fileName}")
    @Produces("application/javascript")
    public Response getJs(@PathParam("fileName") String fileName) {
        return serveStaticResource("static/js/" + fileName);
    }

    /**
     * Helper method to serve a static resource.
     * 
     * @param resourcePath Path to the resource
     * @return Response containing the resource content
     */
    private Response serveStaticResource(String resourcePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        
        if (is == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Resource not found: " + resourcePath)
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        
        return Response.ok(is).build();
    }
}


