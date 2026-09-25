package com.tripplanner.agentic.agents;

import com.tripplanner.model.VehicleEvaluation;
import dev.langchain4j.model.chat.ChatModel;
import io.quarkiverse.langchain4j.ModelName;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class DynamicModelSelector {

    private static final double ENHANCED_MODEL_THRESHOLD = 6.0;

    @Inject
    ChatModel baseModel;

    @Inject
    @ModelName("enhancedModel")
    ChatModel enhancedModel;

    public ChatModel select(VehicleEvaluation evaluation) {
        if (evaluation != null && evaluation.score() > ENHANCED_MODEL_THRESHOLD) {
            Log.infof("Score %.1f > %.1f — switching to enhanced model for final refinement",
                    evaluation.score(), ENHANCED_MODEL_THRESHOLD);
            return enhancedModel;
        }
        return baseModel;
    }
}
