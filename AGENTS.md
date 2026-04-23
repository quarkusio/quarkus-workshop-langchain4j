# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Project Overview

This repository contains a comprehensive, hands-on workshop for building AI-infused applications and agentic systems using Quarkus and LangChain4j. The workshop teaches developers how to integrate Large Language Models into Quarkus applications, build intelligent chatbots with structured outputs and guardrails, implement Retrieval-Augmented Generation (RAG) patterns, use remote tools via Model Context Protocol (MCP), and design agentic systems with workflow and supervisor patterns.

Throughout the workshop, participants build an LLM-powered customer support chatbot for a car rental company, progressively adding features from basic LLM integration to complex multi-agent systems.

## Technology Stack

The workshop uses Java 21 with Quarkus 3.34.3 and the LangChain4j Quarkiverse extension (version 1.9.1). Maven handles the build process, while the documentation is built with MkDocs using Python and Pipenv. The UI components leverage Vaadin Web Components and wc-chatbot for the chat interface.

## Project Structure

This is a multi-module Maven project organized into two main sections. The first section contains 11 steps focused on AI-infused applications, covering topics from basic LLM integration and AI Services through prompt engineering, structured outputs, guardrails, RAG patterns, MCP integration, and observability. These steps are located in `section-1/step-XX/` directories, with the final state available in `section-1/step-11/`.

The second section contains 7 steps dedicated to agentic systems, exploring agentic workflows, multi-agent collaboration, supervisor patterns, and Agent-to-Agent (A2A) communication. These are found in `section-2/step-XX/` directories, with the final state in `section-2/step-07/`.

The documentation lives in the `docs/` directory and can be served locally at http://127.0.0.1:8000/ or accessed online at https://quarkus.io/quarkus-workshop-langchain4j/.

## Building and Running

You'll need Java 21 or higher, Maven 3.8 or higher, and Python 3.x with pipenv for the documentation. You'll also need an OpenAI API key or access to a compatible LLM endpoint.

Each step is a self-contained Quarkus application. To run any step, navigate to its directory and execute `./mvnw quarkus:dev`. The application will start on http://localhost:8080 with Quarkus dev mode features like live reload and the dev UI enabled.

To build the entire project from the root directory, run `./mvnw clean install`. This builds all modules in sequence.

For the documentation, navigate to the `docs` directory, install pipenv if needed, run `pipenv install`, and then `pipenv run mkdocs serve --livereload`. The documentation will be available at http://127.0.0.1:8000/.

## Development Conventions

AI Services are defined as interfaces annotated with `@RegisterAiService`. These services are typically `@SessionScoped` to maintain conversation continuity across multiple interactions.

In Section 2, Agents are defined as interfaces with the `@Agent` annotation, usually accompanied by `@SystemMessage` and `@UserMessage` annotations to define their behavior and prompts. Tools are classes with methods annotated with `@Tool` and are registered via `@ToolBox`.

The package structure differs between sections. Section 1 uses the simpler `dev.langchain4j.quarkus.workshop` package, while Section 2 uses `com.carmanagement` with subpackages for `agentic`, `model`, `resource`, and `service`.

LLM configuration is handled in `application.properties`, and each step may have specific configuration requirements. API keys should be set via environment variables or properties files.

## Workshop Workflow

Each step builds incrementally on the previous one, and the step directories contain the final state of that step. Participants can start from any step by copying or opening that directory directly. When working with the workshop, make changes in a working copy rather than directly in the step directories.

The workshop is designed for progressive learning, with earlier steps being simpler and later steps introducing more advanced concepts. When helping with workshop content, always check the corresponding documentation in `docs/docs/section-X/step-XX.md` for context and instructions.

## Key Architectural Patterns

The AI Service pattern is straightforward:

```java
@SessionScoped
@RegisterAiService
public interface CustomerSupportAgent {
    String chat(String userMessage);
}
```

The Agent pattern used in Section 2 is more elaborate:

```java
@Agent("Agent description")
@ToolBox(ToolClass.class)
@SystemMessage("System instructions...")
@UserMessage("User message template with {parameters}")
String processTask(String param1, String param2);
```

Tools follow a simple pattern:

```java
@Tool("Tool description")
public String toolMethod(String param) {
    // Implementation
}
```

## Important Considerations

Each step directory is a complete, runnable project. Don't assume dependencies between steps. The root `pom.xml` is just a parent aggregator, and each step has its own complete `pom.xml` with all necessary dependencies.

LLM endpoints, API keys, and model configurations vary by step, so always check `application.properties` in the specific step you're working with. The workshop uses web components for the chat interface, with UI code typically located in `src/main/resources/META-INF/resources/`.

Section 1 uses a simpler package structure and focuses on single-agent patterns, while Section 2 introduces more complex package organization and multi-agent systems. Steps 8 and beyond in Section 1, as well as steps in Section 2, may involve external services or remote agents, so check for additional setup requirements.

## Documentation Writing Guidelines

When writing or modifying documentation for this workshop, use natural, flowing prose rather than the typical AI pattern of bullet points followed by colons and descriptions. Write as a human would write technical documentation.

Avoid creating artificial section divisions like "Part 1", "Part 2", or "Step 1", "Step 2" within a single page. The documentation already has a table of contents that provides navigation structure. Instead, use descriptive section titles that clearly indicate what each section covers.

Use bullet points sparingly and only when they genuinely improve readability, such as when listing prerequisites, commands, or distinct items that don't require explanation. When explaining concepts, processes, or providing instructions, prefer paragraph form with clear transitions between ideas.

For example, instead of writing:
```
Prerequisites:
- Java 21: Required for running the application
- Maven: Used for building the project
- API Key: Needed to access the LLM
```

Write naturally:
```
You'll need Java 21 or higher, Maven 3.8 or higher, and an OpenAI API key or access to a compatible LLM endpoint.
```

When describing a process, integrate the steps into flowing paragraphs rather than numbered lists, unless the sequence is complex enough that numbered steps genuinely aid comprehension.

## Common Commands

Start a step in dev mode with `./mvnw quarkus:dev`. Build without tests using `./mvnw clean package -DskipTests`. Run tests with `./mvnw test`. Clean build artifacts with `./mvnw clean`.

For documentation, build with `cd docs && pipenv run mkdocs build --clean` or serve with live reload using `cd docs && pipenv run mkdocs serve --livereload`.

## Workshop Resources

The workshop website is available at https://quarkus.io/quarkus-workshop-langchain4j/. Additional resources include the Quarkus LangChain4j Guide at https://docs.quarkiverse.io/quarkus-langchain4j/, the LangChain4j Documentation at https://docs.langchain4j.dev/, and the Quarkus Documentation at https://quarkus.io/guides/.