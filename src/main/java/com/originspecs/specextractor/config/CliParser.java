package com.originspecs.specextractor.config;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CliParser {

    private static final String USAGE = String.join(System.lineSeparator(),
            "Usage: java -jar spec-extractor.jar [--source-artifact-id <uuid>] <inputFile.xls>",
            "Output is written automatically to src/main/resources/local-data/output/ as {timestamp}_{input_base_name}.json.",
            "Lineage: If <inputFile>.source-artifact-id exists (DataPrep output), the first line that parses as a UUID is used",
            "unless you pass --source-artifact-id (then it must match the sidecar when both are present).",
            "Original ministry workbook: local-data/artifacts/<uuid>.<ext> (DataPrep).",
            "Example: java -jar target/spec-extractor.jar src/main/resources/local-data/input/cleaned.xls",
            "Example: java -jar target/spec-extractor.jar --source-artifact-id 550e8400-e29b-41d4-a716-446655440000 path/to/file.xls");

    private CliParser() {}

    /**
     * Parses CLI arguments into a validated Config, or throws {@link CliException} after
     * logging the error and usage message. The caller is responsible for calling
     * {@code System.exit} if appropriate.
     *
     * @throws CliException if arguments are invalid or the input file does not exist
     */
    public static Config parseOrExit(String[] args) {
        try {
            Config config = ConfigParser.parse(args);
            ConfigValidator.validate(config);
            return DataPrepLineageResolver.applySidecarLineage(config);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Invalid arguments: {}", e.getMessage());
            log.error(USAGE);
            throw new CliException(e.getMessage(), e);
        }
    }
}
