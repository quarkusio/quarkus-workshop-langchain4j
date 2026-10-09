# Calling Parallel Search MCP from Quarkus

This optional Step 08 example calls a hosted MCP server through the same Quarkus
LangChain4j extension used by the weather lesson. [Parallel Search MCP](https://docs.parallel.ai/integrations/mcp/search-mcp)
provides anonymous web search and page extraction without an API key. Free access
is rate limited, so an exhausted allowance requires waiting before trying again.

Use Java 21 or higher. From the repository root, build the standalone example:

```shell
./mvnw -f section-1/step-08/parallel-search/pom.xml clean package
```

Search for documentation, then fetch a particular page when you need its contents:

```shell
java -jar section-1/step-08/parallel-search/target/quarkus-app/quarkus-run.jar search "Quarkus LangChain4j MCP client configuration"
java -jar section-1/step-08/parallel-search/target/quarkus-app/quarkus-run.jar fetch https://docs.quarkiverse.io/quarkus-langchain4j/dev/mcp.html
```

Inspect the JSON output for source URLs and excerpts. Search returns a `results`
array, while fetch returns extracted page content and any per-page errors. A tool
error is printed to stderr and exits with status 1. Check fetch's per-page errors
even when the tool call itself succeeds.

The named `parallel` client is injected with `@McpClientName` and configured in
`src/main/resources/application.properties` with Streamable HTTP and a project
User-Agent. The client explicitly uses MCP protocol `2025-11-25`, which the
hosted endpoint supports, rather than probing the newer stateless protocol.
The command discovers the selected tool and executes it directly,
without an LLM. `ObjectMapper` serializes the arguments so quoted queries remain
valid JSON. Each invocation creates a session identifier for free-tier accounting.
If you extend the command to make related search and fetch calls in one session,
reuse that identifier across those calls.

The weather client and customer support agent in the main Step 08 application
keep their existing configuration. Continue that lesson in its original directory.
