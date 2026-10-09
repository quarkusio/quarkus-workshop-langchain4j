package dev.langchain4j.quarkus.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(McpServer.class)
class ParallelSearchTest {
    @Inject
    ParallelSearch command;

    @Test
    void configuredClientDiscoversAndCallsBothToolsAnonymously() throws Exception {
        McpServer.calls.clear();
        String query = "Quarkus \"MCP\" client configuration";
        assertEquals(0, command.run("search", query));
        assertEquals(0, command.run("fetch", "https://quarkus.io/guides/"));
        assertEquals(2, McpServer.calls.size());
        var search = McpServer.calls.get(0);
        assertEquals("web_search", search.path("name").asText());
        assertEquals(query, search.path("arguments").path("objective").asText());
        assertEquals(query, search.path("arguments").path("search_queries").get(0).asText());
        UUID.fromString(search.path("arguments").path("session_id").asText());
        var fetch = McpServer.calls.get(1);
        assertEquals("web_fetch", fetch.path("name").asText());
        assertEquals("https://quarkus.io/guides/", fetch.path("arguments").path("urls").get(0).asText());
        UUID.fromString(fetch.path("arguments").path("session_id").asText());
        assertTrue(McpServer.requests.stream().anyMatch(request ->
                request.path("method").asText().equals("initialize")
                && request.path("params").path("protocolVersion").asText().equals("2025-11-25")));
        assertTrue(McpServer.requests.stream().noneMatch(request ->
                request.path("method").asText().equals("server/discover")));
        assertTrue(McpServer.headers.size() >= 3);
        assertTrue(McpServer.headers.stream().allMatch("quarkus-workshop-langchain4j/1.0.0"::equals));
    }

    @Test
    void toolErrorsAndInvalidArgumentsReturnNonzeroStatus() throws Exception {
        assertEquals(1, command.run("search", "rate-limit-test"));
        assertEquals(2, command.run());
        assertEquals(2, command.run("unknown", "query"));
    }
}
