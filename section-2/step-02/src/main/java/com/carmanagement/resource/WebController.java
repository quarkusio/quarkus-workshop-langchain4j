package com.carmanagement.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Controller for serving web pages.
 */
@Path("/")
public class WebController {
    
    /**
     * Serve the main page of the application.
     * 
     * @return HTML content of the main page
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String index() {
        return readResourceFile("/templates/index.html");
    }
    
    /**
     * Helper method to read a resource file.
     * 
     * @param path Path to the resource file
     * @return Content of the resource file as a string
     */
    private String readResourceFile(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                return "Resource not found: " + path;
            }
            
            try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").next();
            }
        } catch (Exception e) {
            return "Error reading resource: " + e.getMessage();
        }
    }
}


