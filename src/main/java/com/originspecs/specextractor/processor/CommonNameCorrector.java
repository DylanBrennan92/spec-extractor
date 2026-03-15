package com.originspecs.specextractor.processor;

import com.originspecs.specextractor.model.SpecRecord;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Post-processes SpecRecords to correct known DeepL mistranslations of Common Name
 * and strip footnote markers (e.g. "86 *2" → "86").
 */
@Slf4j
public class CommonNameCorrector {

    private static final String COMMON_NAME_HEADER = "Common Name";

    /** Known mistranslations: DeepL output → correct model name. */
    private static final Map<String, String> MISTRANSLATIONS = Map.ofEntries(
            Map.entry("I'm Ease", "Mira e:S"),
            Map.entry("Here and there", "Mira Tocot"),
            Map.entry("Vroom", "Boon"),
            Map.entry("Extrail", "X-Trail"),
            Map.entry("Randy", "Landy"),
            Map.entry("Insights", "Insight")
    );

    /**
     * Applies corrections to all records. Returns a new list; input records are not mutated.
     */
    public List<SpecRecord> correct(List<SpecRecord> records) {
        return records.stream()
                .map(this::correctRecord)
                .toList();
    }

    private SpecRecord correctRecord(SpecRecord record) {
        String commonName = record.get(COMMON_NAME_HEADER);
        if (commonName == null || commonName.isEmpty()) {
            return record;
        }

        String corrected = correctCommonName(commonName);
        if (corrected.equals(commonName)) {
            return record;
        }

        log.debug("Common Name correction: '{}' → '{}'", commonName, corrected);

        Map<String, String> newFields = new LinkedHashMap<>(record.fields());
        newFields.put(COMMON_NAME_HEADER, corrected);
        return new SpecRecord(newFields);
    }

    private String correctCommonName(String value) {
        String stripped = stripFootnoteMarkers(value);
        return MISTRANSLATIONS.getOrDefault(stripped, stripped);
    }

    /** Removes trailing footnote markers like " *", " *2", " *3". */
    private static String stripFootnoteMarkers(String value) {
        if (value == null) return "";
        return value.replaceFirst("\\s*\\*\\d*\\s*$", "").trim();
    }
}
