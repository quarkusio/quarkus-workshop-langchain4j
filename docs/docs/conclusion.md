# Conclusion

Alright, this is the end! I hope you enjoyed this tutorial and gained valuable insights into building AI-infused applications.

In just a few hours, we built an intelligent chatbot using Quarkus and Quarkus LangChain4j, demonstrating how to integrate cutting-edge AI capabilities into a modern application. 
Throughout the process, we explored key concepts, including:

## Section 1 - AI Apps
- Integrating a large language model (LLM) seamlessly within a Quarkus application
- Utilizing annotations to efficiently pass prompts and structure interactions
- Implementing the Retrieval Augmented Generation (RAG) pattern to enrich responses with external data
- Leveraging function calling to create _agents_—LLMs that can reason and interact with various system components
- Implementing guardrails to safeguard against common risks, such as prompt injection and LLM misbehavior
- Adding observability and fault tolerance
- Adding an embedded LLM into our Java application

## Section 2 - Agentic Workflows
- Integrating AI agents into a Quarkus application in a similar way to AI services
- Connecting agents into chains using sequence workflows with shared state
- Invoking agents in parallel workflows to perform work more efficiently
- Building conditional workflows that let you control which agents work on a request
- Combining agents and workflows of agents into nested workflows
- Engaging remote agents, potentially built using different agentic frameworks, using Agent2Agent

By the end of this tutorial, you should now have a solid foundation for building AI-enhanced applications with Quarkus, using its powerful tools to create smarter, more responsive systems. 
If you have any questions or feedback, don’t hesitate to reach out to us on [Zulip](https://quarkusio.zulipchat.com/).
We're excited to see what you build next!
