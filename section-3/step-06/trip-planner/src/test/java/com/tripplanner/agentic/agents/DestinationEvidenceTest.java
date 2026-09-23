package com.tripplanner.agentic.agents;

import com.tripplanner.agentic.workflow.DestinationEvidence;
import com.tripplanner.agentic.workflow.DestinationIntelligence;
import com.tripplanner.model.TripError;
import com.tripplanner.model.TripIntelligenceException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DestinationEvidenceTest {
    private static final String WEATHER = """
            {"destination":"Rome","summary":"Workshop fixture","conditions":"Snow",
             "avgTemperatureCelsius":-2,"warnings":["Use indoor alternatives"]}
            """;
    private static final String POI = """
            {"entries":[{"destination":"Rome","name":"Workshop museum","category":"museum",
              "description":"Fictional indoor exhibit","rating":4.0}]}
            """;

    @Test
    void acceptsValidDataAndEmptyCatalogWithoutChangingEvidence() {
        assertDoesNotThrow(() -> DestinationEvidence.validate("Rome", WEATHER, POI));
        assertDoesNotThrow(() -> DestinationEvidence.validate("Rome", WEATHER, "{\"entries\":[]}"));
        String output = DestinationIntelligence.output("Rome", WEATHER, POI);
        assertTrue(output.contains("Use indoor alternatives"));
        assertTrue(output.contains("Fictional indoor exhibit"));
    }

    @Test
    void rejectsMissingMalformedAndWrongDestinationEvidence() {
        for (String bad : new String[] {null, "", "timeout", "{}", "null", "[]",
                WEATHER.replace("Rome", "Paris"), WEATHER.replace("-2", "null"),
                WEATHER.replace("Snow", ""), WEATHER.replace("[\"Use indoor alternatives\"]", "null"),
                WEATHER + " {}"}) {
            assertThrows(TripIntelligenceException.class, () -> DestinationEvidence.validate("Rome", bad, POI));
        }
        for (String bad : new String[] {null, "", "{}", "null", "[{}]", POI.replace("Rome", "Paris"),
                POI.replace("4.0", "8.0"), POI.replace("Workshop museum", "")}) {
            assertThrows(TripIntelligenceException.class, () -> DestinationEvidence.validate("Rome", WEATHER, bad));
        }
    }

    @Test
    void remoteFailureHasSafeStatusAndMessage() {
        var error = TripError.from(new IllegalStateException(new TripIntelligenceException()), false);
        assertEquals("intelligence_unavailable", error.error());
        assertEquals(502, error.httpStatus());
        assertFalse(error.message().contains("IllegalStateException"));
    }
}
