package com.originspecs.specextractor.config;

import java.nio.file.Files;

/** Validates {@link Config} against the environment (input path must exist and be a regular file). */
public final class ConfigValidator {

    private ConfigValidator() {}

    public static void validate(Config config) {
        var inputFile = config.inputFile();
        if (!Files.exists(inputFile)) {
            throw new IllegalArgumentException("Input file does not exist: " + inputFile.toAbsolutePath());
        }
        if (!Files.isRegularFile(inputFile)) {
            throw new IllegalArgumentException("Input path is not a regular file: " + inputFile.toAbsolutePath());
        }
    }
}
