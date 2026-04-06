package com.originspecs.specextractor.config;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validates {@link Config} against the filesystem before a run: the input workbook path must exist and be a regular file.
 * <p>
 * DataPrep lineage from a sidecar file is applied separately by {@link DataPrepLineageResolver#applySidecarLineage(Config)}
 * after validation.
 */
public final class ConfigValidator {

    private ConfigValidator() {}

    public static void validate(Config config) {
        Path inputFile = config.inputFile();
        if (!Files.exists(inputFile)) {
            throw new IllegalArgumentException("Input file does not exist: " + inputFile.toAbsolutePath());
        }
        if (!Files.isRegularFile(inputFile)) {
            throw new IllegalArgumentException("Input path is not a regular file: " + inputFile.toAbsolutePath());
        }
    }
}
