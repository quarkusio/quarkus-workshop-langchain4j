package dev.langchain4j.quarkus.workshop;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/")
public class WeatherService {
    @RestClient
    WeatherClient wc;

    @Path("weather")
    @GET
    public String getWeather(@RestQuery double latitude,
                             @RestQuery double longitude){
        return wc.getForecast(
                latitude,
                longitude,
                16,
                "temperature_2m,snowfall,rain,precipitation,precipitation_probability");
    }
}
