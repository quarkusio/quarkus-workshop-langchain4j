package com.tripplanner.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.http.TestHTTPResource;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class McpTransportTest {
    @TestHTTPResource("/mcp") URI endpoint;
    @Inject ObjectMapper json;

    DefaultMcpClient client() {
        return new DefaultMcpClient.Builder()
                .transport(new StreamableHttpMcpTransport.Builder().url(endpoint.toString()).build())
                .protocolVersion("2025-11-25").autoHealthCheck(false)
                .initializationTimeout(Duration.ofSeconds(5)).toolExecutionTimeout(Duration.ofSeconds(2)).build();
    }

    @Test
    void initializesDiscoversAndCallsBothTools() throws Exception {
        try (var client = client()) {
            var weatherTool = client.listTools().stream().filter(t -> t.name().equals("getWeatherForecast"))
                    .findFirst().orElseThrow();
            assertInstanceOf(JsonStringSchema.class, weatherTool.parameters().properties().get("days"));
            var weather = client.executeTool(ToolExecutionRequest.builder().name("getWeatherForecast")
                    .arguments("{\"destination\":\"Rome\",\"startDate\":\"2027-07-10\",\"days\":\"3\"}").build());
            assertFalse(weather.isError());
            var forecast = json.readTree(weather.resultText());
            assertEquals("Rome", forecast.path("destination").asText());
            assertTrue(forecast.path("summary").asText().contains("Workshop fixture"));
            var poi = client.executeTool(ToolExecutionRequest.builder().name("getPointsOfInterest")
                    .arguments("{\"destination\":\"Rome\",\"tripType\":\"family\"}").build());
            assertFalse(poi.isError());
            var entries = json.readTree(poi.resultText()).path("entries");
            assertTrue(entries.isArray(), poi.resultText());
            assertFalse(entries.isEmpty());
            assertEquals("Rome", entries.get(0).path("destination").asText());
        }
    }

    @Test
    void invalidDurationFailsAndUnknownDestinationHasNoPoi() throws Exception {
        try (var client = client()) {
            assertThrows(dev.langchain4j.exception.ToolExecutionException.class, () -> client.executeTool(ToolExecutionRequest.builder().name("getWeatherForecast")
                    .arguments("{\"destination\":\"Rome\",\"startDate\":\"2027-07-10\",\"days\":\"0\"}").build()));
            var empty = client.executeTool(ToolExecutionRequest.builder().name("getPointsOfInterest")
                    .arguments("{\"destination\":\"Atlantis\",\"tripType\":\"family\"}").build());
            assertFalse(empty.isError());
            assertEquals(0, json.readTree(empty.resultText()).path("entries").size());
        }
    }
}
