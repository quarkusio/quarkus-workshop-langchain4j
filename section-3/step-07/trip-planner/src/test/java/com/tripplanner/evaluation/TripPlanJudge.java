package com.tripplanner.evaluation;

import dev.langchain4j.model.chat.ChatModel;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationResult;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationSample;
import io.quarkiverse.langchain4j.testing.evaluation.judge.AiJudgeStrategy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Thin wrapper over {@link AiJudgeStrategy} that loads the rubric from
 * {@code src/test/resources/evaluation/rubric.txt} and records which judge
 * model was used, so judge usage is separable from application measurements.
 *
 * <p>The rubric must ask for a literal {@code true}/{@code false} answer:
 * {@link AiJudgeStrategy} parses the verdict with
 * {@code Boolean.parseBoolean}, so any other shape (JSON, prose) is a failing
 * judgment.
 */
public class TripPlanJudge {

    private static final Path RUBRIC = Path.of("src/test/resources/evaluation/rubric.txt");

    private final ChatModel judgeModel;
    private final AiJudgeStrategy strategy;

    public TripPlanJudge(ChatModel judgeModel) {
        this(judgeModel, loadRubric());
    }

    public TripPlanJudge(ChatModel judgeModel, String rubric) {
        this.judgeModel = judgeModel;
        this.strategy = new AiJudgeStrategy(judgeModel, rubric);
    }

    public EvaluationResult judge(EvaluationSample<String> sample, String actual) {
        EvaluationResult result = strategy.evaluate(sample, actual);
        // The judge-model metadata is the class of the model used, which is
        // stable and does not depend on the model's self-reported name (which
        // the ChatModel interface does not expose in this stack).
        return result.withMetadata(Map.of("judge-model", judgeModel.getClass().getSimpleName()));
    }

    private static String loadRubric() {
        try {
            return Files.readString(RUBRIC);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load judge rubric from " + RUBRIC, e);
        }
    }
}
