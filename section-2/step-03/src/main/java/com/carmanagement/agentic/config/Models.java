package com.carmanagement.agentic.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

/**
 * Configuration for language models used by the agentic system.
 */
@ApplicationScoped
public class Models {

    @ConfigProperty(name = "ollama.model.name", defaultValue = "gpt-oss:20b")
    String ollamaModelName;

    @ConfigProperty(name = "model.temperature",  defaultValue = "0")
    double modelTemperature;

    @ConfigProperty(name = "ollama.url",  defaultValue = "http://127.0.0.1:11434")
    String ollamaUrl;

    private enum MODEL_PROVIDER {
        OPENAI,
        OLLAMA
    }

    // Set the model provider here
    private static final MODEL_PROVIDER modelProvider = MODEL_PROVIDER.OPENAI;

    /**
     * Provides the base chat language model for the application.
     */
    public ChatModel baseModel() {
        return switch (modelProvider) {
            case OPENAI -> createOpenAiModel();
            case OLLAMA -> createOllamaModel();
        };
    }
    
    private ChatModel createOpenAiModel() {
        return OpenAiChatModel.builder()
                .baseUrl(System.getenv("OPENAI_BASE_URL"))
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .organizationId(System.getenv("OPENAI_ORGANIZATION_ID"))
                .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                .temperature(modelTemperature)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
    
    private ChatModel createOllamaModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaUrl)
                .modelName(ollamaModelName)
                .timeout(Duration.ofMinutes(5))
                .temperature(modelTemperature)
                .logRequests(true)
                .logResponses(true)
                .think(false)
                .build();
    }
}


