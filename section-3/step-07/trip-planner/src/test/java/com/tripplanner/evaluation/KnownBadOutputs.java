package com.tripplanner.evaluation;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Loads the deliberately invalid plan texts from
 * {@code src/test/resources/evaluation/known-bad.yaml}. Resolved relative to
 * the module working directory, the same way the sample loader does.
 */
public final class KnownBadOutputs {

    private static final String PATH = "src/test/resources/evaluation/known-bad.yaml";

    private KnownBadOutputs() {
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> load() {
        Yaml yaml = new Yaml(new LoaderOptions());
        try (InputStream in = new FileInputStream(PATH)) {
            return yaml.loadAs(in, List.class);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load " + PATH, e);
        }
    }
}
