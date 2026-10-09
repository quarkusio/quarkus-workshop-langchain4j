package dev.langchain4j.quarkus.workshop;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class McpServer implements QuarkusTestResourceLifecycleManager {
    static final List<JsonNode> calls = new CopyOnWriteArrayList<>();
    static final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    static final List<String> headers = new CopyOnWriteArrayList<>();
    private HttpServer server;

    @Override
    public Map<String, String> start() {
        try {
            var json = new ObjectMapper();
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/mcp", exchange -> {
                // Streamable HTTP's optional notification channel is not needed here.
                if (!exchange.getRequestMethod().equals("POST")) {
                    exchange.sendResponseHeaders(405, -1);
                    exchange.close();
                    return;
                }
                headers.add(exchange.getRequestHeaders().getFirst("User-Agent"));
                if (exchange.getRequestHeaders().containsKey("Authorization")
                        || exchange.getRequestHeaders().containsKey("x-api-key")) {
                    exchange.sendResponseHeaders(401, -1);
                    exchange.close();
                    return;
                }
                JsonNode request = json.readTree(exchange.getRequestBody());
                requests.add(request);
                if (!request.has("id")) {
                    exchange.sendResponseHeaders(202, -1);
                    exchange.close();
                    return;
                }
                Object result = switch (request.path("method").asText()) {
                    case "initialize" -> Map.of("protocolVersion", "2025-11-25",
                            "capabilities", Map.of("tools", Map.of()),
                            "serverInfo", Map.of("name", "test-server", "version", "1"));
                    case "tools/list" -> Map.of("tools", List.of(
                            Map.of("name", "web_search", "inputSchema", Map.of("type", "object")),
                            Map.of("name", "web_fetch", "inputSchema", Map.of("type", "object"))));
                    case "tools/call" -> {
                        calls.add(request.path("params"));
                        boolean error = request.path("params").path("arguments").path("objective")
                                .asText().equals("rate-limit-test");
                        yield Map.of("isError", error, "content", List.of(Map.of("type", "text",
                                "text", error ? "Rate limit reached" : "Page excerpts")));
                    }
                    default -> Map.of();
                };
                byte[] body = json.writeValueAsBytes(Map.of("jsonrpc", "2.0", "id", request.get("id"), "result", result));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return Map.of("quarkus.langchain4j.mcp.parallel.url",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/mcp");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}
