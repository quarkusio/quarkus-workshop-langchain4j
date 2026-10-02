package com.tripplanner.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.langfuse.api.model.Dataset;
import com.langfuse.api.model.DatasetItem;
import io.quarkiverse.langchain4j.testing.evaluation.EvaluationSample;
import io.quarkiverse.langchain4j.testing.evaluation.Parameters;
import io.quarkiverse.langfuse.api.LangfuseOperations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads evaluation samples from the Langfuse dataset that dev mode seeds from
 * {@code samples.yaml}.
 *
 * <p>The live IT calls this class directly and passes in its injected
 * {@link LangfuseOperations}. Arc removes a bean that is only looked up
 * through {@code CDI.current().select()}, so the IT holds the injected
 * reference.
 */
public class LangfuseDatasetSampleLoader {

    /** The dataset item input keys, in {@code planTrip} parameter order. */
    static final List<String> INPUT_KEYS =
            List.of("destination", "startDate", "days", "tripType", "travelers", "budget", "preferences");

    private static final ObjectMapper JSON = new ObjectMapper();

    /** A dataset item together with the ids an experiment span has to carry. */
    public record DatasetSample(String datasetId, String itemId, String inputJson, EvaluationSample<String> sample) {
    }

    private final LangfuseOperations langfuse;

    public LangfuseDatasetSampleLoader(LangfuseOperations langfuse) {
        this.langfuse = langfuse;
    }

    public Optional<String> datasetId(String datasetName) {
        return langfuse.datasets().findByName(datasetName).map(Dataset::getId);
    }

    public DatasetSample load(String itemId) {
        DatasetItem item = langfuse.datasetItems().findById(itemId)
                .orElseThrow(() -> new IllegalStateException("no dataset item with id " + itemId));
        // Dev mode stores the input as a JSON object keyed by parameter name.
        Map<?, ?> input = JSON.convertValue(item.getInput(), Map.class);

        Parameters parameters = new Parameters();
        INPUT_KEYS.forEach(key -> parameters.add(key, String.valueOf(input.get(key))));

        EvaluationSample<String> sample = EvaluationSample.<String>builder()
                .withName(item.getId())
                .withParameters(parameters)
                .withExpectedOutput(String.valueOf(item.getExpectedOutput()))
                .build();
        return new DatasetSample(item.getDatasetId(), item.getId(), toJson(input), sample);
    }

    private static String toJson(Map<?, ?> input) {
        try {
            return JSON.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
