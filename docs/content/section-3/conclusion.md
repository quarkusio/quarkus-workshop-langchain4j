---
title: "Enterprise Agentic Patterns"
layout: page
content-toc: true
---
# Conclusion: Enterprise Agentic Patterns

You've completed Section 3. Over seven steps, you built a Customer Trip Planner that started as a synchronous multi-agent pipeline and grew into a persistent, event-driven system with guardrails, external tool integration, and automated evaluation.

## What you built

The trip planner began in Step 00 as a baseline with a research agent, a cost estimator, and a synchronous REST endpoint. Each subsequent step added one enterprise concern without rewriting what came before.

Step 01 introduced agent skills as Markdown files on the classpath, giving the agents domain-specific guidance that can be updated without changing code. Step 02 added output guardrails and a rental pricing tool, so the planner rejects plans that violate safety or compliance rules before they reach the customer.

Step 03 brought in voting and iterative loops. Three evaluators score the same vehicle recommendation, a voting strategy averages their scores, and a reviser keeps improving the vehicle until it passes. If it still falls short after three revisions, the planner gives up with a `quality_not_met` error. Nobody wants to send a mediocre car on a family holiday. This step also introduced adaptive model selection, which switches the reviser to a more capable model once the recommendation is close to good enough.

Step 04 moved the planner into an event-driven workflow using Quarkus Flow and Kafka. The workflow generates a plan, then waits for the customer's approval without holding an HTTP connection open, and a decision event resumes it. Step 05 made that workflow durable by persisting the plan and the approval state in PostgreSQL, so a restart doesn't lose a customer's trip.

Step 06 connected the planner to external services through MCP. A dedicated `@McpClientAgent` interface calls the weather and points-of-interest tools deterministically, without giving the language model the option to skip them. Step 07 added an evaluation harness that runs invariant checks and a judge model against saved plan outputs, producing a score and a per-case report. Langfuse, started for you by Dev Services, attaches those scores to the OpenTelemetry traces from the planning run, so you can see which run earned which score.

## Patterns worth keeping

A few patterns from this section transfer directly to other systems.

Skills as external content let domain experts change agent behavior without a code change or a redeployment. Output guardrails separate enforcement from generation, so the model can produce freely while a deterministic check catches what it shouldn't return. The voting and loop pattern applies anywhere a single model call isn't reliable enough: run several, aggregate, and retry if needed.

Quarkus Flow with persistence turns a stateless agent pipeline into something that survives restarts and can pause for human approval. MCP integration with `@McpClientAgent` gives you the benefits of tool calling without the unpredictability of letting the model decide whether to call the tool at all.

Running evaluation as a test suite lets you catch regressions every time you change a prompt or swap a model. The combination of deterministic invariants and a judge model covers both structural correctness and subjective quality.

## Where to go from here

Section 2 Step 08 covers A2A communication if you want to distribute agents across services. The Quarkus LangChain4j documentation at [docs.quarkiverse.io](https://docs.quarkiverse.io/quarkus-langchain4j/) has the full API reference for everything used in this section. The [LangChain4j docs](https://docs.langchain4j.dev/) cover the underlying Java library.

If you're deploying to production, the bonus step in Section 2 walks through Kubernetes deployment with Quarkus profiles, health probes, and service discovery. Add the observability setup from Step 07 and you get traces and evaluation scores from production runs too. Management has already asked for a dashboard.
