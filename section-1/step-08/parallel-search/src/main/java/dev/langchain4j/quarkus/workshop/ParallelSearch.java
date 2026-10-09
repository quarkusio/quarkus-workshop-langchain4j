package dev.langchain4j.quarkus.workshop;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.exception.ToolExecutionException;
import io.quarkiverse.langchain4j.mcp.runtime.McpClientName;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

@QuarkusMain
public class ParallelSearch implements QuarkusApplication {

    @Inject
    @McpClientName("parallel")
    McpClient client;

    @Inject
    ObjectMapper json;

    @Override
    public int run(String... args) throws Exception {
        if (args.length != 2 || !(args[0].equals("search") || args[0].equals("fetch"))) {
            System.err.println("Usage: search \"search objective\" | fetch https://example.com/page");
            return 2;
        }

        String tool = args[0].equals("search") ? "web_search" : "web_fetch";
        if (client.listTools().stream().noneMatch(spec -> spec.name().equals(tool))) {
            System.err.println("MCP server does not expose " + tool);
            return 1;
        }

        // One identifier for this invocation, shared by related calls if extended.
        String sessionId = UUID.randomUUID().toString();
        Map<String, Object> arguments = args[0].equals("search")
                ? Map.of("objective", args[1], "search_queries", List.of(args[1]), "session_id", sessionId)
                : Map.of("urls", List.of(args[1]), "session_id", sessionId);
        try {
            var result = client.executeTool(ToolExecutionRequest.builder()
                    .name(tool)
                    .arguments(json.writeValueAsString(arguments))
                    .build());
            if (result.isError()) {
                System.err.println(result.resultText());
                return 1;
            }
            System.out.println(result.resultText());
            return 0;
        } catch (ToolExecutionException e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
