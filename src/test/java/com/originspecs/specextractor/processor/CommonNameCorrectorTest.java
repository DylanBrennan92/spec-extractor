package com.originspecs.specextractor.processor;

import com.originspecs.specextractor.model.SpecRecord;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CommonNameCorrectorTest {

    private static final Map<String, String> TEST_MISTRANSLATIONS = Map.ofEntries(
            Map.entry("I'm Ease", "Mira e:S"),
            Map.entry("Here and there", "Mira Tocot"),
            Map.entry("Vroom", "Boon"),
            Map.entry("Extrail", "X-Trail"),
            Map.entry("Randy", "Landy"),
            Map.entry("Insights", "Insight")
    );

    private final CommonNameCorrector corrector = new CommonNameCorrector(TEST_MISTRANSLATIONS);

    @Test
    void correct_mistranslations_replaced() {
        assertCorrected("I'm Ease", "Mira e:S");
        assertCorrected("Here and there", "Mira Tocot");
        assertCorrected("Vroom", "Boon");
        assertCorrected("Extrail", "X-Trail");
        assertCorrected("Randy", "Landy");
        assertCorrected("Insights", "Insight");
    }

    @Test
    void correct_footnoteMarkers_stripped() {
        assertCorrected("86 *2", "86");
        assertCorrected("Passo *", "Passo");
        assertCorrected("Rise *", "Rise");
    }

    @Test
    void correct_footnoteThenMistranslation_bothApplied() {
        assertCorrected("Insights *", "Insight");
    }

    @Test
    void correct_unchangedValues_preserved() {
        assertCorrected("RX450h", "RX450h");
        assertCorrected("Note", "Note");
        assertCorrected("Corolla", "Corolla");
    }

    @Test
    void correct_noCommonName_unchanged() {
        SpecRecord record = new SpecRecord(Map.of("Car Name", "Toyota", "Engine", "2ZR"));
        List<SpecRecord> result = corrector.correct(List.of(record));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("Common Name")).isEmpty();
        assertThat(result.get(0).get("Car Name")).isEqualTo("Toyota");
    }

    @Test
    void correct_emptyCommonName_unchanged() {
        SpecRecord record = recordWithCommonName("");
        List<SpecRecord> result = corrector.correct(List.of(record));
        assertThat(result.get(0).get("Common Name")).isEmpty();
    }

    @Test
    void correct_defaultConstructor_loadsFromPropertiesFile() {
        CommonNameCorrector fileBasedCorrector = new CommonNameCorrector();
        SpecRecord record = recordWithCommonName("Extrail");
        List<SpecRecord> result = fileBasedCorrector.correct(List.of(record));
        assertThat(result.get(0).get("Common Name")).isEqualTo("X-Trail");
    }

    private void assertCorrected(String input, String expected) {
        SpecRecord record = recordWithCommonName(input);
        List<SpecRecord> result = corrector.correct(List.of(record));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("Common Name")).isEqualTo(expected);
    }

    private static SpecRecord recordWithCommonName(String value) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Car Name", "Brand");
        fields.put("Common Name", value);
        fields.put("Engine", "KF");
        return new SpecRecord(fields);
    }
}
