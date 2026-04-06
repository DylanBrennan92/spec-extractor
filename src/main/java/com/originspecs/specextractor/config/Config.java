package com.originspecs.specextractor.config;

import com.originspecs.specextractor.model.SourceArtifactId;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable paths and options for one extraction run.
 * Built via {@link ConfigParser#parse(String[])}, validated with {@link ConfigValidator#validate(Config)},
 * then optionally enriched with {@link DataPrepLineageResolver#applySidecarLineage(Config)}.
 *
 * @param sourceArtifactId {@code null} when neither {@code --source-artifact-id} nor a DataPrep sidecar supplied lineage.
 */
public record Config(
        Path inputFile,
        Path outputFile,
        String deeplApiKey,
        SourceArtifactId sourceArtifactId
) {

    public Config {
        Objects.requireNonNull(inputFile, "inputFile");
        Objects.requireNonNull(outputFile, "outputFile");
        Objects.requireNonNull(deeplApiKey, "deeplApiKey");
    }

    /** Absent when no {@code --source-artifact-id} was supplied (preferred over nullable accessor at call sites). */
    public Optional<SourceArtifactId> sourceArtifactLineage() {
        return Optional.ofNullable(sourceArtifactId);
    }
}
