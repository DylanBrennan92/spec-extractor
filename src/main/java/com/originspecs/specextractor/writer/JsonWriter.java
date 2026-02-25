package com.originspecs.specextractor.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Serialises a list of spec records (header → value maps) to a pretty-printed JSON file.
 */
@Slf4j
public class JsonWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Writes the given records to a JSON file at {@code outputPath}.
     * The output directory is created if it does not already exist.
     *
     * @param records    List of header → value maps to serialise
     * @param outputPath Destination file path
     * @throws IOException if the file cannot be written
     */
    public void write(List<Map<String, String>> records, Path outputPath) throws IOException {
        ensureOutputDirectoryExists(outputPath);

        String json = OBJECT_MAPPER.writeValueAsString(records);
        Files.writeString(outputPath, json);

        log.info("Wrote {} record(s) to {}", records.size(), outputPath.toAbsolutePath());
    }

    private void ensureOutputDirectoryExists(Path outputPath) throws IOException {
        Path parentDir = outputPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            log.info("Creating output directory: {}", parentDir);
            Files.createDirectories(parentDir);
        }
    }
}
