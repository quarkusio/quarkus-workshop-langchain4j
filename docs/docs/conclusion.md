# Conclusion

Alright, this is the end! We hope you enjoyed this workshop and gained valuable insights into building AI-infused applications and agentic systems.

Throughout the workshop, we followed the Miles of Smiles car rental company across three sections, building progressively more sophisticated AI-powered systems with Quarkus and Quarkus LangChain4j. Here's a recap of what we covered:

## Section 1 - AI-Infused Applications
- Integrating a large language model (LLM) seamlessly within a Quarkus application
- Utilizing annotations to efficiently pass prompts and structure interactions
- Implementing the Retrieval Augmented Generation (RAG) pattern to enrich responses with external data
- Leveraging function calling to create tools that LLMs can reason over and invoke
- Integrating remote tools and services via the Model Context Protocol (MCP)
- Implementing guardrails to safeguard against common risks, such as prompt injection and LLM misbehavior
- Adding observability and fault tolerance

## Section 2 - Agentic Systems
- Integrating AI agents into a Quarkus application in a similar way to AI services
- Connecting agents into chains using sequence workflows with shared state
- Invoking agents in parallel workflows to perform work more efficiently
- Building conditional workflows that let you control which agents work on a request
- Combining agents and workflows of agents into nested workflows
- Keeping a human in the loop for approval and intervention
- Processing multimodal inputs such as images alongside text
- Engaging remote agents, potentially built using different agentic frameworks, using Agent-to-Agent (A2A) communication
- Deploying a multi-agent system to Kubernetes/OpenShift

## Section 3 - Enterprise Agentic AI Patterns _(experimental)_
- Defining and dynamically discovering agent skills at runtime
- Enforcing guardrails and compliance policies across agents
- Building event-driven workflows with Quarkus Flow
- Persisting workflow state across restarts with PostgreSQL
- Applying voting and loop patterns with adaptive model selection
- Composing custom orchestration with a `PlannerAgent`
- Integrating external tools via MCP
- Communicating with remote agents via A2A
- Evaluating agent behavior with LLM-based testing

By the end of this workshop, you should have a solid foundation for building AI-enhanced applications with Quarkus, from a simple chatbot all the way to an enterprise-grade agentic system.
If you have any questions or feedback, don't hesitate to reach out to us on [Zulip](https://quarkusio.zulipchat.com/) or open a discussion on [GitHub](https://github.com/quarkiverse/quarkus-langchain4j/discussions).
We're excited to see what you build next!
