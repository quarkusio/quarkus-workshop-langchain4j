# Step 06: MCP integration with non-AI agents

This step adds a Trip Intelligence MCP server and wires its tools as declarative `@McpClientAgent` interfaces in the trip planner workflow. The MCP server exposes weather forecasts and points of interest as tools. The trip planner consumes them through two `@McpClientAgent` interfaces (`WeatherAgent`, `PointsOfInterestAgent`) that call the MCP server deterministically, before any LLM runs.

The project is a multi-module Maven build:

- `mcp-server/` — Quarkus MCP server with `@Tool` methods returning deterministic stub data
- `trip-planner/` — The trip planner app from Step 05 with added `quarkus-langchain4j-mcp` dependency, `@McpClientAgent` interfaces, and a `DestinationIntelligence` parallel phase

The agent pipeline, voting loop, pricing tool, output guardrails, Flow descriptor, persistence, REST envelopes, and browser UI are inherited from Step 05. Vehicle and itinerary research agents now receive weather and POI data from the scope.

## Run

Use Java 21 or newer, Docker or Podman, and a real `OPENAI_API_KEY` for normal application use.

**Terminal 1 — MCP Server:**
```bash
cd mcp-server
./mvnw quarkus:dev
```

**Terminal 2 — Trip Planner:**
```bash
cd trip-planner
./mvnw quarkus:dev
```

Open http://localhost:8080 and http://localhost:8080/q/dev. The MCP server runs on port 8085.

## Tests

MCP server tests:
```bash
cd mcp-server && ./mvnw test
```

Trip planner tests (persistence + MCP agent unit tests):
```bash
cd trip-planner && ./mvnw test -Dquarkus.http.test-port=0
```

Tests need no real API key, live model, or running MCP server. The `McpAgentTest` validates the `@McpClientAgent` interface declarations. The existing persistence tests are inherited from Step 05.

## Guides

- [Quarkus MCP Server](https://docs.quarkiverse.io/quarkus-mcp-server/dev/index.html)
- [Quarkus LangChain4j MCP Client](https://docs.quarkiverse.io/quarkus-langchain4j/dev/mcp.html)
- [LangChain4j Non-AI Agents](https://docs.langchain4j.dev/tutorials/agents/#non-ai-agents)
- [LangChain4j MCP-based Tool Agents](https://docs.langchain4j.dev/tutorials/agents/#mcp-based-tool-agents)
