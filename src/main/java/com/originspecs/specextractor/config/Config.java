package com.originspecs.specextractor.config;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.security.SecureRandom;

@Slf4j
public record Config(
        Path inputFile,
        Path outputFile
) {
    private static final String OUTPUT_DIR = "src/main/resources/local-data/output";
    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SUFFIX_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static Config fromArgs(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Exactly 1 argument required: <inputFile.xls>");
        }

        Path inputFile = Path.of(args[0]);
        Path outputFile = deriveOutputPath(inputFile);

        return new Config(inputFile, outputFile);
    }

    /**
     * Derives the output JSON path from the input filename.
     * Strips the file extension, appends a 10-character random alphanumeric suffix,
     * and places the result in the fixed output directory.
     * e.g. {@code pre_processed_file.xls} → {@code src/main/resources/local-data/output/pre_processed_file_a3k7xq29nz.json}
     */
    static Path deriveOutputPath(Path inputFile) {
        String inputName = inputFile.getFileName().toString();
        int dotIndex = inputName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? inputName.substring(0, dotIndex) : inputName;
        String suffix = randomAlphanumeric(SUFFIX_LENGTH);
        String outputName = baseName + "_" + suffix + ".json";
        return Path.of(OUTPUT_DIR, outputName);
    }

    private static String randomAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    public void validate() {
        if (!inputFile.toFile().exists()) {
            throw new IllegalArgumentException("Input file does not exist: " + inputFile.toAbsolutePath());
        }
    }
}
