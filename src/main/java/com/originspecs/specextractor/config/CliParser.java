package com.originspecs.specextractor.config;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CliParser {

    private static final String USAGE = """
            Usage: java -jar spec-extractor.jar <inputFile.xls>
            Output is written automatically to src/main/resources/local-data/output/ with a random suffix.
            Example: java -jar target/spec-extractor.jar src/main/resources/local-data/input/pre_processed_file.xls
            """;

    /**
     * Parses CLI arguments into a validated Config, or logs an error, prints usage and exits the process.
     */
    public static Config parseOrExit(String[] args) {
        try {
            Config config = Config.fromArgs(args);
            config.validate();
            return config;
        } catch (IllegalArgumentException e) {
            log.error("Invalid arguments: {}", e.getMessage());
            log.error(USAGE);
            System.exit(1);
            return null;
        }
    }
}
