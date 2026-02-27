package com.originspecs.specextractor.config;

import java.nio.file.Path;
import java.security.SecureRandom;

/**
 * Derives the output JSON file path from a given input file path.
 * The input filename has its extension replaced with {@code .json},
 * a 10-character random alphanumeric suffix appended, and is placed
 * in {@link Constants#OUTPUT_DIR}.
 *
 * <p>e.g. {@code pre_processed_file.xls}
 * → {@code src/main/resources/local-data/output/pre_processed_file_a3k7xq29nz.json}
 */
public final class OutputNamer {

    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SUFFIX_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private OutputNamer() {}

    /**
     * Derives an output path from the given input file path.
     *
     * @param inputFile Path to the input .xls file
     * @return Path to the output .json file in the fixed output directory
     */
    public static Path derive(Path inputFile) {
        String inputName = inputFile.getFileName().toString();
        int dotIndex = inputName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? inputName.substring(0, dotIndex) : inputName;
        String outputName = baseName + "_" + randomAlphanumeric(SUFFIX_LENGTH) + ".json";
        return Path.of(Constants.OUTPUT_DIR, outputName);
    }

    private static String randomAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}
