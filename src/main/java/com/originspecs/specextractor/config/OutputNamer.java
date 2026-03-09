package com.originspecs.specextractor.config;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Derives the output JSON file path from a given input file path.
 * The output filename is {@code {iso_timestamp}_{original_base_name}.json},
 * with the timestamp in format {@code yyyyMMdd'T'HHmmss} (date, hours, minutes, seconds).
 * The file is placed in {@link Constants#OUTPUT_DIR}.
 *
 * <p>e.g. {@code pre_processed_file.xls}
 * → {@code src/main/resources/local-data/output/20260225T214530_pre_processed_file.json}
 */
public final class OutputNamer {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private OutputNamer() {}

    /**
     * Derives an output path from the given input file path.
     *
     * @param inputFile Path to the input .xls file
     * @return Path to the output .json file in the fixed output directory
     */
    public static Path derive(Path inputFile) {
        Path fileName = inputFile.getFileName();
        String inputName = fileName != null ? fileName.toString() : inputFile.toString();
        int dotIndex = inputName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? inputName.substring(0, dotIndex) : inputName;
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String outputName = timestamp + "_" + baseName + ".json";
        return Path.of(Constants.OUTPUT_DIR, outputName);
    }
}
