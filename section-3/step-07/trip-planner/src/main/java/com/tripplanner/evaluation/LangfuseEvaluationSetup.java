package com.tripplanner.evaluation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.langfuse.api.LangfuseApiException;
import com.langfuse.api.model.BooleanEvaluationRuleFilter1;
import com.langfuse.api.model.CreateDatasetItemRequest;
import com.langfuse.api.model.CreateDatasetRequest;
import com.langfuse.api.model.CreateEvaluationRuleRequest;
import com.langfuse.api.model.CreateLlmAsJudgeEvaluatorRequest1;
import com.langfuse.api.model.CreateScoreConfigRequest;
import com.langfuse.api.model.EvaluationRuleBooleanFilterOperator;
import com.langfuse.api.model.EvaluationRuleEvaluatorAssignmentInput;
import com.langfuse.api.model.EvaluationRuleFilter;
import com.langfuse.api.model.EvaluationRuleOptionsFilterOperator;
import com.langfuse.api.model.EvaluatorChatPromptInput;
import com.langfuse.api.model.EvaluatorModelConfig;
import com.langfuse.api.model.EvaluatorOutputDefinition;
import com.langfuse.api.model.LlmAdapter;
import com.langfuse.api.model.PromptVariableMappingInput;
import com.langfuse.api.model.PromptVariableMappingSource;
import com.langfuse.api.model.PublicEvaluatorNumericScore1;
import com.langfuse.api.model.ScoreConfigDataType;
import com.langfuse.api.model.StringOptionsEvaluationRuleFilter1;
import com.langfuse.api.model.UpsertLlmConnectionRequest;
import io.quarkiverse.langfuse.api.LangfuseOperations;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Sets up the trip plan judge inside Langfuse when dev mode starts. Every step
 * looks up or upserts by name, so a restart leaves one of each. A failing step
 * logs a warning and never stops the application from starting.
 */
@Singleton
public class LangfuseEvaluationSetup {

    static final String DATASET = "trip-plan-samples";
    // Langfuse names each score after the evaluator that produced it.
    static final String SCORE = "plan-quality";
    static final String PROVIDER = "openai";

