package com.originspecs.specextractor.config;

import com.originspecs.specextractor.model.SourceArtifactId;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolves ministry workbook lineage from DataPrep's UTF-8 sidecar next to the cleaned input workbook.
 */
@Slf4j
public final class DataPrepLineageResolver {

    private DataPrepLineageResolver() {}

    /**
     * If {@code config} has no {@link SourceArtifactId} and a DataPrep sidecar exists next to the input file,
     * returns a new config with lineage parsed from the sidecar. The first line that parses as a UUID is used;
     * leading blank lines and non-UUID lines (e.g. comments) are skipped. If the CLI already supplied an id,
     * checks the sidecar matches when present. If no sidecar exists, returns {@code config} unchanged.
     */
    public static Config applySidecarLineage(Config config) {
        Path sidecar = DataPrepLineageSidecar.pathForInputWorkbook(config.inputFile());
        if (!Files.isRegularFile(sidecar)) {
            return config;
        }

        String raw;
        try {
            raw = Files.readString(sidecar, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read DataPrep lineage sidecar: " + sidecar.toAbsolutePath(), e);
        }

        Optional<SourceArtifactId> fromSidecar = firstUuidInSidecar(raw);
        if (fromSidecar.isEmpty()) {
            log.warn("DataPrep lineage sidecar has no valid UUID line — ignoring: {}", sidecar.toAbsolutePath());
            return config;
        }

        SourceArtifactId idFromFile = fromSidecar.get();
        SourceArtifactId fromCli = config.sourceArtifactId();
        if (fromCli != null) {
            if (!fromCli.value().equals(idFromFile.value())) {
                throw new IllegalArgumentException(
                        "CLI " + Constants.CLI_SOURCE_ARTIFACT_ID_FLAG + " (" + fromCli.value()
                                + ") does not match DataPrep sidecar (" + idFromFile.value() + ") at "
                                + sidecar.toAbsolutePath());
            }
            return config;
        }

        if (log.isInfoEnabled()) {
            log.info("Loaded sourceArtifactId from DataPrep sidecar: {}", sidecar.toAbsolutePath());
        }
        return new Config(config.inputFile(), config.outputFile(), config.deeplApiKey(), idFromFile);
    }

    private static Optional<SourceArtifactId> firstUuidInSidecar(String utf8Content) {
        for (String line : utf8Content.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                return Optional.of(SourceArtifactId.parse(trimmed));
            } catch (IllegalArgumentException ignored) {
                // skip comment or other non-UUID lines
            }
        }
        return Optional.empty();
    }
}
