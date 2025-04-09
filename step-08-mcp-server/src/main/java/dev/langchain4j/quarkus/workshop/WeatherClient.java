package dev.langchain4j.quarkus.workshop;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/v1/forecast")
@RegisterRestClient(configKey="weatherclient")
public interface WeatherClient {

    @GET
    public String getForecast(
            @QueryParam("latitude") double latitude,
            @QueryParam("longitude") double longitude,
            @QueryParam("forecastdays") int forecastDays,
            @QueryParam("hourly") String hourly
    );
}
