package com.originspecs.specextractor.config;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CliParser {

    private static final String USAGE = """
            Usage: java -jar spec-extractor.jar <inputFile.xls>
            Output is written automatically to src/main/resources/local-data/output/ as {timestamp}_{input_base_name}.json.
            Example: java -jar target/spec-extractor.jar src/main/resources/local-data/input/pre_processed_file.xls
            """;

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
            Config config = Config.fromArgs(args);
            config.validate();
            return config;
        } catch (IllegalArgumentException e) {
            log.error("Invalid arguments: {}", e.getMessage());
            log.error(USAGE);
            throw new CliException(e.getMessage());
        }
    }
}
