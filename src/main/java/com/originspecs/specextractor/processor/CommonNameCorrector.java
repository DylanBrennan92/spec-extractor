package com.originspecs.specextractor.processor;

import com.originspecs.specextractor.config.Constants;
import com.originspecs.specextractor.model.SpecRecord;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Post-processes SpecRecords to correct known DeepL mistranslations of Common Name
 * and strip footnote markers (e.g. "86 *2" → "86").
 * Corrections are loaded from {@code common-name-corrections.properties}.
 */
@Slf4j
public final class CommonNameCorrector implements SpecRecordPostProcessor {

    private static final String CORRECTIONS_RESOURCE = "common-name-corrections.properties";

    private final Map<String, String> mistranslations;

    /**
     * Creates a CommonNameCorrector by loading corrections from the classpath.
     * Use this instead of the constructor to avoid partially initialized objects if loading fails.
     *
     * @throws CommonNameCorrectionsLoadException if the properties file exists but cannot be read
     */
    public static CommonNameCorrector create() {
        return new CommonNameCorrector(loadMistranslations());
    }

    /** Constructor for tests — inject custom corrections. */
    CommonNameCorrector(Map<String, String> mistranslations) {
        this.mistranslations = mistranslations != null ? mistranslations : Map.of();
    }

    private static Map<String, String> loadMistranslations() {
        try (InputStream is = CommonNameCorrector.class.getResourceAsStream("/" + CORRECTIONS_RESOURCE)) {
            if (is == null) {
                if (log.isWarnEnabled()) {
                    log.warn("{} not found — no Common Name corrections applied", CORRECTIONS_RESOURCE);
                }
                return Map.of();
            }
            Properties props = new Properties();
            props.load(is);
            Map<String, String> result = new LinkedHashMap<>();
            props.forEach((k, v) -> result.put(String.valueOf(k), String.valueOf(v)));
            return Map.copyOf(result);
        } catch (IOException e) {
            throw new CommonNameCorrectionsLoadException(
                    "Failed to load " + CORRECTIONS_RESOURCE + ": " + e.getMessage(), e);
        }
    }

    /**
     * Applies corrections to all records. Returns a new list; input records are not mutated.
     */
    public List<SpecRecord> correct(List<SpecRecord> records) {
        return records.stream()
                .map(this::correctRecord)
                .toList();
    }

    @Override
    public List<SpecRecord> process(List<SpecRecord> records) {
        return correct(records);
    }

    private SpecRecord correctRecord(SpecRecord record) {
        String commonName = record.get(Constants.COMMON_NAME_HEADER);
        if (commonName.isEmpty()) {
            return record;
        }

        String corrected = correctCommonName(commonName);
        if (corrected.equals(commonName)) {
            return record;
        }

        if (log.isDebugEnabled()) {
            log.debug("Common Name correction: '{}' → '{}'", commonName, corrected);
        }

        Map<String, String> newFields = new LinkedHashMap<>(record.fields());
        newFields.put(Constants.COMMON_NAME_HEADER, corrected);
        return new SpecRecord(newFields, record.sourceArtifactId());
    }

    private String correctCommonName(String value) {
        String stripped = stripFootnoteMarkers(value);
        return mistranslations.getOrDefault(stripped, stripped);
    }

    /** Removes trailing footnote markers like " *", " *2", " *3". */
    private static String stripFootnoteMarkers(String value) {
        if (value == null) return "";
        return value.replaceFirst("\\s*\\*\\d*\\s*$", "").trim();
    }
}
