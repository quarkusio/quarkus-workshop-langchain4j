# Step 02 - LLM configuration

In this step, we will play with various configurations of the language model (LLM) that we will use in the subsequent steps.

You can either use the code from `step-01` and continue from there, or check the final code of the step located in the `step-02` directory.

??? important "Do not forget to close the application"
    If you have the application running from the previous step and decide to use the `step-02` directory, make sure to stop it (CTRL+C) before continuing.

## The configuration

The application is configured from the `src/main/resources/application.properties` file:

```properties title="application.properties"
--8<-- "../../section-1/step-01/src/main/resources/application.properties"
```

The `quarkus.langchain4j.openai.api-key` property is the OpenAI API key. In our case we are configuring it to read from the `OPENAI_API_KEY` environment variable.

The rest of the configuration indicates which model is used (`gpt-4o-mini`) and whether to log the requests and responses to the model in the terminal.

!!! important "Reloading"
    After changing a configuration property, you need to force a restart of the application to apply the changes.
    Simply submitting a new chat message in the UI does not trigger it (it only sends a websocket message rather than an HTTP request),
    so you have to refresh the page in your browser.

!!! info
    The precise meaning of most model parameters is described on the website of [OpenAI](https://platform.openai.com/docs/api-reference/chat/create).

## Temperature

`quarkus.langchain4j.openai.chat-model.temperature` controls the randomness of the model's responses.
Lowering the temperature will make the model more conservative, while increasing it will make it more creative.

Try adding

```properties
quarkus.langchain4j.openai.chat-model.temperature=0.1
```

to `src/main/resources/application.properties` and try asking 

```
Describe a sunset over the mountains
```

then set the temperature to`1.5` and ask the question again, observing the different styles of the responses. With a too high temperature, the model often starts producing garbage, takes way too long to respond, or fails to produce a valid response at all.

Applications that require deterministic responses should set the temperature to 0.
Note that it will note guarantee the same response for the same input, but it will make the responses more predictable.

Applications that require a bit more creativity (e.g. to generate text for a story) can set the temperature to 0.3 or higher.

For now, set the temperature to `1.0`.

## Max tokens

`quarkus.langchain4j.openai.chat-model.max-tokens` limits the length of the  response.

Try adding

```properties
quarkus.langchain4j.openai.chat-model.max-tokens=20
```

to `src/main/resources/application.properties` and see how the model cuts off the response after 20 tokens.

Tokens are not words, but rather the smallest units of text that the model can generate.
For example, "Hello, world!" has 3 tokens: "Hello", ",", and "world".
Each model has a different tokenization scheme, so the number of tokens in a sentence can vary between models.

For now, set the max tokens to `1000`.

## Frequency penalty

`quarkus.langchain4j.openai.chat-model.frequency-penalty` defines how much the model should avoid repeating itself.

Try adding

```properties
quarkus.langchain4j.openai.chat-model.frequency-penalty=2
```

to `src/main/resources/application.properties` then ask

```
Repeat the word hedgehog 50 times
```

The model will most likely start producing garbage after repeating the word a few times.

Change the value to `0` and you will likely see the model repeat the word 50 times.

!!! info
    The maximum penalty for OpenAI models is `2`.

## Final configuration

==After playing with the configuration, you can set it to the following values:==

```properties title="application.properties"
--8<-- "../../section-1/step-02/src/main/resources/application.properties"
```

Let's now switch to the [next step](./step-03.md)!
