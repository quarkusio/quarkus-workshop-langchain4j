package dev.langchain4j.quarkus.workshop;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/v1/forecast")
@RegisterRestClient(configKey="weatherclient")
public interface WeatherClient {

    @GET
    String getForecast(
            @RestQuery double latitude,
            @RestQuery double longitude,
            @RestQuery int forecastDays,
            @RestQuery String hourly
    );
}
