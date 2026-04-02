package com.originspecs.specextractor.model;

import com.originspecs.specextractor.config.Constants;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable domain object representing one extracted vehicle specification record.
 * Keys are column header names; values are the corresponding cell values.
 *
 * <p>Insertion order is preserved so that JSON output columns appear in the
 * same order as the spreadsheet headers. Jackson shape is defined via {@code SpecRecordMixin}.
 *
 * <p>Avoid using {@link Constants#SOURCE_ARTIFACT_ID_JSON_KEY} as a column header — it is reserved for lineage JSON.
 */
public record SpecRecord(Map<String, String> fields, String sourceArtifactId) {

    /** Row with no ministry workbook lineage (Provenance field omitted from JSON). */
    public SpecRecord(Map<String, String> fields) {
        this(fields, null);
    }

    public SpecRecord {
        Objects.requireNonNull(fields, "fields");
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        sourceArtifactId = sourceArtifactId == null || sourceArtifactId.isBlank()
                ? null
                : sourceArtifactId.trim();
        if (sourceArtifactId != null) {
            SourceArtifactId.parse(sourceArtifactId);
        }
    }

    /** Returns the value for the given header, or an empty string if absent. */
    public String get(String header) {
        return fields.getOrDefault(header, "");
    }

    /** Returns the value for the given header as Optional; empty if absent or blank. */
    public Optional<String> getOptional(String header) {
        String value = fields.get(header);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }
}
