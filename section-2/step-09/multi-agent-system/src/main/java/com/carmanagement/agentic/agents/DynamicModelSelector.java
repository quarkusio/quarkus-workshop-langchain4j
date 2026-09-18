package com.carmanagement.agentic.agents;

import dev.langchain4j.model.chat.ChatModel;
import io.quarkiverse.langchain4j.ModelName;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class DynamicModelSelector {

    private static final int HIGH_VALUE_THRESHOLD = 30000;

    @Inject
    ChatModel baseModel;

    @Inject
    @ModelName("advancedModel")
    ChatModel advancedModel;

    public ChatModel select(String carValue) {
        if (parseValue(carValue) > HIGH_VALUE_THRESHOLD) {
            Log.info("Selecting advanced model for high-value car estimated " + carValue);
            return advancedModel;
        }
        return baseModel;
    }

    static int parseValue(String carValue) {
        if (carValue == null) {
            return 0;
        }
        String digits = carValue.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(digits);
    }
}
