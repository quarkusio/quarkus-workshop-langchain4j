package com.tripplanner.mcp;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.common.http.TestHTTPResource;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(McpTimeoutTest.TimeoutProfile.class)
class McpTimeoutTest {
    @TestHTTPResource("/mcp") URI endpoint;

    @Test
    void slowFixtureProducesBoundedClientFailure() throws Exception {
        try (var client = new DefaultMcpClient.Builder()
                .transport(new StreamableHttpMcpTransport.Builder().url(endpoint.toString()).build())
                .protocolVersion("2025-11-25").autoHealthCheck(false)
                .initializationTimeout(Duration.ofSeconds(5))
                .toolExecutionTimeout(Duration.ofMillis(250)).toolExecutionTimeoutErrorMessage("MCP_TIMEOUT").build()) {
            assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
                var result = client.executeTool(ToolExecutionRequest.builder().name("getWeatherForecast")
                        .arguments("{\"destination\":\"Rome\",\"startDate\":\"2027-07-10\",\"days\":\"3\"}").build());
                // This client version returns timeout text with isError=false. Validate the payload too.
                assertEquals("MCP_TIMEOUT", result.resultText());
            });
        }
    }

    public static class TimeoutProfile implements QuarkusTestProfile {
        @Override public Map<String, String> getConfigOverrides() {
            return Map.of("trip.intelligence.scenario", "timeout");
        }
    }
}
