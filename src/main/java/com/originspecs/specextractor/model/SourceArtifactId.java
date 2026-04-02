package com.originspecs.specextractor.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Canonical ministry workbook lineage id (UUID string), aligned with DataPrep {@code --source-artifact-id}.
 */
public record SourceArtifactId(String value) {

    public SourceArtifactId {
        Objects.requireNonNull(value, "value");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("sourceArtifactId must not be blank");
        }
        UUID.fromString(value);
    }

    public static SourceArtifactId parse(String raw) {
        return new SourceArtifactId(raw);
    }
}
