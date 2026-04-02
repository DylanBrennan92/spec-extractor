package com.originspecs.specextractor.processor;

import com.originspecs.specextractor.config.Constants;
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
    void shouldReplaceMistranslations_whenCommonNameMatchesCorrection() {
        assertCorrected("I'm Ease", "Mira e:S");
        assertCorrected("Here and there", "Mira Tocot");
        assertCorrected("Vroom", "Boon");
        assertCorrected("Extrail", "X-Trail");
        assertCorrected("Randy", "Landy");
        assertCorrected("Insights", "Insight");
    }

    @Test
    void shouldStripFootnoteMarkers_whenCommonNameHasTrailingAsterisk() {
        assertCorrected("86 *2", "86");
        assertCorrected("Passo *", "Passo");
        assertCorrected("Rise *", "Rise");
    }

    @Test
    void shouldApplyBothFootnoteStripAndMistranslationCorrection_whenCommonNameHasBoth() {
        assertCorrected("Insights *", "Insight");
    }

    @Test
    void shouldPreserveValue_whenNoCorrectionExists() {
        assertCorrected("RX450h", "RX450h");
        assertCorrected("Note", "Note");
        assertCorrected("Corolla", "Corolla");
    }

    @Test
    void shouldLeaveRecordUnchanged_whenCommonNameHeaderAbsent() {
        SpecRecord record = new SpecRecord(Map.of("Car Name", "Toyota", "Engine", "2ZR"));
        List<SpecRecord> result = corrector.correct(List.of(record));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get(Constants.COMMON_NAME_HEADER)).isEmpty();
        assertThat(result.get(0).get("Car Name")).isEqualTo("Toyota");
    }

    @Test
    void shouldLeaveRecordUnchanged_whenCommonNameEmpty() {
        SpecRecord record = recordWithCommonName("");
        List<SpecRecord> result = corrector.correct(List.of(record));
        assertThat(result.get(0).get(Constants.COMMON_NAME_HEADER)).isEmpty();
    }

    @Test
    void shouldPreserveSourceArtifactId_whenCommonNameCorrected() {
        String id = "550e8400-e29b-41d4-a716-446655440000";
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Car Name", "Brand");
        fields.put(Constants.COMMON_NAME_HEADER, "I'm Ease");
        fields.put("Engine", "KF");
        SpecRecord record = new SpecRecord(fields, id);

        List<SpecRecord> result = corrector.correct(List.of(record));

        assertThat(result.get(0).sourceArtifactId()).isEqualTo(id);
        assertThat(result.get(0).get(Constants.COMMON_NAME_HEADER)).isEqualTo("Mira e:S");
    }

    private void assertCorrected(String input, String expected) {
        SpecRecord record = recordWithCommonName(input);
        List<SpecRecord> result = corrector.correct(List.of(record));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get(Constants.COMMON_NAME_HEADER)).isEqualTo(expected);
    }

    private static SpecRecord recordWithCommonName(String value) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Car Name", "Brand");
        fields.put(Constants.COMMON_NAME_HEADER, value);
        fields.put("Engine", "KF");
        return new SpecRecord(fields);
    }
}