    @ConfigProperty(name = "trip.evaluation.langfuse-setup.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "quarkus.langchain4j.openai.api-key")
    String openAiKey;

    @ConfigProperty(name = "quarkus.langchain4j.openai.judgeModel.chat-model.model-name")
    String judgeModel;

    @Inject
    LangfuseOperations langfuse;

    void onStart(@Observes StartupEvent event) {
        if (!enabled) {
            return;
        }
        step("LLM connection", this::createLlmConnection);
        step("score config", this::createScoreConfig);
        String datasetId = step("dataset", this::createDataset);
        String evaluatorId = step("evaluator", this::createEvaluator);
        if (datasetId != null && evaluatorId != null) {
            step("evaluation rule", () -> createEvaluationRule(evaluatorId, datasetId));
        }
    }

    String createLlmConnection() {
        return langfuse.llmConnections().upsert(UpsertLlmConnectionRequest.builder()
                .provider(PROVIDER)
                .adapter(LlmAdapter.OPENAI)
                .secretKey(openAiKey)
                .customModels(List.of(judgeModel))
                .build()).getId();
    }

    String createScoreConfig() {
        return langfuse.scoreConfigs().createIfAbsent(CreateScoreConfigRequest.builder()
                .name(SCORE)
                .dataType(ScoreConfigDataType.NUMERIC)
                .minValue(0.0)
                .maxValue(1.0)
                .description("How well a trip plan meets the expected output of its sample. 1 = every requirement met.")
                .build()).getId();
    }

    String createDataset() {
        String datasetId = langfuse.datasets().createIfAbsent(CreateDatasetRequest.builder()
                .name(DATASET)
                .description("Trip requests from evaluation/samples.yaml with the plan each one should produce")
                .build()).getId();
        // The sample name is the item id, so a restart updates the items in place.
        for (Sample sample : readSamples()) {
            langfuse.datasetItems().create(CreateDatasetItemRequest.builder()
                    .datasetName(DATASET)
                    .id(sample.name())
                    .input(sample.input())
                    .expectedOutput(sample.expectedOutput())
                    .build());
        }
        return datasetId;
    }

    String createEvaluator() {
        return langfuse.evaluators().createIfAbsent(CreateLlmAsJudgeEvaluatorRequest1.builder()
                .type(CreateLlmAsJudgeEvaluatorRequest1.TypeEnum.LLM_AS_JUDGE)
                .name(SCORE)
                .description("Trip plan quality: does the plan meet the expected output of its sample?")
                .prompt(new EvaluatorChatPromptInput(readResource("evaluation/rubric.txt")))
                .modelConfig(EvaluatorModelConfig.builder().provider(PROVIDER).model(judgeModel).build())
                .variableMapping(variableMapping())
                .outputDefinition(new EvaluatorOutputDefinition(PublicEvaluatorNumericScore1.builder()
                        .dataType(PublicEvaluatorNumericScore1.DataTypeEnum.NUMERIC)
                        .minValue(0.0)
                        .maxValue(1.0)
                        .scoreValueInstructions("A number from 0 to 1, where 1 means the plan meets every requirement in the expected output.")
                        .scoreReasoningInstructions("One sentence that names any requirement the plan misses.")
                        .build()))
                .build()).getLlmAsJudgeEvaluator1().getId();
    }

    // Judge the root span of every experiment item that runs against the samples dataset.
    String createEvaluationRule(String evaluatorId, String datasetId) {
        return langfuse.evaluationRules().createIfAbsent(CreateEvaluationRuleRequest.builder()
                .name(DATASET + " experiments")
                .enabled(true)
                .sampling(1.0)
                .filter(List.of(
                        new EvaluationRuleFilter(BooleanEvaluationRuleFilter1.builder()
                                .type(BooleanEvaluationRuleFilter1.TypeEnum.BOOLEAN)
                                .column("isExperimentItemRootSpan")
                                .operator(EvaluationRuleBooleanFilterOperator.EQUAL)
                                .value(true)
                                .build()),
                        new EvaluationRuleFilter(StringOptionsEvaluationRuleFilter1.builder()
                                .type(StringOptionsEvaluationRuleFilter1.TypeEnum.STRING_OPTIONS)
                                .column("datasetId")
                                .operator(EvaluationRuleOptionsFilterOperator.ANY_OF)
                                .value(List.of(datasetId))
                                .build())))
                .evaluatorAssignments(List.of(EvaluationRuleEvaluatorAssignmentInput.builder()
                        .evaluatorId(evaluatorId)
                        .variableMapping(variableMapping())
                        .build()))
                .build()).getId();
    }

    // {{input}} and {{output}} come from the span, {{ground_truth}} from the dataset item.
    static List<PromptVariableMappingInput> variableMapping() {
        return List.of(
                mapping("input", PromptVariableMappingSource.INPUT),
                mapping("output", PromptVariableMappingSource.OUTPUT),
                mapping("ground_truth", PromptVariableMappingSource.EXPECTED_OUTPUT));
    }

    static PromptVariableMappingInput mapping(String variable, PromptVariableMappingSource source) {
        return PromptVariableMappingInput.builder().variable(variable).source(source).build();
    }

    static List<Sample> readSamples() {
        try (InputStream yaml = resource("evaluation/samples.yaml")) {
            return new ObjectMapper(new YAMLFactory()).readValue(yaml, new TypeReference<List<Sample>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String readResource(String path) {
        try (InputStream in = resource(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static InputStream resource(String path) throws IOException {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IOException(path + " not found on the classpath");
        }
        return in;
    }

    static <T> T step(String name, Supplier<T> action) {
        try {
            T id = action.get();
            Log.infof("Langfuse evaluation setup: %s ready (%s)", name, id);
            return id;
        } catch (LangfuseApiException e) {
            Log.warnf("Langfuse evaluation setup: %s failed (HTTP %d): %s", name, e.getStatusCode(), e.getServerMessage());
        } catch (RuntimeException e) {
            Log.warnf("Langfuse evaluation setup: %s failed: %s", name, e.getMessage());
        }
        return null;
    }

    /** One entry in samples.yaml. The parameters follow the planTrip argument order. */
    record Sample(String name, List<String> parameters,
                  @JsonProperty("expected-output") String expectedOutput, List<String> tags) {

        Map<String, String> input() {
            String[] keys = {"destination", "startDate", "days", "tripType", "travelers", "budget", "preferences"};
            Map<String, String> input = new LinkedHashMap<>();
            for (int i = 0; i < keys.length; i++) {
                input.put(keys[i], parameters.get(i));
            }
            return input;
        }
    }
}
