package com.originspecs.specextractor.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.originspecs.specextractor.model.SpecRecord;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Serialises a list of SpecRecords to a pretty-printed JSON file.
 * Each record is serialised as a flat JSON object (header → value).
 */
@Slf4j
public class JsonWriter implements SpecRecordWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .addMixIn(SpecRecord.class, SpecRecordMixin.class)
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Writes the given records to a JSON file at {@code outputPath}.
     * The output directory is created if it does not already exist.
     *
     * @param records    SpecRecords to serialise
     * @param outputPath Destination file path
     * @throws IOException if the file cannot be written
     */
    public void write(List<SpecRecord> records, Path outputPath) throws IOException {
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
