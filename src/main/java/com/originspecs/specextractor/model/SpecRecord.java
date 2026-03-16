package com.originspecs.specextractor.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable domain object representing one extracted vehicle specification record.
 * Keys are column header names; values are the corresponding cell values.
 *
 * <p>Insertion order is preserved so that JSON output columns appear in the
 * same order as the spreadsheet headers. {@code @JsonValue} instructs Jackson
 * to serialize the record directly as its fields map rather than as a wrapper object.
 */
public record SpecRecord(Map<String, String> fields) {

    public SpecRecord(Map<String, String> fields) {
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
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

    @JsonValue
    public Map<String, String> fields() {
        return fields;
    }
}
